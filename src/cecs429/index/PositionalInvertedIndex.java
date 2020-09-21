package cecs429.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class PositionalInvertedIndex extends Index<List<PositionalPosting>> {

	public PositionalInvertedIndex() {
		this.index = new HashMap<String, List<PositionalPosting>>();
	}

	public void add(String token, int id, int position) {
		if (!contains(token))
			createPosting(token);
		if (!contains(token, id))
			addPosting(token, id);
		PositionalPosting posting = getLatestPosting(getPostings(token));
		posting.addPosition(position);
	}

	@Override
	public String[] getDictionary() {
		SortedSet<String> tokens = new TreeSet<String>(this.index.keySet());
		return tokens.toArray(new String[tokens.size()]);
	}

	@Override
	public List<PositionalPosting> getPostings(String token) {
		return this.index.get(token);
	}

	@Override
	public void resetIndex() {
		this.index = new HashMap<String, List<PositionalPosting>>();
	}

	@Override
	public int size() {
		return index.size();
	}

	@Override
	protected boolean contains(String token) {
		return this.index.containsKey(token);
	}

	@Override
	protected void createPosting(String token) {
		this.index.put(token, new ArrayList<PositionalPosting>());
	}

	private void addPosting(String token, int id) {
		getPostings(token).add(new PositionalPosting(id));
	}
	private boolean contains(String token, int id) {
		List<PositionalPosting> postings = getPostings(token);
		return !postings.isEmpty() && getLatestPosting(postings).getId() >= id;
	}
	private PositionalPosting getLatestPosting(List<PositionalPosting> postings) {
		return postings.get(postings.size() - 1);
	}

}