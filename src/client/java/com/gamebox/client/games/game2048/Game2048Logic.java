package com.gamebox.client.games.game2048;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for 2048. Contains no Minecraft classes or rendering code
 * on purpose, so it can be understood, modified and tested in complete
 * isolation from the game engine.
 *
 * The board is always a fixed 4x4 grid, matching the original 2048 - there's
 * no natural "difficulty" concept for this game, so it doesn't use the
 * shared Difficulty enum (same reasoning as Tic-Tac-Toe).
 *
 * Each move() records the individual tile movements that occurred (see
 * TileMove), so the screen layer can animate tiles sliding from their old
 * position to their new one instead of the board just jumping to its
 * final state.
 */
public class Game2048Logic {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    /**
     * A single tile's movement during one move(): from its old board
     * position to its new one. displayValue is always the tile's own
     * value BEFORE any merge - for a merging pair, both contributing tiles
     * report their shared pre-merge value (never the doubled result),
     * since the doubled tile is what appears once the animation finishes
     * and the real board state (already updated) takes over.
     */
    public record TileMove(int fromRow, int fromCol, int toRow, int toCol, int displayValue, boolean merged) {
    }

    private static final int SIZE = 4;

    private final Random random;
    private int[][] board;
    private int score;
    private boolean lastMoveChangedBoard;
    private final List<TileMove> lastMoves = new ArrayList<>();
    private int lastSpawnRow = -1;
    private int lastSpawnCol = -1;

    public Game2048Logic() {
        this(new Random().nextLong());
    }

    public Game2048Logic(long seed) {
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        this.board = new int[SIZE][SIZE];
        this.score = 0;
        this.lastMoves.clear();
        this.lastSpawnRow = -1;
        this.lastSpawnCol = -1;
        spawnRandomTile();
        spawnRandomTile();
    }

    /**
     * Attempts to slide/merge the board in the given direction.
     *
     * @return true if the board actually changed (i.e. this was a legal
     * move) - the caller should only spawn a new tile and count a move when
     * this returns true, since 2048 doesn't allow "passing" by sliding into
     * a wall with nothing moving.
     */
    public boolean move(Direction direction) {
        int[][] before = copyBoard(board);
        lastMoves.clear();

        for (int lineIndex = 0; lineIndex < SIZE; lineIndex++) {
            List<int[]> coords = getLineCoordinates(direction, lineIndex);
            slideLineWithTracking(coords);
        }

        this.lastMoveChangedBoard = !boardsEqual(before, board);
        if (lastMoveChangedBoard) {
            spawnRandomTile();
        } else {
            lastSpawnRow = -1;
            lastSpawnCol = -1;
        }
        return lastMoveChangedBoard;
    }

    /**
     * @return, for a given direction and line index (row index for
     * LEFT/RIGHT, column index for UP/DOWN), the ordered list of board
     * coordinates along that line starting from the edge tiles compact
     * towards, going outward. This single method replaces having separate
     * hand-written slideLeft/Right/Up/Down methods - every direction is
     * just "the same compaction algorithm over a different coordinate order".
     */
    private List<int[]> getLineCoordinates(Direction direction, int index) {
        List<int[]> coords = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            switch (direction) {
                case LEFT -> coords.add(new int[]{index, i});
                case RIGHT -> coords.add(new int[]{index, SIZE - 1 - i});
                case UP -> coords.add(new int[]{i, index});
                case DOWN -> coords.add(new int[]{SIZE - 1 - i, index});
            }
        }
        return coords;
    }

    /**
     * Compacts and merges the values found along {@code coords} (in order,
     * from the compaction target outward), writing the result back to the
     * board and appending a TileMove for every tile that actually moved.
     */
    private void slideLineWithTracking(List<int[]> coords) {
        int n = coords.size();
        int[] lineValues = new int[n];
        int[][] lineCoords = new int[n][];
        int count = 0;

        for (int[] coord : coords) {
            int value = board[coord[0]][coord[1]];
            if (value != 0) {
                lineValues[count] = value;
                lineCoords[count] = coord;
                count++;
            }
        }

        int[] newLineValues = new int[n];
        int outIndex = 0;
        int i = 0;
        while (i < count) {
            int currentValue = lineValues[i];
            int[] currentCoord = lineCoords[i];
            int[] destCoord = coords.get(outIndex);

            if (i + 1 < count && lineValues[i + 1] == currentValue) {
                int mergedValue = currentValue * 2;
                newLineValues[outIndex] = mergedValue;
                score += mergedValue;

                lastMoves.add(new TileMove(currentCoord[0], currentCoord[1], destCoord[0], destCoord[1], currentValue, true));
                int[] secondCoord = lineCoords[i + 1];
                lastMoves.add(new TileMove(secondCoord[0], secondCoord[1], destCoord[0], destCoord[1], currentValue, true));

                outIndex++;
                i += 2;
            } else {
                newLineValues[outIndex] = currentValue;
                if (destCoord[0] != currentCoord[0] || destCoord[1] != currentCoord[1]) {
                    lastMoves.add(new TileMove(currentCoord[0], currentCoord[1], destCoord[0], destCoord[1], currentValue, false));
                }
                outIndex++;
                i++;
            }
        }

        for (int j = 0; j < n; j++) {
            int[] coord = coords.get(j);
            board[coord[0]][coord[1]] = newLineValues[j];
        }
    }

    private void spawnRandomTile() {
        List<int[]> emptyCells = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) {
                    emptyCells.add(new int[]{row, col});
                }
            }
        }
        if (emptyCells.isEmpty()) {
            lastSpawnRow = -1;
            lastSpawnCol = -1;
            return;
        }
        int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
        // 90% chance of a 2, 10% chance of a 4 - matches the original game.
        board[cell[0]][cell[1]] = random.nextInt(10) == 0 ? 4 : 2;
        lastSpawnRow = cell[0];
        lastSpawnCol = cell[1];
    }

    private int[][] copyBoard(int[][] source) {
        int[][] copy = new int[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }

    private boolean boardsEqual(int[][] a, int[][] b) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (a[row][col] != b[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true if no move in any direction would change the board -
     * i.e. the board is full and no two adjacent equal tiles exist.
     */
    public boolean isGameOver() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) {
                    return false;
                }
                if (col + 1 < SIZE && board[row][col] == board[row][col + 1]) {
                    return false;
                }
                if (row + 1 < SIZE && board[row][col] == board[row + 1][col]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true if any tile has reached 2048. The screen decides what to
     * do with this (e.g. show a "You reached 2048!" banner once, without
     * necessarily ending the game - the player can usually keep playing).
     */
    public boolean hasReached2048() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] >= 2048) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public int getValue(int row, int col) {
        return board[row][col];
    }

    public int getSize() {
        return SIZE;
    }

    public int getScore() {
        return score;
    }

    public boolean didLastMoveChangeBoard() {
        return lastMoveChangedBoard;
    }

    /**
     * @return the individual tile movements from the most recent move(),
     * used by the screen to animate tiles sliding rather than jumping
     * straight to their final position. Empty right after restart() or
     * after a move that didn't change the board.
     */
    public List<TileMove> getLastMoves() {
        return List.copyOf(lastMoves);
    }

    /**
     * @return the row of the tile spawned by the most recent move() (or
     * restart()), or -1 if none was spawned (e.g. the board was full).
     */
    public int getLastSpawnRow() {
        return lastSpawnRow;
    }

    public int getLastSpawnCol() {
        return lastSpawnCol;
    }
}