package chessmate.player.ai;

import chessmate.board.Board;

public interface BoardEvaluator {

    int evaluate(Board board, int depth);

}
