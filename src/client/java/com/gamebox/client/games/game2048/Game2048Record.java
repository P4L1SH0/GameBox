package com.gamebox.client.games.game2048;

/**
 * 2048-specific record data. No Difficulty split, same reasoning as
 * Tic-Tac-Toe: 2048 always uses a fixed 4x4 board.
 */
public class Game2048Record {

    private int bestScore;
    private int gamesPlayed;

    // Required by Gson for deserialization.
    public Game2048Record() {
    }

    public Game2048Record(int bestScore, int gamesPlayed) {
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