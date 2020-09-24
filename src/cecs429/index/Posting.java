package cecs429.index;

import java.util.ArrayList;
import java.util.List;

/**
 * A Posting encapulates a document ID associated with a search query component.
 */
public class Posting {
	private int mDocumentId;
	private List<Integer> positions;
	
	public Posting(int documentId, int position) {

		mDocumentId = documentId;
		positions = new ArrayList<Integer>();
		positions.add(position);
	}
	
	public int getDocumentId() {
		return mDocumentId;
	}

	/**
	 * update positions list with a new position
	 * @param position the new position to be added
	 */
	public void addPosition(int position) {
		positions.add(position);
	}

	public List<Integer> getPositions() {
		return positions;
	}
}
