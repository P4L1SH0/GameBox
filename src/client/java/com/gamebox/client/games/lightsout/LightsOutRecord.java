package com.gamebox.client.games.lightsout;

/**
 * Lights Out-specific record data for a single difficulty level.
 * bestMoves and bestTimeMs are tracked independently - they don't
 * necessarily come from the same completed game.
 */
public class LightsOutRecord {

    private int bestMoves;
    private long bestTimeMs;
    private int gamesCompleted;

    // Required by Gson for deserialization.
    public LightsOutRecord() {
    }

    public LightsOutRecord(int bestMoves, long bestTimeMs, int gamesCompleted) {
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