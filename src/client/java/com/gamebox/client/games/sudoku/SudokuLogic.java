package com.gamebox.client.games.sudoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Sudoku. Contains no Minecraft classes or rendering
 * code on purpose, so it can be understood, modified and tested in complete
 * isolation from the game engine.
 *
 * Note: generated puzzles are not guaranteed to have a unique solution (see
 * project notes). Because of that, victory is determined by "the board is
 * completely filled and has no conflicts", not by matching the specific
 * solution this class happened to generate - any valid completion counts.
 */
public class SudokuLogic {

    private static final int SIZE = 9;
    private static final int BOX_SIZE = 3;

    private final SudokuDifficultySettings settings;
    private final Random random;

    private int[][] solution;
    private boolean[][] given;
    private int[][] current;
    private boolean[][][] notes; // [row][col][digit - 1]
    private int moveCount;

    public SudokuLogic(SudokuDifficultySettings settings) {
        this(settings, new Random().nextLong());
    }

    public SudokuLogic(SudokuDifficultySettings settings, long seed) {
        this.settings = settings;
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        this.solution = new int[SIZE][SIZE];
        fillSolution(0);

        this.given = new boolean[SIZE][SIZE];
        this.current = new int[SIZE][SIZE];
        this.notes = new boolean[SIZE][SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                current[row][col] = solution[row][col];
                given[row][col] = true;
            }
        }

        removeCells(settings.cellsToRemove());
        this.moveCount = 0;
    }

    private boolean fillSolution(int index) {
        if (index == SIZE * SIZE) {
            return true;
        }
        int row = index / SIZE;
        int col = index % SIZE;

        List<Integer> candidates = new ArrayList<>();
        for (int v = 1; v <= SIZE; v++) {
            candidates.add(v);
        }
        Collections.shuffle(candidates, random);

        for (int value : candidates) {
            if (canPlace(solution, row, col, value)) {
                solution[row][col] = value;
                if (fillSolution(index + 1)) {
                    return true;
                }
                solution[row][col] = 0;
            }
        }
        return false;
    }

    private void removeCells(int count) {
        List<int[]> allCells = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                allCells.add(new int[]{row, col});
            }
        }
        Collections.shuffle(allCells, random);

        int toRemove = Math.min(count, allCells.size());
        for (int i = 0; i < toRemove; i++) {
            int[] cell = allCells.get(i);
            current[cell[0]][cell[1]] = 0;
            given[cell[0]][cell[1]] = false;
        }
    }

    private boolean canPlace(int[][] grid, int row, int col, int value) {
        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == value || grid[i][col] == value) {
                return false;
            }
        }
        int boxRow = (row / BOX_SIZE) * BOX_SIZE;
        int boxCol = (col / BOX_SIZE) * BOX_SIZE;
        for (int r = boxRow; r < boxRow + BOX_SIZE; r++) {
            for (int c = boxCol; c < boxCol + BOX_SIZE; c++) {
                if (grid[r][c] == value) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Places {@code value} (1-9) at (row, col), or clears it if value is 0.
     * Does nothing if the cell is a given (pre-filled) cell. Placing a real
     * value (not 0) clears any notes on that cell, since they're no longer
     * needed once the cell is actually filled in.
     */
    public void setValue(int row, int col, int value) {
        if (given[row][col]) {
            return;
        }
        current[row][col] = value;
        moveCount++;
        if (value != 0) {
            clearNotes(row, col);
        }
    }

    public void clearValue(int row, int col) {
        setValue(row, col, 0);
    }

    /**
     * Toggles a pencil-mark candidate (1-9) on an empty, non-given cell.
     * Does nothing on given cells or cells that already have a real value.
     */
    public void toggleNote(int row, int col, int digit) {
        if (given[row][col] || current[row][col] != 0) {
            return;
        }
        notes[row][col][digit - 1] = !notes[row][col][digit - 1];
    }

    public boolean hasNote(int row, int col, int digit) {
        return notes[row][col][digit - 1];
    }

    public boolean hasAnyNote(int row, int col) {
        for (boolean note : notes[row][col]) {
            if (note) {
                return true;
            }
        }
        return false;
    }

    public void clearNotes(int row, int col) {
        for (int i = 0; i < SIZE; i++) {
            notes[row][col][i] = false;
        }
    }

    /**
     * @return true if the value at (row, col) conflicts with another cell
     * in the same row, column, or 3x3 box. Empty cells never conflict.
     */
    public boolean hasConflict(int row, int col) {
        int value = current[row][col];
        if (value == 0) {
            return false;
        }
        for (int i = 0; i < SIZE; i++) {
            if (i != col && current[row][i] == value) {
                return true;
            }
            if (i != row && current[i][col] == value) {
                return true;
            }
        }
        int boxRow = (row / BOX_SIZE) * BOX_SIZE;
        int boxCol = (col / BOX_SIZE) * BOX_SIZE;
        for (int r = boxRow; r < boxRow + BOX_SIZE; r++) {
            for (int c = boxCol; c < boxCol + BOX_SIZE; c++) {
                if ((r != row || c != col) && current[r][c] == value) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return true if every cell is filled and no cell has a conflict.
     * This accepts any valid completion, not just the originally generated
     * solution (see class-level note on solution uniqueness).
     */
    public boolean isSolved() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (current[row][col] == 0 || hasConflict(row, col)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true if (row, col) is in the same row, column, or 3x3 box as
     * (otherRow, otherCol) - used by the screen to highlight peers of the
     * currently selected cell.
     */
    public boolean isPeer(int row, int col, int otherRow, int otherCol) {
        if (row == otherRow && col == otherCol) {
            return false;
        }
        if (row == otherRow || col == otherCol) {
            return true;
        }
        return (row / BOX_SIZE) == (otherRow / BOX_SIZE) && (col / BOX_SIZE) == (otherCol / BOX_SIZE);
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public int getValue(int row, int col) {
        return current[row][col];
    }

    public boolean isGiven(int row, int col) {
        return given[row][col];
    }

    public int getSolutionValue(int row, int col) {
        return solution[row][col];
    }

    public int getMoveCount() {
        return moveCount;
    }

    public int getSize() {
        return SIZE;
    }

    public SudokuDifficultySettings getSettings() {
        return settings;
    }
}