package com.gamebox.client.games.simon;

import com.gamebox.client.core.Difficulty;

/**
 * Translates the generic Difficulty enum into concrete Simon parameters.
 * Only the playback speed of the sequence changes with difficulty - the
 * board itself is always the same 4 colors.
 */
public record SimonDifficultySettings(int flashDurationMs, int pauseBetweenMs) {

    public static SimonDifficultySettings forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new SimonDifficultySettings(700, 400);
            case NORMAL -> new SimonDifficultySettings(500, 300);
            case HARD -> new SimonDifficultySettings(350, 200);
            case EXPERT -> new SimonDifficultySettings(250, 150);
        };
    }
}