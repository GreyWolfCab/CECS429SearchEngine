package cecs429.query;

import cecs429.index.Index;
import cecs429.index.KGram;
import cecs429.index.Posting;
import mainapp.Indexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A WildcardLiteral represents a subquery that includes * and utilizes k-grams.
 * ex. c*r
 *  will return all documents that include a term beginning in 'c' and ending in 'r'
 */
public class WildcardLiteral implements Query {

    private String mTerm;

    public WildcardLiteral(String term) {
        mTerm = term;
    }

    public String getTerm() {
        return mTerm;
    }

    @Override
    public List<Posting> getPostingsPositions(Index index, KGram kGramIndex) {
        return getPostings(index, kGramIndex);
    }

    @Override
    public List<Posting> getPostings(Index index, KGram kGramIndex) {
        //generate the largest k-gram we can from each section of the term
        List<String> grams = kGramIndex.getGrams(Indexer.K_GRAM_LIMIT, mTerm);
        //retrieve the common terms among all grams
        Set<String> terms = intersectGramPostings(grams, kGramIndex);
        //post filter step
        List<Posting> filteredTerm = postFilterStep(grams, terms, index);
        //return the postings for the most likely term
        return filteredTerm;//cano*

    }

    private Set<String> intersectGramPostings(List<String> grams, KGram kGramIndex) {

        Set<String> intersectingTerms = new HashSet<>(kGramIndex.getTerms(grams.get(0)));

        for (int i = 1; i < grams.size(); i++) {
            intersectingTerms.retainAll(kGramIndex.getTerms(grams.get(i)));
        }

        return intersectingTerms;

    }

    private List<Posting> postFilterStep(List<String> grams, Set<String> terms, Index index) {

        List<Posting> mergedPostings = new ArrayList<>();

        for (String term : terms) {//go through every duplicate term
            String wildTerm = "$" + term + "$";//add begin/end markers
            boolean[] validTerm = new boolean[grams.size()];//keeps track of each gram that exists in the term

            for (int i = 0; i < grams.size(); i++) {//go through every gram from the query

                boolean gramExists = false;//will remain false if the gram is not in the term

                //linear search the term with the gram, check within the size of the gram
                for (int j = 0; j < (wildTerm.length()-grams.get(i).length()+1); j++) {
                    //check that the gram exists within the term
                    int gramLen = grams.get(i).length()+j;
                    String wildGram = "";
                    for (int k = j; k < gramLen; k++) {
                        wildGram += grams.get(i).charAt(k);
                    }
                    if (wildGram.equals(grams.get(i))) {
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
                    if (index.getPostings(term) != null) {
                        mergedPostings = orMergePosting(mergedPostings, index.getPostings(term));
                    }
                }
            }

        }

        return mergedPostings;//otherwise you get nothing

    }

    /**
     * or merge adds the smallest doc id first, inclusive if both terms have the same id
     * @param firstPostings
     * @param secondPostings
     * @return
     */
    private List<Posting> orMergePosting(List<Posting> firstPostings, List<Posting> secondPostings) {

        List<Posting> result = new ArrayList<Posting>();

        //starting indices for both postings lists
        int i = 0;
        int j = 0;

        //iterate through both postings lists, end when one list has no more elements
        while (i < firstPostings.size() && j < secondPostings.size()) {

            //both lists have this document
            if (firstPostings.get(i).getDocumentId() == secondPostings.get(j).getDocumentId()) {
                result.add(firstPostings.get(i));//include it in merged list
                i++;//iterate through in both lists
                j++;
                //first list docid is less than second lists docid
            } else if (firstPostings.get(i).getDocumentId() < secondPostings.get(j).getDocumentId()) {
                result.add(firstPostings.get(i));
                i++;//iterate first list
            } else {// second list docid is less than first lists docid
                result.add(secondPostings.get(j));
                j++;//iterate second list
            }

        }

        //include the rest of the first postings
        while (i < firstPostings.size()) {
            result.add(firstPostings.get(i));
            i++;
        }

        //include the rest of the second postings
        while (j < secondPostings.size()) {
            result.add(secondPostings.get(j));
            j++;
        }

        return result;

    }

    @Override
    public String toString() {
        return mTerm;
    }
}
