package cecs429.query;

import cecs429.index.Index;
import cecs429.index.Posting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An OrQuery composes other Query objects and merges their postings with a union-type operation.
 */
public class OrQuery implements Query {
	// The components of the Or query.
	private List<Query> mChildren;

	public OrQuery(Iterable<Query> children) {

		mChildren = new ArrayList<Query>((Collection<? extends Query>) children);
	}

	@Override
	public List<Posting> getPostings(Index index) {
		List<Posting> result = null;

		// TODO: program the merge for an OrQuery, by gathering the postings of the composed Query children and
		// unioning the resulting postings.

		for (int i = 0; i < mChildren.size(); i++){
			// union each postings list to the results list
			result.addAll(mChildren.get(i).getPostings(index));
		}

		return result;
	}

	@Override
	public String toString() {
		// Returns a string of the form "[SUBQUERY] + [SUBQUERY] + [SUBQUERY]"
		return "(" +
				String.join(" + ", mChildren.stream().map(c -> c.toString()).collect(Collectors.toList()))
				+ " )";
	}
}

