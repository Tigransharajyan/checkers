package com.checkers.engine;

import com.checkers.model.enums.PieceColor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable 8×8 Russian-checkers board. Only dark squares hold pieces.
 */
public final class Board {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Piece[][] cells = new Piece[Square.SIZE][Square.SIZE];
    private PieceColor turn;
    private GameOutcome outcome;

    public Board(PieceColor turn) {
        this.turn = Objects.requireNonNull(turn);
        this.outcome = GameOutcome.IN_PROGRESS;
    }

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

    public Board copy() {
        Board copy = new Board(turn);
        copy.outcome = outcome;
        for (int r = 0; r < Square.SIZE; r++) {
            System.arraycopy(cells[r], 0, copy.cells[r], 0, Square.SIZE);
        }
        return copy;
    }

    public Piece get(Square square) {
        return cells[square.rank()][square.file()];
    }

    public void set(Square square, Piece piece) {
        cells[square.rank()][square.file()] = piece;
    }

    public void clear(Square square) {
        cells[square.rank()][square.file()] = null;
    }

    public PieceColor turn() {
        return turn;
    }

    public void setTurn(PieceColor turn) {
        this.turn = turn;
    }

    public GameOutcome outcome() {
        return outcome;
    }

    public void setOutcome(GameOutcome outcome) {
        this.outcome = outcome;
    }

    public PieceColor opponent() {
        return turn == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
    }

    public void switchTurn() {
        turn = opponent();
    }

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

    @Override
    public String toString() {
        return toJson();
    }
}
