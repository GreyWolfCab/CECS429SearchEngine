package cecs429.query;

import cecs429.index.Index;
import cecs429.index.KGramIndex;
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

	public OrQuery(Collection<Query> children) {

		mChildren = new ArrayList<Query>(children);
	}

	@Override
	public List<Posting> getPostings(Index index, KGramIndex kGramIndex) {
		List<Posting> result = new ArrayList<>();

		for (int i = 0; i < mChildren.size(); i++) {
			//verify the next posting appears in at least 1 document
			Query child = mChildren.get(i);
			List<Posting> posting = child.getPostings(index, kGramIndex);
			int size = posting.size();
			if (mChildren.get(i).getPostings(index, kGramIndex).size() != 0) {
				result = orMergePosting(mChildren.get(i).getPostings(index, kGramIndex), result);
			}
		}

		// Done: program the merge for an OrQuery, by gathering the postings of the composed Query children and
		// unioning the resulting postings.

		return result;
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
		// Returns a string of the form "[SUBQUERY] + [SUBQUERY] + [SUBQUERY]"
		return "(" +
				String.join(" + ", mChildren.stream().map(c -> c.toString()).collect(Collectors.toList()))
				+ " )";
	}
}

