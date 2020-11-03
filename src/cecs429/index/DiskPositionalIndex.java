package cecs429.index;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;

public class DiskPositionalIndex implements Index {

//    DB db = DBMaker.fileDB("file.db").make();

    DB diskIndex = null;
    BTreeMap<String, Long> map = null;

    public DiskPositionalIndex(String dir) {
        try {
            diskIndex = DBMaker.fileDB(dir + "\\index\\file.db").make();
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

    public void closeBTree() {
        //db.commit();//not sure if there is a difference
        diskIndex.close();
    }

    public long getKeyTermAddress(String term) {
        if (map.get(term) == null) {
            return -1;
        } else {
            return map.get(term);
        }
    }

    @Override
    public List<Posting> getPostings(String term) {

        return null;
    }

    @Override
    public List<String> getVocabulary() {
        return null;
    }
}
