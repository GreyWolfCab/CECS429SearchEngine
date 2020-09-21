package cecs429.index;

import java.util.ArrayList;
import java.util.List;

public class PositionalPosting {
	
	private int id;
	private List<Integer> positions;

	public PositionalPosting(int id) {
		this.id = id;
		this.positions = new ArrayList<Integer>();
	}

	public void addPosition(int position) {
		this.positions.add(position);
	}


	public int getId() {
		return id;
	}

	public List<Integer> getPositions() {
		return this.positions;
	}

	public void setPositions(List<Integer> positions) {
		this.positions = positions;
	}
	
}