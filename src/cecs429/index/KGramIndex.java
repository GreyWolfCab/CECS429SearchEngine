package cecs429.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class KGramIndex {

    private HashMap<String, List<String>> mIndex;

    public KGramIndex() {
        mIndex = new HashMap<String, List<String>>();
    }

    public void addGram(int gramLimit, String term) {

        String gramableTerm = "$" + term + "$";//signify beginning and end of term

        if (gramableTerm.length() <= 2) {
            //ignore the term
        } else {

            for (int j = gramLimit; j > 0; j--) {//add multiple k-gram sizes starting from the upper limit

                int gramSize = j;

                //make sure the gram is within the limits of the term
                if (gramableTerm.length() >= gramSize) {
                    gramSize = gramableTerm.length()-1;
                }

                //break the term down by the given gram size
                for (int i = 0; i < gramableTerm.length()-(gramSize-1); i++) {
                    String gram = gramableTerm.substring(i, i+gramSize);//get a usable gram

                    List<String> terms = this.mIndex.get(gram);//get the term associated to the gram

                    if (terms == null) {//this is the first occurence of the gram
                        terms = new ArrayList<>();//create a new arraylist
                        terms.add(term);//add the term to the list
                        this.mIndex.put(gram, terms);//add the pair to the hashmap
                    } else {//this gram has occurred before
                        if (!terms.contains(term)) {//verify the gram is not repeating in the same term
                            terms.add(term);
                        }
                    }
                }

            }

        }

    }

    public List<String> getTerms(String gram) {
        List<String> terms = mIndex.get(gram);
        Collections.sort(terms);
        return terms;
    }

    public List<String> getGrams() {
        List<String> keySet = new ArrayList<>(mIndex.keySet());
        Collections.sort(keySet);
        return keySet;
    }

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

}
