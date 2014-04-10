
public class GameBoard {
	private float towerProbability[][];
	private int visits[][];
	private int hits[][];
	private boolean seen[][];
	private boolean hasTree[][];
	
	public GameBoard(int width, int height, float towerDensity) {
		towerProbability = new   float[width][height];
		visits           = new     int[width][height];
		hits             = new     int[width][height];
		seen             = new boolean[width][height];
		hasTree          = new boolean[width][height];
		
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				towerProbability[i][j] = towerDensity;
				visits[i][j]           = 0;
				hits[i][j]             = 0;
				seen[i][j]             = false;
				hasTree[i][j]          = false;
			}
		}
	}
	
	public float getTowerProbability(int x, int y) {
		return towerProbability[x][y];
	}
	
	public void setTowerProbability(int x, int y, float prob) {
		towerProbability[x][y] = prob;
	}
	
	public int getVisits(int x, int y) {
		return visits[x][y];
	}
	
	public void incrementVisits(int x, int y) {
		visits[x][y]++;
	}
	
	public int getHits(int x, int y) {
		return hits[x][y];
	}
	
	public void incrementHits(int x, int y) {
		hits[x][y]++;
	}
	
	public boolean getSeen(int x, int y) {
		return seen[x][y];
	}
	
	public void setSeen(int x, int y, boolean seen) {
		this.seen[x][y] = seen;
	}
	
	public boolean getHasTree(int x, int y) {
		return hasTree[x][y];
	}
	
	public void setHasTree(int x, int y, boolean hasTree) {
		this.hasTree[x][y] = hasTree;
	}

	public float[][] getBoardCopy() {
		int x = towerProbability.length;
		int y = towerProbability[0].length;
	    float[][] newMap = new float[x][y];
	    for (int i = 0; i < x; i++) {
	        System.arraycopy(towerProbability[i], 0, newMap[i], 0, y);
	    }
	    return newMap; 
	}

	public void print() {
		String[] value = new String[] { "0 ", "1 ", "2 ", "3 ", "4 ", "5 ", "6 ", "7 ", "8 ", "9 ", "X!" };
		// for (int i = 0; i < towerProbability.length; i++) {
			// for (int j = 0; j < towerProbability[i].length; j++) {
		for (int i = towerProbability.length - 1; i >= 0; i--) {
			for (int j = towerProbability[i].length - 1; j >= 0; j--) {
				System.out.print(value[(int)(towerProbability[i][j] * 10f + 0.01f)]);
			}
			System.out.println();
		}
	}
}
