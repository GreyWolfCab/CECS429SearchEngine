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
		List<Posting> result = new ArrayList<Posting>();

		// Done: program the merge for an AndQuery, by gathering the postings of the composed QueryComponents and
		// intersecting the resulting postings.
		// TODO: optimize by which posting is larger

		if (mChildren.size() < 2) {//should be impossible to reach for and query
			System.out.println("How did you get in the And Query?");
		} else if (mChildren.size() == 2) {//if you only have to merge 2 postings

			//verify the both terms appear at least in one document
			if (mChildren.get(0).getPostings(index) != null &&
					mChildren.get(1).getPostings(index) != null) {
				//merge two postings together into result
				result = andMergePosting(mChildren.get(0).getPostings(index), mChildren.get(1).getPostings(index));
			}

		} else {//there are more than 2 postings

			//iterate through the rest of the postings
			for (int i = 2; i < mChildren.size(); i++) {

				//verify the next posting appears in at least 1 document
				if (mChildren.get(i).getPostings(index) != null) {
					//merge previous result postings with new term postings
					result = andMergePosting(mChildren.get(i).getPostings(index), result);
				}

			}

		}

		return result;
	}

	/**
	 * merge two postings lists together based on the ANDing the document id's
	 * @param firstPostings first list of postings
	 * @param secondPostings second list of postings
	 * @return merged list of postings after ANDing the two postings together
	 */
	private List<Posting> andMergePosting(List<Posting> firstPostings, List<Posting> secondPostings) {

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
				i++;//iterate first list
			} else {// second list docid is less than first lists docid
				j++;//iterate second list
			}

		}

		return result;

	}

	@Override
	public String toString() {
		return
				String.join(" ", mChildren.stream().map(c -> c.toString()).collect(Collectors.toList()));
	}
}

