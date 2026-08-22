package com.gamebox.client.games.mastermind;

public class MastermindRecord {

    private int bestAttempts;
    private int gamesWon;

    // Required by Gson for deserialization.
    public MastermindRecord() {
    }

    public MastermindRecord(int bestAttempts, int gamesWon) {
        this.bestAttempts = bestAttempts;
        this.gamesWon = gamesWon;
    }

    public int getBestAttempts() {
        return bestAttempts;
    }

    public int getGamesWon() {
        return gamesWon;
    }
}