package chessmate.gui;

import chessmate.board.*;
import chessmate.board.Move;
import chessmate.pieces.Piece;
import chessmate.player.Player;
import chessmate.player.ai.StandardBoardEvaluator;
import chessmate.player.ai.StockAlphaBeta;
//import chess.pgn.MySqlGamePersistence;
import com.google.common.collect.Lists;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static javax.swing.JFrame.setDefaultLookAndFeelDecorated;
import static javax.swing.SwingUtilities.*;
/**
 *
 * @author Yaroslav
 */
public class MainFrame extends javax.swing.JFrame {
    
    Dimension OUTER_FRAME_DIMENSION = new Dimension(700, 600);
    Dimension BOARD_PANEL_DIMENSION = new Dimension(400, 350);
    Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);
    private BoardPanel boardPanel;
    private Board chessBoard;
    private BoardDirection boardDirection;
    private GameSetup gameSetup;
    private Piece sourceTile;
    private Piece humanMovedPiece;
    private GameHistoryPanel gameHistoryPanel;
    private TakenPiecesPanel takenPiecesPanel;
    private MoveLog moveLog;
    private Move computerMove;
    private Color lightTileColor = Color.decode("#779556");
    private Color darkTileColor = Color.decode("#ebecd0");
    private boolean highlightLegalMoves;
    private boolean useBook;
    private String pieceIconPath = "art/figures/";
    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();
        //addObserver(new TableGameAIWatcher());
        this.setSize(OUTER_FRAME_DIMENSION);
        final JMenuBar tableMenuBar = new JMenuBar();
        this.setJMenuBar(tableMenuBar);
        this.setLayout(new BorderLayout());
        chessBoard = Board.createStandardBoard();
        boardDirection = BoardDirection.NORMAL;
        highlightLegalMoves = true;
        useBook = false;
        gameHistoryPanel = new GameHistoryPanel();
        takenPiecesPanel = new TakenPiecesPanel();
        boardPanel = new BoardPanel();
        moveLog = new MoveLog();
        gameSetup = new GameSetup(this, true);
        setDefaultLookAndFeelDecorated(true);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); 
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = this.getSize().width;
        final int h = this.getSize().height;
        final int x = (dim.width - w) / 2;
        final int y = (dim.height - h) / 2;
        this.setLocation(x, y);
        tableMenuBar.add(createGameMenu());
        tableMenuBar.add(createPreferencesMenu());
        add(this.takenPiecesPanel, BorderLayout.WEST);
        add(this.boardPanel, BorderLayout.CENTER);
        add(this.gameHistoryPanel, BorderLayout.EAST);
    }
    
    private MoveLog getMoveLog() {
        return this.moveLog;
    }
    
    private GameSetup getGameSetup() {
        return gameSetup;
    }
    
    private Board getGameBoard() {
        return chessBoard;
    }
    
    private boolean getUseBook() {
        return useBook;
    }
    
    private void moveMadeUpdate(final PlayerType playerType) {
        /*
        setChanged();
        notifyObservers(playerType);
        */
    }
    
    private void updateGameBoard(final Board board) {
        chessBoard = board;
    }

    private void updateComputerMove(final Move move) {
        computerMove = move;
    }
    
    private GameHistoryPanel getGameHistoryPanel() {
        return gameHistoryPanel;
    }

    private TakenPiecesPanel getTakenPiecesPanel() {
        return takenPiecesPanel;
    }
    
    private void setupUpdate(final GameSetup gameSetup) {
        /*
        setChanged();
        notifyObservers(gameSetup);
        */
    }
    
    private void undoAllMoves() {
        for(int i = getMoveLog().size() - 1; i >= 0; i--) {
            final Move lastMove = getMoveLog().removeMove(getMoveLog().size() - 1);
            this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        }
        this.computerMove = null;
        getMoveLog().clear();
        getGameHistoryPanel().redo(chessBoard, getMoveLog());
        getTakenPiecesPanel().redo(getMoveLog());
        getBoardPanel().drawBoard(chessBoard);
    }
    
    private void undoLastMove() {
        final Move lastMove = getMoveLog().removeMove(getMoveLog().size() - 1);
        this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        this.computerMove = null;
        getMoveLog().removeMove(lastMove);
        getGameHistoryPanel().redo(chessBoard, getMoveLog());
        getTakenPiecesPanel().redo(getMoveLog());
        getBoardPanel().drawBoard(chessBoard);
    }
    
    private boolean getHighlightLegalMoves() {
        return highlightLegalMoves;
    }
    
    private BoardPanel getBoardPanel() {
        return boardPanel;
    }
    
    private class AIThinkTank extends SwingWorker<Move, String> {

        private AIThinkTank() {
        }

        @Override
        protected Move doInBackground() {
            final Move bestMove;
            final Move bookMove = getUseBook()
                    ? MySqlGamePersistence.get().getNextBestMove(getGameBoard(),
                    getGameBoard().currentPlayer(),
                    getMoveLog().getMoves().toString().replaceAll("\\[", "").replaceAll("]", ""))
                    : MoveFactory.getNullMove();
            if (getUseBook() && bookMove != MoveFactory.getNullMove()) {
                bestMove = bookMove;
            }
            else {
                final StockAlphaBeta strategy = new StockAlphaBeta(getGameSetup().getSearchDepth());
                //strategy.addObserver(getDebugPanel());
                bestMove = strategy.execute(getGameBoard());
            }
            return bestMove;
        }

        @Override
        public void done() {
            try {
                final Move bestMove = get();
                updateComputerMove(bestMove);
                updateGameBoard(getGameBoard().currentPlayer().makeMove(bestMove).getToBoard());
                getMoveLog().addMove(bestMove);
                getGameHistoryPanel().redo(getGameBoard(), getMoveLog());
                getTakenPiecesPanel().redo(getMoveLog());   
                getBoardPanel().drawBoard(getGameBoard());
                moveMadeUpdate(PlayerType.COMPUTER);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private class TableGameAIWatcher
            implements Observer {

        @Override
        public void update(final Observable o,
                           final Object arg) {

            if (getGameSetup().isAIPlayer(getGameBoard().currentPlayer()) &&
                !getGameBoard().currentPlayer().isInCheckMate() &&
                !getGameBoard().currentPlayer().isInStaleMate()) {
                System.out.println(getGameBoard().currentPlayer() + " is set to AI, thinking....");
                final AIThinkTank thinkTank = new AIThinkTank();
                thinkTank.execute();
            }

            if (getGameBoard().currentPlayer().isInCheckMate()) {
                JOptionPane.showMessageDialog(getBoardPanel(),
                        "Game Over: Player " + getGameBoard().currentPlayer() + " is in checkmate!", "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            if (getGameBoard().currentPlayer().isInStaleMate()) {
                JOptionPane.showMessageDialog(getBoardPanel(),
                        "Game Over: Player " + getGameBoard().currentPlayer() + " is in stalemate!", "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        }

    }
    
    private JMenu createGameMenu() {
        final JMenu gameMenu = new JMenu("Игра");
        gameMenu.setMnemonic(KeyEvent.VK_F);
        
        final JMenuItem resetMenuItem = new JMenuItem("Новая игра", KeyEvent.VK_P);
        resetMenuItem.addActionListener(e -> undoAllMoves());
        gameMenu.add(resetMenuItem);
        
        final JMenuItem undoMoveMenuItem = new JMenuItem("Отменить ход", KeyEvent.VK_M);
        undoMoveMenuItem.addActionListener(e -> {
            if(getMoveLog().size() > 0) {
                undoLastMove();
            }
        });
        gameMenu.add(undoMoveMenuItem);

        final JMenuItem exitMenuItem = new JMenuItem("Выход", KeyEvent.VK_X);
        exitMenuItem.addActionListener(e -> {
            System.exit(0);
        });
        gameMenu.add(exitMenuItem);

        return gameMenu;
    }
    
    private JMenu createPreferencesMenu() {

        final JMenu preferencesMenu = new JMenu("Опции");

        final JMenuItem flipBoardMenuItem = new JMenuItem("Перевернуть доску");
        flipBoardMenuItem.addActionListener(e -> {
            boardDirection = boardDirection.opposite();
            boardPanel.drawBoard(chessBoard);
        });
        preferencesMenu.add(flipBoardMenuItem);
        
        final JMenuItem preferencesMenuItem = new JMenuItem("Настройки");
        flipBoardMenuItem.addActionListener(e -> {
            getGameSetup().promptUser();
            setupUpdate(getGameSetup());
        });
        preferencesMenu.add(preferencesMenuItem);
        
        return preferencesMenu;
    }
    
    public static class MoveLog {

        private final List<Move> moves;

        MoveLog() {
            this.moves = new ArrayList<>();
        }

        public List<Move> getMoves() {
            return this.moves;
        }

        void addMove(final Move move) {
            this.moves.add(move);
        }

        public int size() {
            return this.moves.size();
        }

        void clear() {
            this.moves.clear();
        }

        Move removeMove(final int index) {
            return this.moves.remove(index);
        }

        boolean removeMove(final Move move) {
            return this.moves.remove(move);
        }

    }

    private class BoardPanel extends JPanel {

        final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8,8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Color.decode("#8B4726"));
            validate();
        }

        void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel boardTile : boardDirection.traverse(boardTiles)) {
                boardTile.drawTile(board);
                add(boardTile);
            }
            validate();
            repaint();
        }

        void setTileDarkColor(final Board board,
                              final Color darkColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setDarkTileColor(darkColor);
            }
            drawBoard(board);
        }

        void setTileLightColor(final Board board,
                                      final Color lightColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setLightTileColor(lightColor);
            }
            drawBoard(board);
        }

    }

    enum BoardDirection {
        NORMAL {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return Lists.reverse(boardTiles);
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);
        abstract BoardDirection opposite();

    }
    
    enum PlayerType {
        HUMAN,
        COMPUTER
    }
    
    private class TilePanel extends JPanel {

        private final int tileId;

        TilePanel(final BoardPanel boardPanel,
                  final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignTilePieceIcon(chessBoard);
            highlightTileBorder(chessBoard);
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent event) {

                    if(getGameSetup().isAIPlayer(getGameBoard().currentPlayer()) ||
                       BoardUtils.isEndGame(getGameBoard())) {
                        return;
                    }

                    if (isRightMouseButton(event)) {
                        sourceTile = null;
                        humanMovedPiece = null;
                    } else if (isLeftMouseButton(event)) {
                        if (sourceTile == null) {
                            sourceTile = chessBoard.getPiece(tileId);
                            humanMovedPiece = sourceTile;
                            if (humanMovedPiece == null) {
                                sourceTile = null;
                            }
                        } else {
                            final Move move = MoveFactory.createMove(chessBoard, sourceTile.getPiecePosition(),
                                    tileId);
                            final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                            if (transition.getMoveStatus().isDone()) {
                                chessBoard = transition.getToBoard();
                                moveLog.addMove(move);
                            }
                            sourceTile = null;
                            humanMovedPiece = null;
                        }
                    }
                    invokeLater(() -> {
                        gameHistoryPanel.redo(chessBoard, moveLog);
                        takenPiecesPanel.redo(moveLog);
                        //if (gameSetup.isAIPlayer(chessBoard.currentPlayer())) {
                            moveMadeUpdate(PlayerType.HUMAN);
                        //}
                        boardPanel.drawBoard(chessBoard);
                    });
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                }
            });
            validate();
        }

        void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightTileBorder(board);
            highlightLegals(board);
            highlightAIMove();
            validate();
            repaint();
        }

        void setLightTileColor(final Color color) {
            lightTileColor = color;
        }

        void setDarkTileColor(final Color color) {
            darkTileColor = color;
        }

        private void highlightTileBorder(final Board board) {
            if(humanMovedPiece != null &&
               humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance() &&
               humanMovedPiece.getPiecePosition() == this.tileId) {
                setBorder(BorderFactory.createLineBorder(Color.cyan));
            } else {
                setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        }

        private void highlightAIMove() {
            if(computerMove != null) {
                if(this.tileId == computerMove.getCurrentCoordinate()) {
                    setBackground(Color.pink);
                } else if(this.tileId == computerMove.getDestinationCoordinate()) {
                    setBackground(Color.red);
                }
            }
        }

        private void highlightLegals(final Board board) {
            if (getHighlightLegalMoves()) {
                for (final Move move : pieceLegalMoves(board)) {
                    if (move.getDestinationCoordinate() == this.tileId) {
                        try {
                            add(new JLabel(new ImageIcon(ImageIO.read(new File("art/misc/green_dot.png")))));
                        }
                        catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if(humanMovedPiece != null && humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance()) {
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }

        private void assignTilePieceIcon(final Board board) {
            this.removeAll();
            if(board.getPiece(this.tileId) != null) {
                try{
                    final BufferedImage image = ImageIO.read(new File(pieceIconPath +
                            board.getPiece(this.tileId).getPieceAllegiance().toString().substring(0, 1) + "" +
                            board.getPiece(this.tileId).toString() +
                            ".gif"));
                    add(new JLabel(new ImageIcon(image)));
                } catch(final IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void assignTileColor() {
            if (BoardUtils.INSTANCE.FIRST_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.THIRD_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.FIFTH_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.SEVENTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            } else if(BoardUtils.INSTANCE.SECOND_ROW.get(this.tileId) ||
                      BoardUtils.INSTANCE.FOURTH_ROW.get(this.tileId) ||
                      BoardUtils.INSTANCE.SIXTH_ROW.get(this.tileId)  ||
                      BoardUtils.INSTANCE.EIGHTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }
        }
        
        
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ChessMate");
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("mephi_circle.png")).getImage());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 633, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 520, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
