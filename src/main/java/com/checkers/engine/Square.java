package com.checkers.engine;

/**
 * Immutable board coordinate with indices zero through seven. Contains geometry and algebraic
 * conversion, not information about the occupying piece.
 */
public record Square(int file, int rank) {

    public static final int SIZE = 8;

    /**
     * Checks that file and rank are within zero through seven. Rejects off-board coordinates with
     * IllegalArgumentException.
     */
    public Square {
        if (file < 0 || file >= SIZE || rank < 0 || rank >= SIZE) {
            throw new IllegalArgumentException("Square out of board: " + file + "," + rank);
        }
    }

    /**
     * Creates a validated square from internal file and rank indices.
     */
    public static Square of(int file, int rank) {
        return new Square(file, rank);
    }

    /**
     * Converts algebraic notation such as c3 to file and rank indices. Rejects an invalid length,
     * file letter or rank number.
     */
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

    /**
     * Tests whether a string can be parsed as a square without propagating parsing exceptions.
     */
    public static boolean isValidAlgebraic(String algebraic) {
        try {
            parse(algebraic);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * Determines square color from coordinate parity. Treats a1 as dark and therefore playable.
     */
    public boolean isDark() {
        return (file + rank) % 2 == 0;
    }

    /**
     * Reports whether the coordinates are within the eight-by-eight board. Normal Square
     * construction already guarantees these bounds.
     */
    public boolean onBoard() {
        return file >= 0 && file < SIZE && rank >= 0 && rank < SIZE;
    }

    /**
     * Offsets the square by the supplied file and rank changes. Returns null when the destination
     * is off-board instead of constructing an invalid square.
     */
    public Square step(int df, int dr) {
        int nf = file + df;
        int nr = rank + dr;
        if (nf < 0 || nf >= SIZE || nr < 0 || nr >= SIZE) {
            return null;
        }
        return new Square(nf, nr);
    }

    /**
     * Converts internal indices to algebraic notation: file zero becomes a and rank zero becomes
     * one.
     */
    public String algebraic() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }

    /**
     * Returns the algebraic coordinate for logging and debugging.
     */
    @Override
    public String toString() {
        return algebraic();
    }
}
