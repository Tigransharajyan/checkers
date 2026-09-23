package com.checkers.engine;

import com.checkers.model.enums.PieceColor;

public record Piece(PieceColor color, PieceType type) {

    public boolean isKing() {
        return type == PieceType.KING;
    }

    public String code() {
        char side = color == PieceColor.WHITE ? 'W' : 'B';
        char kind = type == PieceType.KING ? 'K' : 'M';
        return "" + side + kind;
    }

    public static Piece fromCode(String code) {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("Invalid piece code: " + code);
        }
        PieceColor color = code.charAt(0) == 'W' ? PieceColor.WHITE : PieceColor.BLACK;
        PieceType type = code.charAt(1) == 'K' ? PieceType.KING : PieceType.MAN;
        return new Piece(color, type);
    }

    public Piece promote() {
        return new Piece(color, PieceType.KING);
    }
}
