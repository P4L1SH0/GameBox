package com.gamebox.client.games.game1010;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for 10x10 (a.k.a. Block Puzzle / 1010!). Contains no
 * Minecraft classes or rendering code on purpose, so it can be understood,
 * modified and tested in complete isolation from the game engine.
 *
 * The board stores each occupied cell's ARGB color directly (0 = empty),
 * rather than a plain boolean, so the screen layer can render each placed
 * piece in the color it was placed with without needing separate state.
 */
public class Game1010Logic {

    public static final int BOARD_SIZE = 10;
    public static final int PIECE_SLOTS = 3;

    /**
     * A placeable shape: a set of (row, col) offsets from its own top-left
     * corner, plus the color it's rendered in once placed on the board.
     */
    public record Piece(int[][] cells, int color) {
        public int width() {
            int max = 0;
            for (int[] cell : cells) {
                max = Math.max(max, cell[1]);
            }
            return max + 1;
        }

        public int height() {
            int max = 0;
            for (int[] cell : cells) {
                max = Math.max(max, cell[0]);
            }
            return max + 1;
        }
    }

    // No rotation system - each orientation that should be available is
    // listed here as its own shape instead.
    private static final int[][][] SHAPES = {
            {{0, 0}},                                                  // single
            {{0, 0}, {0, 1}},                                          // domino H
            {{0, 0}, {1, 0}},                                          // domino V
            {{0, 0}, {0, 1}, {0, 2}},                                  // tromino I H
            {{0, 0}, {1, 0}, {2, 0}},                                  // tromino I V
            {{0, 0}, {0, 1}, {1, 0}, {1, 1}},                          // square O
            {{0, 0}, {1, 0}, {1, 1}},                                  // L tromino a
            {{0, 0}, {0, 1}, {1, 0}},                                  // L tromino b
            {{0, 0}, {0, 1}, {1, 1}},                                  // L tromino c
            {{0, 1}, {1, 0}, {1, 1}},                                  // L tromino d
            {{0, 0}, {0, 1}, {0, 2}, {0, 3}},                          // tetromino I H
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}},                          // tetromino I V
            {{0, 0}, {1, 0}, {2, 0}, {2, 1}},                          // tetromino L a
            {{0, 0}, {0, 1}, {0, 2}, {1, 0}},                          // tetromino L b
            {{0, 0}, {0, 1}, {1, 1}, {2, 1}},                          // tetromino L c
            {{1, 0}, {1, 1}, {1, 2}, {0, 2}},                          // tetromino L d
            {{0, 0}, {0, 1}, {0, 2}, {1, 1}},                          // tetromino T a
            {{0, 1}, {1, 0}, {1, 1}, {2, 1}},                          // tetromino T b
            {{0, 1}, {0, 2}, {1, 0}, {1, 1}},                          // tetromino S
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}},                          // tetromino Z
            {{0, 0}, {0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}}, // big square 3x3
            {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}},                  // big corner a
            {{0, 0}, {0, 1}, {0, 2}, {1, 0}, {2, 0}}                   // big corner b
    };

    private static final int[] COLOR_PALETTE = {
            0xFF00BCD4, // cyan
            0xFFFFC107, // amber
            0xFF9C27B0, // purple
            0xFF4CAF50, // green
            0xFFF44336, // red
            0xFF3F51B5, // indigo
            0xFFFF9800  // orange
    };

    private final Random random;

    private int[][] board;
    private Piece[] currentPieces;
    private int score;
    private boolean gameOver;

    public Game1010Logic() {
        this(new Random().nextLong());
    }

    public Game1010Logic(long seed) {
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        this.board = new int[BOARD_SIZE][BOARD_SIZE];
        this.score = 0;
        this.gameOver = false;
        generateNewPieces();
        updateGameOverStatus();
    }

    private void generateNewPieces() {
        this.currentPieces = new Piece[PIECE_SLOTS];
        for (int i = 0; i < PIECE_SLOTS; i++) {
            currentPieces[i] = randomPiece();
        }
    }

    private Piece randomPiece() {
        int index = random.nextInt(SHAPES.length);
        int color = COLOR_PALETTE[index % COLOR_PALETTE.length];
        return new Piece(SHAPES[index], color);
    }

    /**
     * @return true if the piece in the given slot (0..PIECE_SLOTS-1) can be
     * placed with its top-left cell at (row, col) without overlapping the
     * board edges or already-occupied cells. False if the slot is empty
     * (piece already used) or the game is over.
     */
    public boolean canPlace(int slotIndex, int row, int col) {
        if (gameOver) {
            return false;
        }
        Piece piece = currentPieces[slotIndex];
        return piece != null && canPlace(piece, row, col);
    }

    private boolean canPlace(Piece piece, int row, int col) {
        for (int[] cell : piece.cells()) {
            int r = row + cell[0];
            int c = col + cell[1];
            if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                return false;
            }
            if (board[r][c] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Places the piece from the given slot with its top-left cell at
     * (row, col). Awards 1 point per cell placed, plus a bonus for any
     * rows/columns that get completed and cleared as a result. If this
     * was the last of the 3 slots to be used, a fresh set of 3 pieces is
     * generated immediately.
     *
     * @return true if the placement was legal and applied.
     */
    public boolean placePiece(int slotIndex, int row, int col) {
        if (gameOver) {
            return false;
        }
        Piece piece = currentPieces[slotIndex];
        if (piece == null || !canPlace(piece, row, col)) {
            return false;
        }

        for (int[] cell : piece.cells()) {
            board[row + cell[0]][col + cell[1]] = piece.color();
        }
        score += piece.cells().length;
        currentPieces[slotIndex] = null;

        int linesCleared = clearFullLines();
        if (linesCleared > 0) {
            score += linesCleared * linesCleared * 10;
        }

        if (allSlotsEmpty()) {
            generateNewPieces();
        }
        updateGameOverStatus();
        return true;
    }

    private boolean allSlotsEmpty() {
        for (Piece piece : currentPieces) {
            if (piece != null) {
                return false;
            }
        }
        return true;
    }

    private int clearFullLines() {
        List<Integer> fullRows = new ArrayList<>();
        List<Integer> fullCols = new ArrayList<>();

        for (int r = 0; r < BOARD_SIZE; r++) {
            boolean full = true;
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                fullRows.add(r);
            }
        }

        for (int c = 0; c < BOARD_SIZE; c++) {
            boolean full = true;
            for (int r = 0; r < BOARD_SIZE; r++) {
                if (board[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                fullCols.add(c);
            }
        }

        for (int r : fullRows) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                board[r][c] = 0;
            }
        }
        for (int c : fullCols) {
            for (int r = 0; r < BOARD_SIZE; r++) {
                board[r][c] = 0;
            }
        }

        return fullRows.size() + fullCols.size();
    }

    private void updateGameOverStatus() {
        for (Piece piece : currentPieces) {
            if (piece == null) {
                continue;
            }
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    if (canPlace(piece, r, c)) {
                        this.gameOver = false;
                        return;
                    }
                }
            }
        }
        this.gameOver = true;
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public boolean isCellFilled(int row, int col) {
        return board[row][col] != 0;
    }

    public int getCellColor(int row, int col) {
        return board[row][col];
    }

    public Piece getPiece(int slotIndex) {
        return currentPieces[slotIndex];
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getBoardSize() {
        return BOARD_SIZE;
    }
}