import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class State {
    public static final int N = 30;

    // Occupancy only (row-major 3x10)
    private final char[] cells;

    private int humanOut;
    private int computerOut;

    public static final char HUMAN = 'H';
    public static final char COMP  = 'C';
    public static final char EMPTY = '.';

    // Special squares by number (1..30)
    public static final int REBIRTH   = 15; // checkpoint
    public static final int HAPPINESS = 26; // must pass through, no jumping over
    public static final int WATER     = 27; // go to rebirth

    // --- S-path mapping ---
    // path[sq-1] = index (0..29) in cells[]
    private static final int[] PATH = buildPath();
    // squareOfIndex[idx] = sq (1..30)
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
        initDefaultSetup();
    }

    private State(char[] cells, int humanOut, int computerOut) {
        this.cells = cells;
        this.humanOut = humanOut;
        this.computerOut = computerOut;
    }

    public State copy() {
        return new State(Arrays.copyOf(cells, N), humanOut, computerOut);
    }

    private void initDefaultSetup() {
        for (int sq = 1; sq <= 14; sq++) {
            int idx = indexOfSquare(sq);
            cells[idx] = (sq % 2 == 1) ? HUMAN : COMP;
        }
        humanOut = 0;
        computerOut = 0;
    }

    private int squareOfIndex(int idx) {
        return SQUARE_OF_INDEX[idx]; // 1..30
    }

    private int indexOfSquare(int sq) {
        return PATH[sq - 1];
    }

    private boolean isInsideBoardSquare(int sq) {
        return sq >= 1 && sq <= 30;
    }


    public char at(int index) { return cells[index]; }

    public boolean isTerminal() {
        return humanOut >= 7 || computerOut >= 7;
    }

    public boolean win(char player) {
        if (player == HUMAN) return humanOut >= 7;
        return computerOut >= 7;
    }

    public List<Move> getLegalMoves(char player, int roll) {
        List<Move> moves = new ArrayList<>();

        // مساعدة: إذا حجر على 28/29/30، لا يخرج إلا برمية محددة (نسخة مباشرة)
        // + لا يمكن القفز فوق 26: إذا كنت قبل 26 ووجهتك بعده، لازم تهبط عليه بالضبط.
        for (int fromIdx = 0; fromIdx < N; fromIdx++) {
            if (cells[fromIdx] != player) continue;

            int fromSq = squareOfIndex(fromIdx);

            if (fromSq == 28) {
                if (roll == 3) moves.add(new Move(fromIdx, -1));
                continue;
            }
            if (fromSq == 29) {
                if (roll == 2) moves.add(new Move(fromIdx, -1));
                continue;
            }
            if (fromSq == 30) {
                // حسب طلبك: أي رمية تخرج الحجر
                moves.add(new Move(fromIdx, -1));
                continue;
            }

            int toIdx = computeDestinationIndex(fromIdx, roll);

            // -2 = حركة غير صالحة / خارج النطاق
            if (toIdx == -2) continue;

            // -1 = خروج (يجب أن يكون بالضبط خارج 30)
            if (toIdx == -1) {
                // من غير المربعات 28/29/30: يسمح بالخروج إذا كانت الحركة بالضبط = 31
                // (يعني من 30 غير وارد هنا لأننا تعاملنا معه فوق)
                moves.add(new Move(fromIdx, -1));
                continue;
            }

            // لا تهبط على حجر لنفس اللاعب
            if (cells[toIdx] == player) continue;

            // حركة قانونية: إما فراغ أو حجر خصم (swap)
            moves.add(new Move(fromIdx, toIdx));
        }

        return moves;
    }

    // ---------- Applying a move ----------
    public State applyMove(char player, Move move, int roll) {
        State s = this.copy();

        int fromIdx = move.fromIndex;
        char opp = (player == HUMAN) ? COMP : HUMAN;

        // حماية بسيطة
        if (fromIdx < 0 || fromIdx >= N) return s;
        if (s.cells[fromIdx] != player) return s;

        // خروج
        if (move.isExit()) {
            s.cells[fromIdx] = EMPTY;
            if (player == HUMAN) s.humanOut++;
            else s.computerOut++;
            return s;
        }

        int toIdx = move.toIndex;
        if (toIdx < 0 || toIdx >= N) return s;

        // نفذ النقل + swap إذا على الخصم
        if (s.cells[toIdx] == opp) {
            // swap
            s.cells[toIdx] = player;
            s.cells[fromIdx] = opp;
        } else {
            // normal move
            s.cells[toIdx] = player;
            s.cells[fromIdx] = EMPTY;
        }

        // طبق قواعد المربعات الخاصة بعد الهبوط
        int toSq = s.squareOfIndex(toIdx);

        // 27: House of Water => العودة فوراً إلى 15
        if (toSq == WATER) {
            s.cells[toIdx] = EMPTY;
            s.placeOnRebirth(player);
        }

        return s;
    }

    // ضع الحجر على 15، وإذا كانت مشغولة: أول مربع فارغ قبلها (14..1)
    private void placeOnRebirth(char player) {
        int rebirthIdx = indexOfSquare(REBIRTH);
        if (cells[rebirthIdx] == EMPTY) {
            cells[rebirthIdx] = player;
            return;
        }
        for (int sq = REBIRTH - 1; sq >= 1; sq--) {
            int idx = indexOfSquare(sq);
            if (cells[idx] == EMPTY) {
                cells[idx] = player;
                return;
            }
        }

    }

    // ---------- Path / destination ----------
    // return:
    //   0..29 destination index
    //   -1 for EXIT
    //   -2 invalid move
    public int computeDestinationIndex(int fromIndex, int roll) {
        int fromSq = squareOfIndex(fromIndex);
        int destSq = fromSq + roll;

        // لا قفز فوق 26: إذا كنت قبل 26 ووجهتك بعده، لازم تهبط على 26 بالضبط
        if (fromSq < HAPPINESS && destSq > HAPPINESS && destSq != HAPPINESS) {
            return -2;
        }

        // الخروج يجب أن يكون "بالضبط" بعد 30 (31)
        if (destSq == 31) return -1;
        if (!isInsideBoardSquare(destSq)) return -2;

        return indexOfSquare(destSq);
    }

    // ---------- Special squares helpers ----------
    public boolean isCheckpointSquare(int squareNumber1to30) {
        return squareNumber1to30 == REBIRTH || squareNumber1to30 >= 26;
    }

    // ---------- Evaluation (تجهيز للـ AI) ----------
    // Heuristic بسيط: تقدّم الكمبيوتر - تقدّم الإنسان
    // (كلما كان رقم المربع أكبر كان أفضل) + مكافأة كبيرة للخروج.
    public double evaluate() {
        double score = 0.0;

        for (int idx = 0; idx < N; idx++) {
            char p = cells[idx];
            if (p == EMPTY) continue;

            int sq = squareOfIndex(idx);
            if (p == COMP) score += sq;
            else score -= sq;
        }

        score += computerOut * 40.0;
        score -= humanOut * 40.0;

        return score;
    }

    public boolean hasPieceOnSquare(char player, int square1to30) {
        int idx = indexOfSquare(square1to30);
        return cells[idx] == player;
    }

    public State applyEndZoneReturnIfNeeded(char player, int roll) {
        State s = this.copy();

        // إذا حجر على 29 ولم تظهر 2 في هذا الدور (دور نفس اللاعب) يرجع إلى 15
        if (s.hasPieceOnSquare(player, 29) && roll != 2) {
            int fromIdx = s.indexOfSquare(29);
            s.cells[fromIdx] = EMPTY;
            s.placeOnRebirth(player);
        }

        // إذا حجر على 28 ولم تظهر 3 في هذا الدور يرجع إلى 15
        if (s.hasPieceOnSquare(player, 28) && roll != 3) {
            int fromIdx = s.indexOfSquare(28);
            s.cells[fromIdx] = EMPTY;
            s.placeOnRebirth(player);
        }

        return s;
    }

    // ---------- Printing ----------
    @Override
    public String toString() {
        // طباعة 3 صفوف × 10 أعمدة، مع تمييز checkpoints أثناء العرض فقط
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
        if (sq == REBIRTH) return 'R';      // 15
        if (sq == HAPPINESS) return 'S';    // 26
        if (sq == WATER) return 'W';        // 27
        if (sq == 28) return 'A';
        if (sq == 29) return 'B';
        if (sq == 30) return 'D';
        return '?';
    }
}
