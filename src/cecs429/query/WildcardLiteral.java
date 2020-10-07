package cecs429.query;

import cecs429.index.Index;
import cecs429.index.KGramIndex;
import cecs429.index.Posting;
import mainapp.Indexer;

import java.util.ArrayList;
import java.util.List;

public class WildcardLiteral implements Query {

    private String mTerm;

    public WildcardLiteral(String term) {
        mTerm = term;
    }

    public String getTerm() {
        return mTerm;
    }

    @Override
    public List<Posting> getPostings(Index index, KGramIndex kGramIndex) {
        //generate the largest k-gram we can from each section of the term
        List<String> grams = kGramIndex.getGrams(Indexer.K_GRAM_LIMIT, mTerm);
        //retrieve the common terms among all grams
        List<String> terms = intersectGramPostings(grams, kGramIndex);
        //post filter step
        String filteredTerm = postFilterStep(grams, terms);
        //return the postings for the most likely term
        if (filteredTerm != null) {
            return index.getPostings(filteredTerm);
        } else {
            return null;
        }

    }

    private List<String> intersectGramPostings(List<String> grams, KGramIndex kGramIndex) {

        List<String> intersectingTerms = new ArrayList<>();

        //intersect the first 2 gram postings
        if (grams.size() >= 2) {
            //make sure both grams do not have empty postings
            if (kGramIndex.getTerms(grams.get(0)).size() != 0 &&
                    kGramIndex.getTerms(grams.get(1)).size() != 0) {
                //intersect duplicate terms
                intersectingTerms = mergeTerms(kGramIndex.getTerms(grams.get(0)), kGramIndex.getTerms(grams.get(1)));
            }
        }

        //for more than 2 grams merge the previous merged terms together
        for (int i = 2; i < grams.size(); i++) {

            //intersect duplicate terms
            intersectingTerms = mergeTerms(intersectingTerms, kGramIndex.getTerms(grams.get(i)));

        }

        return intersectingTerms;

    }

    private List<String> mergeTerms(List<String> firstTerms, List<String> secondTerms) {

        List<String> duplicateTerms = new ArrayList<>();

        //starting indices for both postings lists
        int i = 0;
        int j = 0;

        //iterate through both postings lists, end when one list has no more elements
        while(i < firstTerms.size() && j < secondTerms.size()) {

            if (firstTerms.get(i).equals(secondTerms.get(j))) {//if the terms are the same
                duplicateTerms.add(firstTerms.get(i));
                i++;
                j++;
            } else if (firstTerms.get(i).compareTo(secondTerms.get(j)) < 0) {//if the first term is less than the second
                i++;
            } else {//the second term is less than the first
                j++;
            }

        }

        return duplicateTerms;

    }

    private String postFilterStep(List<String> grams, List<String> terms) {

        for (String term : terms) {//go through every duplicate term

            String wildTerm = "$" + term + "$";//add begin/end markers
            boolean[] validTerm = new boolean[grams.size()];//keeps track of each gram that exists in the term

            for (int i = 0; i < grams.size(); i++) {//go through every gram from the query

                boolean gramExists = false;//will remain false if the gram is not in the term

                //linear search the term with the gram, check within the size of the gram
                for (int j = 0; j < (wildTerm.length()-grams.get(i).length()+1); j++) {
                    //check that the gram exists within the term
                    if (wildTerm.substring(j, grams.get(i).length()+j).equals(grams.get(i))) {
                        gramExists = true;//no longer have to check the term
                        break;
                    }

                }

                if (gramExists) {//mark the gram as existing for the term
                    validTerm[i] = gramExists;
                } else {//the gram doesn't exist in the term, therefore it is an invalid term
                    break;
                }

            }

            //verify that the term has every gram present
            for (int q = 0; q < validTerm.length; q++) {
                if (!validTerm[q]) {//if a gram was not found
                    break;//this is not the correct term
                } else if (validTerm[q] && q == validTerm.length-1) {//if every gram was found
                    return term;//this likely the term
                }
            }

        }

        return null;//otherwise you get nothing

    }

    @Override
    public String toString() {
        return mTerm;
    }
}
