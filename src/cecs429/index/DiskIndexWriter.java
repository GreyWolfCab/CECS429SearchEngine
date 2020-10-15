package cecs429.index;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DiskIndexWriter {

    public ArrayList<Integer> writeIndex(Index index, String indexLocation) throws IOException {

        ArrayList<Integer> termAddresses = new ArrayList<>();

        //create an index folder in the corpus
        File directory = new File(indexLocation + "\\index");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        List<String> terms = index.getVocabulary();
        // all values in the file are 4-bytes
        //format: term 1 frequency : firstDocumentId : total positions : firstPosition :
        // term 2 frequency : firstDocument : total positions : firstPosition : secondPosition :

        // maximum address is 8-bytes = 64-bits = 9,223,372,036,854,775,807 in decimal

        //create postings.bin file to act as index on disk
        try (DataOutputStream dout = new DataOutputStream(
                new FileOutputStream(indexLocation + "\\index\\postings.bin"))) {

            for (int i = 0; i < terms.size(); i++) {//iterate through vocabulary of index

                //get current position stored as address for term
                termAddresses.add(dout.size());
                //make sure the term exists
                if (index.getPostings(terms.get(i)) == null) {
                    dout.writeInt(0);//term appears in 0 documents
                } else {
                    int termFrequency = index.getPostings(terms.get(i)).size();//term frequency among documents
                    dout.writeInt(termFrequency);//store term frequency among documents
                    int prevDocumentId = 0;
                    for (int j = 0; j < termFrequency; j++) {//iterate through every document with this term

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

        return termAddresses;

    }

}
