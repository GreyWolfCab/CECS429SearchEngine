package cecs429.query;

import cecs429.index.Index;
import cecs429.index.KGramIndex;
import cecs429.index.Posting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * A NearLiteral represents a query with a NEAR operator.
 * ex. [baseball NEAR/2 angels]
 */
public class NearLiteral implements Query{

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
    public List<Posting> getPostings(Index index, KGramIndex kGramIndex) {
        List<Posting> result = new ArrayList<>();



        return null;
    }
}
