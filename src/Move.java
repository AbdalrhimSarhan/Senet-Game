// ---------- Move representation ----------
public class Move {
    public final int fromIndex;
    public final int toIndex;
    public Move(int fromIndex, int toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public boolean isExit() {
        return toIndex < 0;
    }

    private int toVisualSquare(int index) {
        int sq = index + 1;

        if (sq >= 1 && sq <= 10) return sq;

        if (sq >= 11 && sq <= 20) return 31 - sq;

        return sq;
    }

    @Override
    public String toString() {
        if (isExit()) {
            return "(" + toVisualSquare(fromIndex) + " -> OUT)";
        }

        int fromV = toVisualSquare(fromIndex);
        int toV   = toVisualSquare(toIndex);

        return "(" + fromV + " -> " + toV + ")";
    }
}
