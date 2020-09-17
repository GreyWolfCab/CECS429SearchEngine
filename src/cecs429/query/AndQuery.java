package cecs429.query;

import cecs429.index.Index;
import cecs429.index.Posting;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An AndQuery composes other Query objects and merges their postings in an intersection-like operation.
 */
public class AndQuery implements Query {
	private List<Query> mChildren;
	
	public AndQuery(Iterable<Query> children) {

		mChildren = new ArrayList<>();

		children.forEach(new Consumer<Query>() {

			@Override
			public void accept(Query q) {
				mChildren.add(q);
			}

		});

	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		List<Posting> result = null;
		
		// TODO: program the merge for an AndQuery, by gathering the postings of the composed QueryComponents and
		// intersecting the resulting postings.
		
		return result;
	}
	
	@Override
	public String toString() {
		return String.join(" ", mChildren.stream().map(c -> c.toString()).collect(Collectors.toList()));
	}
}
