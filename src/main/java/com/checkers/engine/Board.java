package com.checkers.engine;

import com.checkers.model.enums.PieceColor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable internal eight-by-eight position containing cells, turn and outcome. The engine decides
 * move legality; independent board copies support safe search.
 */
public final class Board {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Piece[][] cells = new Piece[Square.SIZE][Square.SIZE];
    private PieceColor turn;
    private GameOutcome outcome;

    /**
     * Creates an empty board with the supplied side to move and an IN_PROGRESS outcome. The initial
     * method performs the standard piece setup separately.
     */
    public Board(PieceColor turn) {
        this.turn = Objects.requireNonNull(turn);
        this.outcome = GameOutcome.IN_PROGRESS;
    }

    /**
     * Places twelve men per side on the dark squares of each side's first three ranks. White moves
     * first.
     */
    public static Board initial() {
        Board board = new Board(PieceColor.WHITE);
        for (int rank = 0; rank < 3; rank++) {
            for (int file = 0; file < Square.SIZE; file++) {
                Square sq = Square.of(file, rank);
                if (sq.isDark()) {
                    board.set(sq, new Piece(PieceColor.WHITE, PieceType.MAN));
                }
            }
        }
        for (int rank = 5; rank < 8; rank++) {
            for (int file = 0; file < Square.SIZE; file++) {
                Square sq = Square.of(file, rank);
                if (sq.isDark()) {
                    board.set(sq, new Piece(PieceColor.BLACK, PieceType.MAN));
                }
            }
        }
        return board;
    }

    /**
     * Copies cells, side to move and outcome into an independent board for search. Immutable Piece
     * objects can safely be shared between copies.
     */
    public Board copy() {
        Board copy = new Board(turn);
        copy.outcome = outcome;
        for (int r = 0; r < Square.SIZE; r++) {
            System.arraycopy(cells[r], 0, copy.cells[r], 0, Square.SIZE);
        }
        return copy;
    }

    /**
     * Returns the piece on the requested square, or null when that cell is empty.
     */
    public Piece get(Square square) {
        return cells[square.rank()][square.file()];
    }

    /**
     * Places the supplied piece on the mutable board. This low-level operation does not validate a
     * game move; the rules engine does.
     */
    public void set(Square square, Piece piece) {
        cells[square.rank()][square.file()] = piece;
    }

    /**
     * Empties a cell, for example when moving a piece or removing a captured opponent.
     */
    public void clear(Square square) {
        cells[square.rank()][square.file()] = null;
    }

    /**
     * Returns the color whose turn it is to move.
     */
    public PieceColor turn() {
        return turn;
    }

    /**
     * Sets the side to move, including on temporary search boards used to inspect another side's
     * mobility.
     */
    public void setTurn(PieceColor turn) {
        this.turn = turn;
    }

    /**
     * Returns the current position outcome: ongoing play, either side's victory or a draw.
     */
    public GameOutcome outcome() {
        return outcome;
    }

    /**
     * Stores an already calculated outcome without evaluating rules itself.
     */
    public void setOutcome(GameOutcome outcome) {
        this.outcome = outcome;
    }

    /**
     * Returns the color opposite to the current side to move.
     */
    public PieceColor opponent() {
        return turn == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
    }

    /**
     * Passes the turn to the opponent after one complete path, including every jump in a capture
     * chain.
     */
    public void switchTurn() {
        turn = opponent();
    }

    /**
     * Counts all pieces of the supplied color, including both men and kings.
     */
    public int countPieces(PieceColor color) {
        int count = 0;
        for (int r = 0; r < Square.SIZE; r++) {
            for (int f = 0; f < Square.SIZE; f++) {
                Piece p = cells[r][f];
                if (p != null && p.color() == color) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns occupied squares as a coordinate-to-code map, for example a1 to WM. Rank-by-rank
     * traversal provides stable serialization order.
     */
    public Map<String, String> occupiedAlgebraic() {
        Map<String, String> map = new LinkedHashMap<>();
        for (int r = 0; r < Square.SIZE; r++) {
            for (int f = 0; f < Square.SIZE; f++) {
                Piece p = cells[r][f];
                if (p != null) {
                    map.put(Square.of(f, r).algebraic(), p.code());
                }
            }
        }
        return map;
    }

    /**
     * Serializes the side to move, outcome and occupied squares for persistence and client
     * snapshots.
     */
    public String toJson() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("turn", turn.name());
        payload.put("outcome", outcome.name());
        payload.put("squares", occupiedAlgebraic());
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize board", e);
        }
    }

    /**
     * Restores a saved JSON position by converting square names and piece codes to internal
     * objects. Rejects malformed state with an exception.
     */
    @SuppressWarnings("unchecked")
    public static Board fromJson(String json) {
        try {
            Map<String, Object> payload = MAPPER.readValue(json, Map.class);
            PieceColor turn = PieceColor.valueOf((String) payload.get("turn"));
            Board board = new Board(turn);
            Object outcomeRaw = payload.get("outcome");
            if (outcomeRaw != null) {
                board.outcome = GameOutcome.valueOf((String) outcomeRaw);
            }
            Map<String, String> squares = (Map<String, String>) payload.getOrDefault("squares", Map.of());
            for (Map.Entry<String, String> e : squares.entrySet()) {
                board.set(Square.parse(e.getKey()), Piece.fromCode(e.getValue()));
            }
            return board;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid board_state JSON", e);
        }
    }

    /**
     * Returns the same JSON position as toJson for diagnostics and search keys.
     */
    @Override
    public String toString() {
        return toJson();
    }
}
