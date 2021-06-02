package chessmate.pgn;

import chessmate.board.Board;
import chessmate.board.Move;
import chessmate.player.Player;

public interface PGNPersistence {

    void persistGame(Game game);

    Move getNextBestMove(Board board, Player player, String gameText);

}
