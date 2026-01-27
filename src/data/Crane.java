package data;

public class Crane {
	public int id;
	public int initColumn;
	public int initRow;
	
	public Crane() {
    }

	public Crane(int id, int columnLocation, int rowLocation) {
		this.id = id;
		this.initColumn = columnLocation;
		this.initRow = rowLocation;
	}
}
