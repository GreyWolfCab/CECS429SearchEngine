package cecs429.index;

import java.util.Map;

public abstract class Index <x> {

	protected Map<String, x> index;
	
	public abstract String[] getDictionary();
	
	public abstract x getPostings(String token);
	
	public abstract void resetIndex();
	
	public abstract int size();

	protected abstract boolean contains(String token);

	protected abstract void createPosting(String token);

}
