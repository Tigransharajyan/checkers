package com.checkers.engine;

/**
 * Algebraic square on an 8×8 board (a1–h8). Dark squares only are playable.
 */
public record Square(int file, int rank) {

    public static final int SIZE = 8;

    public Square {
        if (file < 0 || file >= SIZE || rank < 0 || rank >= SIZE) {
            throw new IllegalArgumentException("Square out of board: " + file + "," + rank);
        }
    }

    public static Square of(int file, int rank) {
        return new Square(file, rank);
    }

    public static Square parse(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid square: " + algebraic);
        }
        int file = Character.toLowerCase(algebraic.charAt(0)) - 'a';
        int rank = algebraic.charAt(1) - '1';
        if (file < 0 || file >= SIZE || rank < 0 || rank >= SIZE) {
            throw new IllegalArgumentException("Invalid square: " + algebraic);
        }
        return new Square(file, rank);
    }

    public static boolean isValidAlgebraic(String algebraic) {
        try {
            parse(algebraic);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** Dark squares: a1 is dark (file+rank even). */
    public boolean isDark() {
        return (file + rank) % 2 == 0;
    }

    public boolean onBoard() {
        return file >= 0 && file < SIZE && rank >= 0 && rank < SIZE;
    }

    public Square step(int df, int dr) {
        int nf = file + df;
        int nr = rank + dr;
        if (nf < 0 || nf >= SIZE || nr < 0 || nr >= SIZE) {
            return null;
        }
        return new Square(nf, nr);
    }

    public String algebraic() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }

    @Override
    public String toString() {
        return algebraic();
    }
}
