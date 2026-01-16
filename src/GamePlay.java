import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class GamePlay {

    private final Random rng = new Random();
    private final Scanner in = new Scanner(System.in);

    private State board = new State();

    // 0 = Human, 1 = Computer
    private int turn = 0;

    // for later AI
    private int depthLimit = 4;
    private long nodesVisited = 0;
    private long nodesEvaluated = 0;

    public void playGame() {
        System.out.println(board);

        while (true) {
            int roll = rollSticks();
            System.out.println("Roll = " + roll);

            if (turn == 0) {
                System.out.println("************ USER ************");
                board = board.handleEndZone('H', roll);
                userTurn(roll);
                System.out.println(board);

                if (board.win('H')) {
                    System.out.println("User Wins");
                    break;
                }
                turn = 1;

            } else {
                System.out.println("************ COMPUTER ************");
                board = board.handleEndZone('C', roll);
                computerTurn(roll);
                System.out.println(board);

                if (board.win('C')) {
                    System.out.println("Computer Wins");
                    break;
                }
                turn = 0;
            }
        }
    }

    public void userTurn(int roll) {

        // (1) قاعدة 30: إذا عندك حجر على 30 -> خروج إجباري بأي رمية
        if (board.hasPieceOnSquare('H', 30)) {
            System.out.println("Forced: You must exit the piece on square 30.");
            int fromIdx = getIndexOfSquare(30);
            board = board.applyMove('H', new Move(fromIdx, -1), roll);
            return;
        }

        // (2) حركات عادية
        List<Move> moves = board.getLegalMoves('H', roll);

        if (moves.isEmpty()) {
            System.out.println("No moves are possible. Skip the turn.");
            return;
        }

        for (int i = 0; i < moves.size(); i++) {
            System.out.println(i + ") " + moves.get(i));
        }

        System.out.print("Select the move number: ");
        int choice = in.nextInt();

        if (choice < 0 || choice >= moves.size()) {
            System.out.println("Invalid choice. Skip.");
            return;
        }

        Move m = moves.get(choice);
        board = board.applyMove('H', m, roll);
    }

    public void computerTurn(int roll) {

        if (board.hasPieceOnSquare('C', 30)) {
            int fromIdx = getIndexOfSquare(30);
            board = board.applyMove('C', new Move(fromIdx, -1), roll);
            return;
        }

        nodesVisited = 0;
        nodesEvaluated = 0;

        Board_Eval best = maxMove(board, depthLimit, roll);
        board = best.getBoard();

        System.out.println("AI nodes visited: " + nodesVisited);
        System.out.println("AI nodes evaluated: " + nodesEvaluated);
    }


    // مكرر في state
    private int getIndexOfSquare(int sq) {
        // نفس PATH الموجودة في State (3x10 بشكل S)
        // squares 1..10 : idx 0..9
        if (sq >= 1 && sq <= 10) return sq - 1;

        // squares 11..20 : idx 19..10
        if (sq >= 11 && sq <= 20) return 20 - (sq - 10); // 11->19, 20->10

        // squares 21..30 : idx 20..29
        return 20 + (sq - 21);
    }

    // من اجل مراعات الاحتمالات المختلفة
    private int rollSticks() {
        int r = rng.nextInt(16); // 0..15
        if (r <= 3)  return 1;   // 4/16
        if (r <= 9)  return 2;   // 6/16
        if (r <= 13) return 3;   // 4/16
        if (r == 14) return 4;   // 1/16
        return 5;                // 1/16
    }

    public Board_Eval maxMove(State s, int depth, int roll) {
        nodesVisited++;

        if (depth == 0 || s.isTerminal()) {
            nodesEvaluated++;
            return new Board_Eval(s, s.evaluate());
        }

        List<Move> moves = s.getLegalMoves(State.COMP, roll);
        if (moves.isEmpty()) {
            return chanceMove(s, depth - 1, State.HUMAN);
        }

        double best = Double.NEGATIVE_INFINITY;
        State bestState = s;

        for (Move m : moves) {
            State next = s.applyMove(State.COMP, m, roll);
            double val = chanceMove(next, depth - 1, State.HUMAN).eval;
            if (val > best) {
                best = val;
                bestState = next;
            }
        }
        return new Board_Eval(bestState, best);
    }


    public Board_Eval minMove(State s, int depth, int roll) {
        nodesVisited++;

        if (depth == 0 || s.isTerminal()) {
            nodesEvaluated++;
            return new Board_Eval(s, s.evaluate());
        }

        List<Move> moves = s.getLegalMoves(State.HUMAN, roll);
        if (moves.isEmpty()) {
            return chanceMove(s, depth - 1, State.COMP);
        }

        double best = Double.POSITIVE_INFINITY;
        State bestState = s;

        for (Move m : moves) {
            State next = s.applyMove(State.HUMAN, m, roll);
            double val = chanceMove(next, depth - 1, State.COMP).eval;
            if (val < best) {
                best = val;
                bestState = next;
            }
        }
        return new Board_Eval(bestState, best);
    }

    public Board_Eval chanceMove(State s, int depth, char player) {
        nodesVisited++;

        if (depth == 0 || s.isTerminal()) {
            nodesEvaluated++;
            return new Board_Eval(s, s.evaluate());
        }

        double expected = 0.0;

        for (int roll = 1; roll <= 5; roll++) {
            double p = ROLL_PROB[roll];
            if (p == 0) continue;

            Board_Eval result;
            if (player == State.COMP)
                result = maxMove(s, depth, roll);
            else
                result = minMove(s, depth, roll);

            expected += p * result.eval;
        }

        return new Board_Eval(s, expected);
    }


    private static final double[] ROLL_PROB = {
            0.0,
            4.0/16.0,
            6.0/16.0,
            4.0/16.0,
            1.0/16.0,
            1.0/16.0
    };

}
