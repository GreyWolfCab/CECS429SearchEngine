package cecs429.index;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;

public class DiskPositionalIndex implements Index {

    DB db = DBMaker.fileDB("file.db").make();
    BTreeMap<String, Long> map = db.treeMap("map")
            .keySerializer(Serializer.STRING)
            .valueSerializer(Serializer.LONG)
            .counterEnable()
            .createOrOpen();

    public void addBTree(String term, long address) {

        map.put(term, address);

    }

    public void closeBTree() {
        db.commit();//not sure if there is a difference
        db.close();
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
