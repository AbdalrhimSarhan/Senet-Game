import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class GamePlay {

    private final Random rng = new Random();
    private final Scanner in = new Scanner(System.in);

    private State board = new State();

    // 0 = Human, 1 = Computer
    private int turn = 0;

    // for comp vs comp
    private int gameMode = 1;
    // for later AI
    private int depthLimit = 4;
    private long nodesVisited = 0;
    private long nodesEvaluated = 0;

//    public void playGame() {
//        System.out.println(board);
//        int count = 2;
//        while (true) {
//            int roll = rollSticks();
//
//            if (turn == 0) {
//                System.out.println("************ USER ************");
//                System.out.println("Roll = " + roll);
//                board = board.handleEndZone('H', roll);
//                userTurn(roll);
//                System.out.println(board);
//
//                if (board.win('H')) {
//                    System.out.println("User Wins");
//                    break;
//                }
//                turn = 1;
//
//            } else {
//                System.out.println("************ COMPUTER ************");
//                System.out.println("Roll = " + roll);
//                board = board.handleEndZone('C', roll);
//                computerTurn(roll);
//                System.out.println(board);
//
//                if (board.win('C')) {
//                    System.out.println("Computer Wins");
//                    break;
//                }
//                turn = 0;
//            }
//            count--;
//        }
//    }

    public void playGame() {

        System.out.println("Choose Game Mode:");
        System.out.println("1) Human vs Computer");
        System.out.println("2) Computer vs Computer");
        System.out.print("Your choice: ");

        gameMode = in.nextInt();

        System.out.println(board);
        int count = 2;
        while (true) {
            int roll = rollSticks();

            if (turn == 0) {
                System.out.println("************ PLAYER H ************");
                System.out.println("Roll = " + roll);

                board = board.handleEndZone('H', roll);

                if (gameMode == 1) {
                    userTurn(roll);
                } else {
                    computerHuman(roll);
                }

                System.out.println(board);

                if (board.win('H')) {
                    System.out.println("H Wins");
                    break;
                }

                turn = 1;

            } else {
                System.out.println("************ PLAYER C ************");
                System.out.println("Roll = " + roll);

                board = board.handleEndZone('C', roll);
                computerTurn(roll);
                System.out.println(board);

                if (board.win('C')) {
                    System.out.println("C Wins");
                    break;
                }

                turn = 0;
            }
            count--;
        }
    }

    public void userTurn(int roll) {

        if (board.hasPieceOnSquare('H', 30)) {
            System.out.println("Forced: You must exit the piece on square 30.");
            int fromIdx = getIndexOfSquare(30);
            board = board.applyMove('H', new Move(fromIdx, -1), roll);
            return;
        }

        List<Move> moves = board.getLegalMoves('H', roll);
        moves.sort((a, b) -> Integer.compare(
                a.fromIndex + 1 >= 11 && a.fromIndex + 1 <= 20 ? 31 - (a.fromIndex + 1) : (a.fromIndex + 1),
                b.fromIndex + 1 >= 11 && b.fromIndex + 1 <= 20 ? 31 - (b.fromIndex + 1) : (b.fromIndex + 1)
        ));

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

        Board_Eval best = maxMove(board, depthLimit, roll,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        board = best.getBoard();

        System.out.println("AI nodes visited: " + nodesVisited);
        System.out.println("AI nodes evaluated: " + nodesEvaluated);
        System.out.println("Best Evaluate Chose :" + best.eval);
    }

    public void computerHuman(int roll) {

        if (board.hasPieceOnSquare('H', 30)) {
            int fromIdx = getIndexOfSquare(30);
            board = board.applyMove('H', new Move(fromIdx, -1), roll);
            return;
        }

        nodesVisited = 0;
        nodesEvaluated = 0;

        Board_Eval best = minMove(board, depthLimit, roll,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        board = best.getBoard();

        System.out.println("AI nodes visited: " + nodesVisited);
        System.out.println("AI nodes evaluated: " + nodesEvaluated);
        System.out.println("Best Evaluate Chose :" + best.eval);
    }

    public Board_Eval maxMove(State s, int depth, int roll, double alpha, double beta) {
        nodesVisited++;

        if (depth <= 0 || s.isTerminal()) {
            nodesEvaluated++;
            return new Board_Eval(s, s.evaluate());
        }

        State current = s.handleEndZone('C', roll);

        if (current.hasPieceOnSquare('C', 30)) {
            int fromIdx = getIndexOfSquare(30);
            State next = current.applyMove('C', new Move(fromIdx, -1), roll);
            return chanceMove(next, depth - 1, 'H', alpha, beta);
        }

        List<Move> moves = current.getLegalMoves('C', roll);

        if (moves.isEmpty()) {
            return chanceMove(current, depth - 1, 'H', alpha, beta);
        }

        State bestState = current;
        double bestVal = Double.NEGATIVE_INFINITY;

        for (Move m : moves) {
            State next = current.applyMove('C', m, roll);

            Board_Eval child = chanceMove(next, depth - 1, 'H', alpha, beta);
            double val = child.getEval();

            if (val > bestVal) {
                bestVal = val;
                bestState = next;
            }

            if (bestVal > alpha) alpha = bestVal;

            if (alpha >= beta) {
                break;
            }
        }

        return new Board_Eval(bestState, bestVal);
    }

    public Board_Eval minMove(State s, int depth, int roll, double alpha, double beta) {
        nodesVisited++;

        if (depth <= 0 || s.isTerminal()) {
            nodesEvaluated++;
            return new Board_Eval(s, s.evaluate());
        }

        State current = s.handleEndZone('H', roll);

        if (current.hasPieceOnSquare('H', 30)) {
            int fromIdx = getIndexOfSquare(30);
            State next = current.applyMove('H', new Move(fromIdx, -1), roll);
            return chanceMove(next, depth - 1, 'C', alpha, beta);
        }

        List<Move> moves = current.getLegalMoves('H', roll);

        if (moves.isEmpty()) {
            return chanceMove(current, depth - 1, 'C', alpha, beta);
        }

        State bestState = current;
        double bestVal = Double.POSITIVE_INFINITY;

        for (Move m : moves) {
            State next = current.applyMove('H', m, roll);

            Board_Eval child = chanceMove(next, depth - 1, 'C', alpha, beta);
            double val = child.getEval();

            if (val < bestVal) {
                bestVal = val;
                bestState = next;
            }
            if (bestVal < beta) beta = bestVal;

            if (alpha >= beta) {
                break;
            }
        }

            return new Board_Eval(bestState, bestVal);
    }


    public Board_Eval chanceMove(State s, int depth, char playerToMove, double alpha, double beta) {
        nodesVisited++;

        if (depth <= 0 || s.isTerminal()) {
            nodesEvaluated++;
            return new Board_Eval(s, s.evaluate());
        }

        double expected = 0.0;

        for (int roll = 1; roll <= 5; roll++) {
            double p;
            if (roll == 1) p = 4.0 / 16.0;
            else if (roll == 2) p = 6.0 / 16.0;
            else if (roll == 3) p = 4.0 / 16.0;
            else if (roll == 4) p = 1.0 / 16.0;
            else p = 1.0 / 16.0;

            Board_Eval res;
            if (playerToMove == 'C') {
                res = maxMove(s, depth, roll, alpha, beta);
            } else {
                res = minMove(s, depth, roll, alpha, beta);
            }

            expected += p * res.getEval();
        }

        return new Board_Eval(s, expected);
    }



    private int getIndexOfSquare(int sq) {

        if (sq >= 1 && sq <= 10) return sq - 1;

        if (sq >= 11 && sq <= 20) return 20 - (sq - 10);

        return 20 + (sq - 21);
    }

    private int rollSticks() {
        int r = rng.nextInt(16); // 0..15
        if (r <= 3)  return 1;   // 4/16
        if (r <= 9)  return 2;   // 6/16
        if (r <= 13) return 3;   // 4/16
        if (r == 14) return 4;   // 1/16
        return 5;                // 1/16
    }


}
