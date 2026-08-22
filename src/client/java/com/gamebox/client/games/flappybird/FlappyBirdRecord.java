package com.gamebox.client.games.flappybird;

public class FlappyBirdRecord {

    private int bestScore;
    private int gamesPlayed;

    // Required by Gson for deserialization.
    public FlappyBirdRecord() {
    }

    public FlappyBirdRecord(int bestScore, int gamesPlayed) {
        this.bestScore = bestScore;
        this.gamesPlayed = gamesPlayed;
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }
}