public class Board_Eval {
    State Board;
    double  eval;

    public Board_Eval(State Board, double  eval) {
        this.Board = Board;
        this.eval = eval;
    }

    public State getBoard() {
        return Board;
    }
    public double getEval() { return eval; }
}
