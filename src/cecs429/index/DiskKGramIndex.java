package cecs429.index;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DiskKGramIndex implements KGram {

    DB diskIndex = null;
    BTreeMap<String, Long> map = null;
    String indexLocation;

    public DiskKGramIndex(String dir) {
        indexLocation = dir + "\\index";
        try {
            diskIndex = DBMaker.fileDB(indexLocation + "\\kGramIndex.db").make();
            map = diskIndex.treeMap("kGram")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .open();
        } catch (Exception e) {
            System.out.println("Could not find B+ Tree on disk...");
            e.printStackTrace();
        }
    }

    public long getKeyGramAddress(String gram) {
        if (map.get(gram) == null) {
            return -1;
        } else {
            return map.get(gram);
        }
    }

    @Override
    public Set<String> getTerms(String gram) {

        Set<String> terms = new HashSet<>();
        long address = getKeyGramAddress(gram);
        if (address == -1) {
            return null;
        }

        try (RandomAccessFile raf = new RandomAccessFile(indexLocation + "\\kGramIndex.bin", "r")) {

            raf.seek(address);//move to initial offset
            int termFrequency = raf.readInt();//get total # of terms associated with the gram
            for (int i = 0; i < termFrequency; i++) {//iterate through all terms
                int termSize = raf.readInt();//get the size of the current term
                byte[] chars = new byte[termSize * 2];//(*2) since chars are 2 bytes
                raf.readFully(chars);//read in all the bytes that make up the term
                //read the string as UTF_16 so it can be compared to default java strings
                terms.add(new String(chars, StandardCharsets.UTF_16));//add the new string to the terms list
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        //return all terms associated with the Gram
        return terms;
    }

    @Override
    public List<String> getGrams() {
        Iterator<String> iterator = map.getKeys().iterator();
        List<String> grams = new ArrayList<>();
        while (iterator.hasNext()) {
            grams.add(iterator.next());
        }
        return grams;
    }

    @Override
    public List<String> getGrams(int gramLimit, String term) {

        String gramableTerm = "$" + term + "$";//signify beginning and end of term
        List<String> grams = new ArrayList<>();

        String[] terms = gramableTerm.split("\\*");

        for (int i = 0; i < terms.length; i++) {

            int gramSize = gramLimit;//get the largest k-gram possible
            while (gramSize > terms[i].length()) {
                gramSize--;
            }

            for (int j = 0; j < terms[i].length() - (gramSize-1); j++) {
                grams.add(terms[i].substring(j, j+gramSize));//get a usable gram
            }

        }

        return grams;

    }

    @Override
    public void addGram(int gramLimit, String term) {
        System.out.println("Unable to add k-grams to on-disk instance...");
    }
}
