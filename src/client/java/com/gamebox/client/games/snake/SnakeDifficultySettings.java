package com.gamebox.client.games.snake;

import com.gamebox.client.core.Difficulty;

/**
 * Translates the generic Difficulty enum into concrete Snake parameters.
 * This is the single place where "difficulty" is interpreted for Snake -
 * no if/else chains scattered through the rest of the game logic.
 */
public record SnakeDifficultySettings(int boardWidth, int boardHeight, int tickIntervalMs, int obstacleCount) {

    public static SnakeDifficultySettings forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new SnakeDifficultySettings(20, 20, 200, 0);
            case NORMAL -> new SnakeDifficultySettings(16, 16, 150, 0);
            case HARD -> new SnakeDifficultySettings(14, 14, 110, 6);
            case EXPERT -> new SnakeDifficultySettings(14, 14, 100, 10);
        };
    }
}