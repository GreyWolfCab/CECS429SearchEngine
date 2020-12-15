package cecs429.index;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.*;
import java.util.*;

public class DiskIndexWriter {

    public ArrayList<Long> writeIndex(Index index, String indexLocation) throws IOException {

        //create an index folder in the corpus
        createIndexFolder(indexLocation);

        //create B+ tree for terms and addresses
        DB db = DBMaker.fileDB(indexLocation + "\\index\\index.db").make();
        BTreeMap<String, Long> map = db.treeMap("map")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .counterEnable()
                .createOrOpen();

        ArrayList<Long> termAddresses = new ArrayList<>();

        List<String> terms = index.getVocabulary();
        // all values in the file are 4-bytes
        //format: postings size : term 1 frequency : firstDocumentId : total positions : firstPosition :
        // term 2 frequency : firstDocument : total positions : firstPosition : secondPosition :

        // maximum address is 8-bytes = 64-bits = 9,223,372,036,854,775,807 in decimal

        //create postings.bin file to act as index on disk
        try (DataOutputStream dout = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(indexLocation + "\\index\\postings.bin")))) {

            for (int i = 0; i < terms.size(); i++) {//iterate through vocabulary of index

                //store term and address in B+ tree
                map.put(terms.get(i), (long)dout.size());
                //get current position stored as address for term
                termAddresses.add((long)dout.size());
                //make sure the term exists
                if (index.getPostings(terms.get(i)) == null) {
                    dout.writeInt(0);//term appears in 0 documents
                } else {//psze = 15, tfreq = 231
                    int postingsSize = index.getPostings(terms.get(i)).size();
                    dout.writeInt(postingsSize);
                    List<Posting> postings = index.getPostings(terms.get(i));
                    int termFrequency = 0;
                    for (int j = 0; j < postings.size(); j++) {
                        termFrequency += postings.get(j).getPositions().size();
                    }
                    //int termFrequency = index.getPostings(terms.get(i)).size();//term frequency among documents
                    dout.writeInt(termFrequency);//store term frequency among documents
                    int prevDocumentId = 0;
                    for (int j = 0; j < postingsSize; j++) {//iterate through every document with this term

                        int documentId = index.getPostings(terms.get(i)).get(j).getDocumentId();//gets document id
                        dout.writeInt(documentId - prevDocumentId);//store the gap between document id's
                        //term frequency within a document
                        int termDocumentFrequency = index.getPostings(terms.get(i)).get(j).getPositions().size();
                        dout.writeInt(termDocumentFrequency);//store term frequency in document
                        int prevTermPosition = 0;
                        for (int k = 0; k < termDocumentFrequency; k++) {//iterate through all terms in a document
                            //gets a terms position within a document
                            int termPosition = index.getPostings(terms.get(i)).get(j).getPositions().get(k);
                            dout.writeInt(termPosition - prevTermPosition);//store each gap between positions
                            prevTermPosition = termPosition;//store previous position
                        }
                        prevDocumentId = documentId;//store previous document
                    }//proceed to next document

                }

            }//proceed to next term

        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }

        db.close();

        return termAddresses;

    }

    public ArrayList<Long> writeKGramIndex(KGram kGramIndex, String indexLocation) {

        ArrayList<Long> termAddresses = new ArrayList<>();

        //create an index folder in the corpus
        createIndexFolder(indexLocation);

        //create B+ tree for grams and addresses
        DB db = DBMaker.fileDB(indexLocation + "\\index\\kGramIndex.db").make();
        BTreeMap<String, Long> map = db.treeMap("kGram")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .counterEnable()
                .createOrOpen();

        List<String> grams = kGramIndex.getGrams();//get every gram from the kGramIndex
        // all values in the file are 4-bytes
        //format: # of terms : sizeTerm1 : char1 : char2 : char3: sizeTerm2: char 1 : char 2 :
        // # of terms : sizeTerm1 : char1 : char2 : # of terms :
        //     int    :    int    :  char :  char :     int    :

        //create postings.bin file to act as index on disk
        try (DataOutputStream dout = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(indexLocation + "\\index\\kGramIndex.bin")))) {

            for (String gram : grams) {//iterate through every gram

                //store gram and address in B+ tree
                map.put(gram, (long)dout.size());
                //get current position stored as address for term
                termAddresses.add((long)dout.size());
                //make sure the gram exists
                if (kGramIndex.getTerms(gram) == null) {
                    dout.writeInt(0);//term appears in 0 documents
                } else {
                    Set<String> terms = kGramIndex.getTerms(gram);
                    int termFrequency = terms.size();//term frequency in the gram
                    dout.writeInt(termFrequency);//store term frequency in the gram

                    for (String term : terms) {//iterate through every term with the current gram
                        int termSize = term.length();
                        dout.writeInt(termSize);//store term length
                        for (int j = 0; j < termSize; j++) {//iterate through each char in the term
                            dout.writeChar(term.charAt(j));//store each term's char
                        }
                    }
                }

            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        db.close();

        return termAddresses;

    }

    public void writeLeaderIndex(int[] docsToLeaders, int corpusSize, String indexLocation) {

        createIndexFolder(indexLocation);

        //format: leadID : # of followers : followerId_1 : followerId_2

        //create leaderIndex.bin file to act associate docs to leaders
        try (DataOutputStream dout = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(indexLocation + "\\index\\leaderIndex.bin")))) {

            HashMap<Integer, Integer> followerToLeader = new HashMap<>();//(followerID, leaderID)
            for (int i = 0; i < docsToLeaders.length; i++) {
                if (docsToLeaders[i] != -1) {
                    followerToLeader.put(i, docsToLeaders[i]);
                }
            }

            /* sort followers by ascending leaders */
            // Create a list from elements of HashMap
            List<Map.Entry<Integer, Integer> > list =
                    new LinkedList<Map.Entry<Integer, Integer> >(followerToLeader.entrySet());

            // Sort the list
            Collections.sort(list, new Comparator<Map.Entry<Integer, Integer> >() {
                public int compare(Map.Entry<Integer, Integer> o1,
                                   Map.Entry<Integer, Integer> o2)
                {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
            });

            dout.writeInt((int) Math.sqrt(corpusSize));//write the number of leaders
            for (int i = 0; i < list.size(); ) {

                int leaderId = list.get(i).getValue();
                ArrayList<Integer> followers = new ArrayList<>();
                int totalFollowers = 0;
                int j = i;
                while(j < list.size() && leaderId == list.get(j).getValue()) {
                    followers.add(list.get(j).getKey());
                    totalFollowers++;
                    j++;

                }

                dout.writeInt(leaderId);
                dout.writeInt(totalFollowers);
                for (int y = 0; y < followers.size(); y++) {
                    dout.writeInt(followers.get(y));
                }

                i = j;

            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    public void writeDocumentWeights(ArrayList<Double> documentWeights, String indexLocation) {

        createIndexFolder(indexLocation);

        //create docWeights.bin file to act as index on disk
        try (DataOutputStream dout = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(indexLocation + "\\index\\docWeights.bin")))) {

            for (Double documentWeight : documentWeights) {//iterate through every document weight in doc id order

                dout.writeDouble(documentWeight);//write doc weight to disk (8-byte double)

            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void createIndexFolder(String indexLocation) {

        //create an index folder in the corpus
        File directory = new File(indexLocation + "\\index");
        if (!directory.exists()) {
            directory.mkdirs();

        }

    }

}
