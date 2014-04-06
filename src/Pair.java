public class Pair<T, U> {
    private T x; //first member of pair
    private U y; //second member of pair

    public Pair(T x, U y) {
        this.x = x;
        this.y = y;
    }

    public void setX(T x) {
        this.x = x;
    }

    public void setY(U y) {
        this.y = y;
    }

    public T getX() {
        return x;
    }

    public U getY() {
        return y;
    }
}