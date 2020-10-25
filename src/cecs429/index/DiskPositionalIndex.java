package cecs429.index;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;

public class DiskPositionalIndex implements Index {

    DB db = DBMaker.fileDB("file.db").make();
    BTreeMap<String[], Long> map = db.treeMap("map").keySerializer(new SerializerArray(Serializer.STRING)).keySerializer(new SerializerArray(Serializer.LONG)).createOrOpen();

    public void createBTree() {
        // TODO: put keys and values into the b+ tree
        //  map.put("keys", "values");
        //  db.close(); // important to close (or data can get corrupted)
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
