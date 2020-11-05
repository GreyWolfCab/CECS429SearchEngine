package cecs429.index;

import java.io.*;
import java.util.*;

import cecs429.text.AdvancedTokenProcesser;
import org.mapdb.*;

public class DiskPositionalIndex implements Index {

    DB diskIndex = null;
    BTreeMap<String, Long> map = null;
    String indexLocation;

    public DiskPositionalIndex(String dir) {
        indexLocation = dir + "\\index";
        try {
            diskIndex = DBMaker.fileDB(indexLocation + "\\file.db").make();
            map = diskIndex.treeMap("map")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .open();
        } catch (Exception e) {
            System.out.println("Could not find B+ Tree on disk...");
            e.printStackTrace();
        }
    }

    public double getDocumentWeight(int docId) {

        try (RandomAccessFile raf = new RandomAccessFile(indexLocation + "\\docWeights.bin", "r")) {

            raf.seek(docId * 8);//account for 8-byte offset
            return raf.readDouble();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return -1;

    }

    public long getKeyTermAddress(String term) {
        if (map.get(term) == null) {
            return -1;
        } else {
            return map.get(term);
        }
    }

    public List<Posting> accessTermData(long address, boolean withPositions) {

        List<Posting> postings = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(indexLocation + "\\postings.bin", "r")) {

            raf.seek(address);//skip to the terms address
            System.out.println("At position: " + address);
            int termFrequency = raf.readInt();//collect how many documents the term appears in
            System.out.println("Term Document frequency: " + termFrequency);
            int docId = 0;
            for (int i = 0; i < termFrequency; i++) {
                docId += raf.readInt();//collect next docId
                int totalPositions = raf.readInt();//collect term frequency in the document
                Posting post = null;
                System.out.println("Doc Id : " + docId + " Document Weight: " + getDocumentWeight(docId) + " Total positions: " + totalPositions);

                if (withPositions) {
                    int position = 0;
                    for (int j = 0; j < totalPositions; j++) {
                        position += raf.readInt();
                        if (post == null) {
                            post = new Posting(docId);
                            post.addPosition(position);
                        } else {
                            post.addPosition(position);
                        }
                        System.out.print(position + ", ");
                    }
                } else {
                    System.out.println("From " + raf.getFilePointer());
                    //each position represents 4 bytes so (* 4) to account for this offset
                    raf.seek(raf.getFilePointer() + (totalPositions * 4));
                    post = new Posting(docId);
                    System.out.println("Skipping to " + raf.getFilePointer());
                }

                postings.add(post);
                System.out.println();
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return postings;
    }

    @Override
    public List<Posting> getPostings(String term) {

        String stemmed = AdvancedTokenProcesser.stemToken(term);//stem the term
        return accessTermData(getKeyTermAddress(stemmed), false);
    }

    @Override
    public List<Posting> getPostingsPositions(String term) {
        String stemmed = AdvancedTokenProcesser.stemToken(term);//stem the term
        return accessTermData(getKeyTermAddress(stemmed), true);
    }

    @Override
    public List<String> getVocabulary() {
        Iterator<String> iterator = map.getKeys().iterator();
        List<String> vocab = new ArrayList<>();
        while (iterator.hasNext()) {
            vocab.add(iterator.next());
        }
        return vocab;
    }
}
