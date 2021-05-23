/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chessmate.board;

import chessmate.piece.Piece;
import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
/**
 *
 * @author artog
 */
public abstract class Tile {

    protected final int tileCoordinate;
    
    private static final Map<Integer, EmptyTile> EMPTY_TILES = createAllPossibleEmptyTiles();
    
     private static Map<Integer, EmptyTile> createAllPossibleEmptyTiles() {
        final Map<Integer, EmptyTile> emptyTileMap = new HashMap<>();
        
        for(int i=0; i<64; i++)
        {
            emptyTileMap.put(i, new EmptyTile(i));
        }
        
        return ImmutableMap.copyOf(emptyTileMap);
    }
     
//    static Tile createTile(final int coordinate, final Piece piece) {
//        return  piece != null ? new OccupiedTile(tileCoordinate, piece) : EMPTY_TILES.get(tileCoordinate);
//    }
     
    Tile(int tileCoordinate)
    {
        this.tileCoordinate = tileCoordinate;
    }
    
    public abstract boolean isTileOccupied();
    
    public abstract Piece getPiece();
    
    public static final class EmptyTile extends Tile
    {
        private EmptyTile(final int coordinate)
        {
            super(coordinate);
        }
        
        @Override 
        public boolean isTileOccupied()
        {
            return false;
        }
        
        @Override 
        public Piece getPiece()
        {
            return null;
        }
    }
    
    public static final class OccupiedTile extends Tile
    {
        private  final Piece pieceOnTile;
        
        private OccupiedTile(final int tileCoordinate, Piece pieceOnTile)
        {
            super(tileCoordinate);
            this.pieceOnTile = pieceOnTile;
        }
        
        @Override 
        public boolean isTileOccupied()
        {
            return true;
        }
        
        @Override 
        public Piece getPiece()
        {
            return pieceOnTile;
        }
    }
}
