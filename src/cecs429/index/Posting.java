package cecs429.index;

import java.util.ArrayList;
import java.util.List;

/**
 * A Posting encapulates a document ID associated with a search query component.
 */
public class Posting {
	private int mDocumentId;
	private List<Integer> positions;
	private String mDocumentTitle;
	
	public Posting(int documentId, int position, String title) {

		mDocumentId = documentId;
		mDocumentTitle = title;
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

	@Override
	public String toString() {
		return
				"    <tr>\n" +
				"        <td>"+mDocumentId+"</td>\n" +
				"        <td><button id=\""+mDocumentId+"\">"+mDocumentTitle+"</button></td>\n" +
				"        <td>"+positions+"</td>\n" +
				"    </tr>\n";
	}

	public String getDocumentTitle() {
		return mDocumentTitle;
	}
}
