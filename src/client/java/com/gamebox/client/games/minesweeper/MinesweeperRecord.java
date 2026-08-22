package com.gamebox.client.games.minesweeper;

/**
 * Minesweeper-specific record data for a single difficulty level.
 */
public class MinesweeperRecord {

    private long bestTimeMs;
    private int gamesWon;

    // Required by Gson for deserialization.
    public MinesweeperRecord() {
    }

    public MinesweeperRecord(long bestTimeMs, int gamesWon) {
        this.bestTimeMs = bestTimeMs;
        this.gamesWon = gamesWon;
    }

    public long getBestTimeMs() {
        return bestTimeMs;
    }

    public int getGamesWon() {
        return gamesWon;
    }
}