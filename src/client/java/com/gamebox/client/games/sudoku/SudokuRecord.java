package com.gamebox.client.games.sudoku;

/**
 * Sudoku-specific record data for a single difficulty level.
 */
public class SudokuRecord {

    private long bestTimeMs;
    private int gamesCompleted;

    // Required by Gson for deserialization.
    public SudokuRecord() {
    }

    public SudokuRecord(long bestTimeMs, int gamesCompleted) {
        this.bestTimeMs = bestTimeMs;
        this.gamesCompleted = gamesCompleted;
    }

    public long getBestTimeMs() {
        return bestTimeMs;
    }

    public int getGamesCompleted() {
        return gamesCompleted;
    }
}