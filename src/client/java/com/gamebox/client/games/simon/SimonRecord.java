package com.gamebox.client.games.simon;

public class SimonRecord {

    private int bestScore;
    private int gamesPlayed;

    // Required by Gson for deserialization.
    public SimonRecord() {
    }

    public SimonRecord(int bestScore, int gamesPlayed) {
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