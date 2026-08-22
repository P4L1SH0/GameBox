package com.gamebox.client.games.game1010;

public class Game1010Record {

    private int bestScore;
    private int gamesPlayed;

    // Required by Gson for deserialization.
    public Game1010Record() {
    }

    public Game1010Record(int bestScore, int gamesPlayed) {
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