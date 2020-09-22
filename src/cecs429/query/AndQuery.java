package cecs429.query;

import cecs429.index.Index;
import cecs429.index.Posting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An AndQuery composes other Query objects and merges their postings in an intersection-like operation.
 */
public class AndQuery implements Query {
	private List<Query> mChildren;

	public AndQuery(Iterable<Query> children) {

		mChildren = new ArrayList<Query>((Collection<? extends Query>) children);
	}

	@Override
	public List<Posting> getPostings(Index index) {
		List<Posting> result = null;

		// TODO: program the merge for an AndQuery, by gathering the postings of the composed QueryComponents and
		// intersecting the resulting postings.

		// Add all postings of the first Query in mChildren
		result.addAll(mChildren.get(0).getPostings(index));
		for (int i = 0; i < mChildren.size(); i++){
			// intersect each postings list to the first one
			result.retainAll(mChildren.get(i).getPostings(index));
		}

		return result;
	}

	@Override
	public String toString() {
		return
				String.join(" ", mChildren.stream().map(c -> c.toString()).collect(Collectors.toList()));
	}
}

