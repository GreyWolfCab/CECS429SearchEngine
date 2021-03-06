package cecs429.index;

import java.util.ArrayList;
import java.util.List;

public interface Index  {

	/**
	 * Retrieves a list of Postings of documents that contain the given term.
	 */
	List<Posting> getPostings(String term);

	List<Posting> getPostingsPositions(String term);

	/**
	 * A (sorted) list of all terms in the index vocabulary.
	 */
	List<String> getVocabulary();

	int getTermFrequency(String term);

	double getDocumentWeight(int docId);

	int getDocumentFrequencyOfTerm(String term);

	ArrayList<Integer> getDocumentLeaders();

	ArrayList<Integer> getDocumentFollowers(int leaderId);

}
