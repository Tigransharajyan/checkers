package com.checkers.engine;

import com.checkers.model.enums.PieceColor;

/**
 * Immutable piece color and MAN/KING type. Promotion creates a new object so board copies can
 * safely share piece references.
 */
public record Piece(PieceColor color, PieceType type) {

    /**
     * Tests whether this piece has type KING.
     */
    public boolean isKing() {
        return type == PieceType.KING;
    }

    /**
     * Encodes color and type as WM, WK, BM or BK for board serialization.
     */
    public String code() {
        char side = color == PieceColor.WHITE ? 'W' : 'B';
        char kind = type == PieceType.KING ? 'K' : 'M';
        return "" + side + kind;
    }

    /**
     * Restores a piece from its two-character code. Checks the length; the server produces codes
     * from its fixed set of values.
     */
    public static Piece fromCode(String code) {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("Invalid piece code: " + code);
        }
        PieceColor color = code.charAt(0) == 'W' ? PieceColor.WHITE : PieceColor.BLACK;
        PieceType type = code.charAt(1) == 'K' ? PieceType.KING : PieceType.MAN;
        return new Piece(color, type);
    }

    /**
     * Returns a new king of the same color without modifying this immutable piece.
     */
    public Piece promote() {
        return new Piece(color, PieceType.KING);
    }
}
