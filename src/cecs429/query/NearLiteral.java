package cecs429.query;

import cecs429.index.Index;
import cecs429.index.KGram;
import cecs429.index.Posting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * A NearLiteral represents a query with a NEAR operator.
 * ex. [baseball NEAR/2 angels]
 */
public class NearLiteral implements Query {

    // The list of the two terms that we will NEAR
    private List<Query> mChildren = new ArrayList<>();
    private int K;

    /**
     * Constructs a NearLiteral with the given terms and K value.
     */
    public NearLiteral(Collection<Query> children, int k) {
        mChildren.addAll(children);
        K = k;
    }

    @Override
    public List<Posting> getPostings(Index index, KGram kGramIndex) {
        return getPostingsPositions(index, kGramIndex);
    }

    @Override
    public List<Posting> getPostingsPositions(Index index, KGram kGramIndex) {
        List<Posting> result = new ArrayList<>();
        List<Posting> firstPostings = mChildren.get(0).getPostingsPositions(index, kGramIndex);
        List<Posting> secondPostings = mChildren.get(1).getPostingsPositions(index, kGramIndex);

        // verify each token appears in at least one document
        if (firstPostings != null && secondPostings != null) {
            // get all postings that contain both tokens where second token can only be up to K positions away
            result = andMergePosting(firstPostings, secondPostings, K);
        }


        return result;
    }


    /**
     * merge two postings lists together based on the ANDing the document id's, and that the first term is some
     * distance before the second term
     *
     * @param firstPostings  first list of postings
     * @param secondPostings second list of postings
     * @param k              positional space between the two terms
     * @return merged list of postings after ANDing the two postings together
     */
    private List<Posting> andMergePosting(List<Posting> firstPostings, List<Posting> secondPostings, int k) {

        List<Posting> result = new ArrayList<Posting>();

        //starting indices for both postings lists
        int i = 0;
        int j = 0;

        //iterate through both postings lists, end when one list has no more elements
        while (i < firstPostings.size() && j < secondPostings.size()) {

            // both lists have this document
            if (firstPostings.get(i).getDocumentId() == secondPostings.get(j).getDocumentId()) {
                // gather the positions of the terms that are up to k positions apart
                for (int d = 1; d < k; d++) {
                    Posting newPosting = positionalMergePosting(firstPostings.get(i), secondPostings.get(j), k);
                    if (newPosting != null) {
                        // posting contains the terms, add to result
                        result.add(newPosting);
                    }
                }
                i++;
                j++;
            } else if (firstPostings.get(i).getDocumentId() < secondPostings.get(j).getDocumentId()) {
                // first list docid is less than second lists docid
                i++;
            } else {
                // second list docid is less than first lists docid
                j++;
            }

        }

        return result;

    }

    /**
     * determine whether the first posting is some positional distance away from the second posting
     *
     * @param firstPosting  doc id should match second term
     * @param secondPosting doc id should match first term
     * @param k             positional space between both terms
     * @return valid postings based on positional distance
     */
    private Posting positionalMergePosting(Posting firstPosting, Posting secondPosting, int k) {

        Posting posting = null;
        int a = 0;
        int b = 0;

        //iterate through position list of both terms, until one runs out
        while (a < firstPosting.getPositions().size() &&
                b < secondPosting.getPositions().size()) {

            //check the different terms are in sequence
            //terms are in sequence
            if (firstPosting.getPositions().get(a) == (secondPosting.getPositions().get(b) - k)) {
                if (posting == null) {
                    posting = new Posting(firstPosting.getDocumentId(), firstPosting.getDocumentTitle());
                    posting.addPosition(firstPosting.getPositions().get(a));
                } else {
                    posting.addPosition(firstPosting.getPositions().get(a));
                }
                a++;
                b++;
                //first term is before the second
            } else if (firstPosting.getPositions().get(a) < (secondPosting.getPositions().get(b) - k)) {
                a++;
                //second term is before the first
            } else {
                b++;
            }

        }

        return posting;

    }
}
