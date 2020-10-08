package cecs429.query;

import cecs429.index.Index;
import cecs429.index.KGramIndex;
import cecs429.index.Posting;
import mainapp.Indexer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<String> terms = intersectGramPostings(grams, kGramIndex);
        //post filter step
        String filteredTerm = postFilterStep(grams, terms);
        System.out.println("filter me " + filteredTerm);
        //return the postings for the most likely term
        if (filteredTerm != null) {
            return index.getPostings(filteredTerm);
        } else {
            return null;
        }

    }

    private Set<String> intersectGramPostings(List<String> grams, KGramIndex kGramIndex) {

        Set<String> intersectingTerms = new HashSet<>(kGramIndex.getTerms(grams.get(0)));

        for (int i = 1; i < grams.size(); i++) {
            intersectingTerms.retainAll(kGramIndex.getTerms(grams.get(i)));
        }

        return intersectingTerms;

    }

    private String postFilterStep(List<String> grams, Set<String> terms) {

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
