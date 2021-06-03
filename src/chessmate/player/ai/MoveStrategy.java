package chessmate.player.ai;

import chessmate.board.Board;
import chessmate.board.Move;

public interface MoveStrategy {

    long getNumBoardsEvaluated();

    Move execute(Board board);

}
