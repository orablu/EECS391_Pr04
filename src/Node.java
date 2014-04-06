
public class Node {
	private int x;
	private int y;
	private float probability;
	private Node parent;
	
	public Node(int x, int y, float probability) {
		this.x = x;
		this.y = y;
		this.probability = probability;
	}

	public Node(Node node, Node parent) {
		this.x = node.x;
		this.y = node.y;
		this.probability = node.probability;
		this.parent = parent;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public float getProbability() {
		return probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
            return false;
		}
        if (o == this) {
            return true;
        }
        if (!(o instanceof Node)) {
            return false;
        }
        
        Node node = (Node) o;
        return x == node.getX() && y == node.getY();
	}

	public int getCost() {
		return this.getHeuristicCost() + this.getAccumulatedCost();
	}

	private int getAccumulatedCost() {
		// TODO accumulate probabilities in some way
		int accumCost;
		if (parent == null) {
            accumCost = 0;
		} else {
//            accumCost = parent.getAccumulatedCost() + 1;
		}
		return 0;
	}

	private int getHeuristicCost() {
		// TODO Heuristic this somehow
		return 0;
	}
}
