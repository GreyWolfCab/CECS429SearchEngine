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

    public long getKeyTermAddress(String term) {
        if (map.get(term) == null) {
            return -1;
        } else {
            return map.get(term);
        }
    }

    public List<Posting> accessTermData(long address) {

        List<Posting> postings = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(indexLocation + "\\postings.bin", "r")) {

            raf.seek(address);
            System.out.println("At position: " + address);
            int termFrequency = raf.readInt();
            System.out.println("Term Document frequency: " + termFrequency);
            int docId = 0;
            for (int i = 0; i < termFrequency; i++) {
                docId += raf.readInt();
                int totalPositions = raf.readInt();
                Posting post = null;
                System.out.println("Doc Id : " + docId + " Total positions: " + totalPositions);
                int position = 0;
                for (int j = 0; j < totalPositions; j++) {
                    position += raf.readInt();
                    if (post == null) {
                        post = new Posting(docId, position);
                    } else {
                        post.addPosition(position);
                    }
                    System.out.print(position + ", ");
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
        return accessTermData(getKeyTermAddress(stemmed));
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
