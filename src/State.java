import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class State {
    public static final int N = 30;

    private final char[] cells;

    private int humanOut;
    private int computerOut;

    public static final char HUMAN = 'H';
    public static final char COMP = 'C';
    public static final char EMPTY = '.';

    public static final int Checkpoint = 15;
    public static final int Wall = 26;
    public static final int GoToCheckpoint = 27;

    private static final int[] PATH = buildPath();

    private static final int[] SQUARE_OF_INDEX = buildSquareOfIndex();

    private static int[] buildPath() {
        int[] p = new int[30];
        int k = 0;

        for (int idx = 0; idx <= 9; idx++) p[k++] = idx;

        for (int idx = 19; idx >= 10; idx--) p[k++] = idx;

        for (int idx = 20; idx <= 29; idx++) p[k++] = idx;

        return p;
    }

    private static int[] buildSquareOfIndex() {
        int[] inv = new int[30];
        for (int sq = 1; sq <= 30; sq++) {
            int idx = PATH[sq - 1];
            inv[idx] = sq;
        }
        return inv;
    }

    public State() {
        this.cells = new char[N];
        Arrays.fill(this.cells, EMPTY);
        defaultSetupStart();
    }

    private State(char[] cells, int humanOut, int computerOut) {
        this.cells = cells;
        this.humanOut = humanOut;
        this.computerOut = computerOut;
    }

    public State copy() {
        return new State(Arrays.copyOf(cells, N), humanOut, computerOut);
    }

    private void defaultSetupStart() {
        Arrays.fill(cells, EMPTY);

        for (int sq = 1; sq <= 14; sq++) {
            int idx = indexOfSquare(sq);
            cells[idx] = (sq % 2 == 1) ? HUMAN : COMP;
        }
        humanOut = 0;
        computerOut = 0;
    }

    private int squareOfIndex(int idx) {
        return SQUARE_OF_INDEX[idx];
    }

    private int indexOfSquare(int sq) {
        return PATH[sq - 1];
    }

    private boolean isInsideBoardSquare(int sq) {
        return sq >= 1 && sq <= 30;
    }


    public char at(int index) {
        return cells[index];
    }

    public boolean isTerminal() {
        return humanOut >= 7 || computerOut >= 7;
    }

    public boolean win(char player) {
        if (player == HUMAN) return humanOut >= 7;
        return computerOut >= 7;
    }

    public List<Move> getLegalMoves(char player, int roll) {
        List<Move> moves = new ArrayList<>();

        for (int fromIdx = 0; fromIdx < N; fromIdx++) {
            if (cells[fromIdx] != player) continue;

            int destIdx = computeDestinationIndex(fromIdx, roll);

            if (destIdx == -2) continue;

            if (destIdx == -1) {
                moves.add(new Move(fromIdx, -1));
                continue;
            }

            if (cells[destIdx] == player) continue;

            moves.add(new Move(fromIdx, destIdx));
        }

        return moves;
    }

    public State applyMove(char player, Move move, int roll) {
        State s = this.copy();

        int fromIdx = move.fromIndex;
        char opp = (player == HUMAN) ? COMP : HUMAN;

        if (fromIdx < 0 || fromIdx >= N) return s;
        if (s.cells[fromIdx] != player) return s;

        if (move.isExit()) {
            s.cells[fromIdx] = EMPTY;
            if (player == HUMAN) s.humanOut++;
            else s.computerOut++;
            return s;
        }

        int toIdx = move.toIndex;
        if (toIdx < 0 || toIdx >= N) return s;

        char target = s.cells[toIdx];

        s.cells[fromIdx] = EMPTY;

        if (target == opp) {
            s.cells[toIdx] = player;
            s.cells[fromIdx] = opp;
        } else {
            s.cells[toIdx] = player;
        }

        int toSq = s.squareOfIndex(toIdx);

        if (toSq == GoToCheckpoint) {
            s.cells[toIdx] = EMPTY;
            s.placeOnCheckpoint(player);
        }

        return s;
    }


    private void placeOnCheckpoint(char player) {
        for (int sq = Checkpoint; sq >= 1; sq--) {
            int idx = indexOfSquare(sq);
            if (cells[idx] == EMPTY) {
                cells[idx] = player;
                break;
            }
        }
    }


    public int computeDestinationIndex(int fromIndex, int roll) {
        int fromSq = squareOfIndex(fromIndex);   // 1..30
        int destSq = fromSq + roll;


        if (destSq > 30) {
            return -1;
        }

        if (fromSq < Wall && destSq > Wall) {
            return -2;
        }

        return indexOfSquare(destSq);
    }

    public boolean isCheckpointSquare(int squareNumber1to30) {
        return squareNumber1to30 == Checkpoint || squareNumber1to30 >= 26;
    }


    public double evaluate() {

        if (win(COMP))  return 10000;
        if (win(HUMAN)) return -10000;
        // فيكم تعتبروا هي تثقيلة يعني معيارنا 20% للتقدم و 80% للأمان والمحافظة على امان القطع
         double W_SAFETY   = 0.70;
         double W_PROGRESS = 0.30;

        double score = 0.0;

        for (int idx = 0; idx < N; idx++) {
            char p = cells[idx];
            if (p == EMPTY) continue;

            int sq = squareOfIndex(idx);

            double danger = 0.0;
            char enemy = (p == COMP) ? HUMAN : COMP;

            for (int k = 1; k <= 5; k++) {
                int fromSq = sq - k;
                if (fromSq < 1) break;

                if (fromSq < Wall && sq > Wall) continue;

                int fromIdx = indexOfSquare(fromSq);
                if (cells[fromIdx] == enemy) {
                    danger += ROLL_PROB[k];
                }
            }

            if (danger > 1.0) danger = 1.0;
            double safety = 1.0 - danger;

            double pieceBase = safety * 30.0;
            double bonus = specialBonus(sq);

            if (p == COMP) score += (pieceBase + bonus);
            else score -= (pieceBase + bonus);
        }

        score += computerOut * 50;
        score -= humanOut * 50;

        return score;
    }



    public boolean hasPieceOnSquare(char player, int square1to30) {
        int idx = indexOfSquare(square1to30);
        return cells[idx] == player;
    }

    public State handleEndZone(char player, int roll) {
        State s = this.copy();

        if (roll != 3 && s.hasPieceOnSquare(player, 28)) {
            int idx = s.indexOfSquare(28);
            s.cells[idx] = EMPTY;
            s.placeOnCheckpoint(player);
        }

        if (roll != 2 && s.hasPieceOnSquare(player, 29)) {
            int idx = s.indexOfSquare(29);
            s.cells[idx] = EMPTY;
            s.placeOnCheckpoint(player);
        }

        return s;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 10; c++) {
                int idx = r * 10 + c;
                int sq = SQUARE_OF_INDEX[idx];
                char occ = cells[idx];

                if (occ == EMPTY && isCheckpointSquare(sq)) {
                    sb.append('[').append(symbolForSpecial(sq)).append(']').append(' ');
                } else {
                    sb.append(' ').append(occ).append(' ').append(' ');
                }
            }
            sb.append('\n');
        }
        sb.append("Out: H=").append(humanOut).append(" C=").append(computerOut).append('\n');
        return sb.toString();
    }

    private char symbolForSpecial(int sq) {
        if (sq == Checkpoint) return 'R';      // 15
        if (sq == Wall) return 'S';    // 26
        if (sq == GoToCheckpoint) return 'W';        // 27
        if (sq == 28) return 'A';
        if (sq == 29) return 'B';
        if (sq == 30) return 'D';
        return '?';
    }

    private static final double[] ROLL_PROB = {
            0.0,
            4.0 / 16.0,
            6.0 / 16.0,
            4.0 / 16.0,
            1.0 / 16.0,
            1.0 / 16.0
    };

    public double specialBonus(int sq) {

        switch (sq) {
            case 26: return 6 + 4;
            case 27: return -20;
            case 28: return 12;
            case 29: return 14;
            case 30: return 25;
            default: return 0;
        }
    }

}
