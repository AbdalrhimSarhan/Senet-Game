
// ---------- Move representation ----------
public class Move {
    public final int fromIndex;   // 0..29
    public final int toIndex;     // 0..29 OR -1 for "exit" (optional)
    public Move(int fromIndex, int toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }
    public boolean isExit() {
        return toIndex < 0;
    }
    @Override public String toString() {
        return "(" + (fromIndex+1) + " -> " + (toIndex+1) + ")";
    }
}