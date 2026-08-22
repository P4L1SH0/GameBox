package com.gamebox.client.games.lightsout;

import com.gamebox.client.core.Difficulty;

/**
 * Translates the generic Difficulty enum into concrete Lights Out parameters.
 * Since scrambling now uses distinct cells (no more silent cancellations),
 * these numbers reflect the real difficulty much more closely than before.
 */
public record LightsOutDifficultySettings(int boardSize, int scrambleMoves) {

    public static LightsOutDifficultySettings forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new LightsOutDifficultySettings(3, 3);
            case NORMAL -> new LightsOutDifficultySettings(4, 7);
            case HARD -> new LightsOutDifficultySettings(5, 13);
            case EXPERT -> new LightsOutDifficultySettings(6, 20);
        };
    }
}