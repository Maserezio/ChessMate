/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chessmate.piece;

/**
 *
 * @author artog
 */
public class Piece {
    
    protected final int piecePosition;
    protected final Alliance pieceAlliance;
    
    Piece(final int piecePosition, final Alliance pieceAlliance) {
    this.pieceAllaince = pieceAlliance;
    this.piecePosition = piecePosition;
    
    }
}
