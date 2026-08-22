package com.gamebox.client.games.snake;

/**
 * Snake-specific record data for a single difficulty level.
 * RecordManager serializes/deserializes this automatically via Gson.
 */
public class SnakeRecord {

    private int highScore;
    private int gamesPlayed;

    // Required by Gson for deserialization.
    public SnakeRecord() {
    }

    public SnakeRecord(int highScore, int gamesPlayed) {
        this.highScore = highScore;
        this.gamesPlayed = gamesPlayed;
    }

    public int getHighScore() {
        return highScore;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }
}