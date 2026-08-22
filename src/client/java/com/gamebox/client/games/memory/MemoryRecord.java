package com.gamebox.client.games.memory;

/**
 * Memory-specific record data for a single difficulty level.
 */
public class MemoryRecord {

    private int bestMoves;
    private long bestTimeMs;
    private int gamesCompleted;

    // Required by Gson for deserialization.
    public MemoryRecord() {
    }

    public MemoryRecord(int bestMoves, long bestTimeMs, int gamesCompleted) {
        this.bestMoves = bestMoves;
        this.bestTimeMs = bestTimeMs;
        this.gamesCompleted = gamesCompleted;
    }

    public int getBestMoves() {
        return bestMoves;
    }

    public long getBestTimeMs() {
        return bestTimeMs;
    }

    public int getGamesCompleted() {
        return gamesCompleted;
    }
}