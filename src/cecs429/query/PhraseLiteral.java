package cecs429.query;

import cecs429.index.Index;
import cecs429.index.Posting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a phrase literal consisting of one or more terms that must occur in sequence.
 */
public class PhraseLiteral implements Query {
	// The list of individual terms in the phrase.
	private List<Query> mChildren = new ArrayList<>();

	/**
	 * Constructs a PhraseLiteral with the given individual phrase terms.
	 */
	public PhraseLiteral(Collection<Query> children) {
		mChildren.addAll(children);
	}

	@Override
	public List<Posting> getPostings(Index index) {
		List<Posting> result = new ArrayList<>();
		// Done: program this method. Retrieve the postings for the individual terms in the phrase,
		// and positional merge them together.
		int distance = 1;//maintain the distance required between phrases

		if (mChildren.size() < 2) {//one child denotes a term literal
			System.out.println("This is a term literal...");
		} else  {//multiple terms to merge

			//verify that both terms appear at least in one document
			if (mChildren.get(0).getPostings(index) != null &&
				mChildren.get(1).getPostings(index) != null) {

				//merge the first 2 terms postings together
				result = andMergePosting(mChildren.get(0).getPostings(index), mChildren.get(1).getPostings(index), distance);

			}

			//if there are more terms in the phrase, iterate through the rest of the term postings
			for (int i = 2; i < mChildren.size(); i++) {

				distance++;//increase the distance between terms
				//verify the next posting appears in at least 1 document
				if (mChildren.get(i).getPostings(index) != null) {
					//merge previous result postings with new term postings
					result = andMergePosting(result, mChildren.get(i).getPostings(index), distance);
				}

			}

		}

		return result;
	}

	/**
	 * merge two postings lists together based on the ANDing the document id's, and that the first term is some
	 * distance before the second term
	 * @param firstPostings first list of postings
	 * @param secondPostings second list of postings
	 * @param distance positional space between the two terms
	 * @return merged list of postings after ANDing the two postings together
	 */
	private List<Posting> andMergePosting(List<Posting> firstPostings, List<Posting> secondPostings, int distance) {

		List<Posting> result = new ArrayList<Posting>();

		//starting indices for both postings lists
		int i = 0;
		int j = 0;

		//iterate through both postings lists, end when one list has no more elements
		while (i < firstPostings.size() && j < secondPostings.size()) {

			//both lists have this document
			if (firstPostings.get(i).getDocumentId() == secondPostings.get(j).getDocumentId()) {
				//gather the positions of the phrase terms
				List<Posting> newPostings = positionalMergePosting(firstPostings.get(i), secondPostings.get(j), distance);
				if (newPostings.size() > 0) {//if the phrase actually exists
					result.addAll(newPostings);//include it in merged list
				}
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

	/**
	 * determine whether the first posting is some positional distance away from the second posting
	 * @param firstPosting doc id should match second term
	 * @param secondPosting doc id should match first term
	 * @param distance positional space between both terms
	 * @return valid postings based on positional distance
	 */
	private List<Posting> positionalMergePosting(Posting firstPosting, Posting secondPosting, int distance) {

		List<Posting> postings = new ArrayList<>();//postings that are considered a phrase

		//positional indices
		int a = 0;
		int b = 0;

		//iterate through position list of both terms, until one runs out
		while (a < firstPosting.getPositions().size() &&
				b < secondPosting.getPositions().size()) {

			//check the different terms are in sequence
			//terms are in sequence
			if (firstPosting.getPositions().get(a) == (secondPosting.getPositions().get(b) - distance)) {
				postings.add(new Posting(firstPosting.getDocumentId(), firstPosting.getPositions().get(a)));
				a++;
				b++;
				//first term is before the second
			} else if (firstPosting.getPositions().get(a) < (secondPosting.getPositions().get(b) - distance)) {
				a++;
				//second term is before the first
			} else {
				b++;
			}

		}

		return postings;

	}

	@Override
	public String toString() {
		return String.join(" ", mChildren.stream().map(c -> c.toString()).collect(Collectors.toList()));
	}
}
