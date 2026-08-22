package com.gamebox.client.games.mastermind;

import com.gamebox.client.core.Difficulty;

/**
 * Translates the generic Difficulty enum into concrete Mastermind
 * parameters. Difficulty affects the number of guesses allowed, whether
 * the secret code may contain repeated colors, how many colors are
 * available to guess from, and how many pegs long the secret code is.
 *
 * colorCount is always codeLength + 1: the palette always has exactly one
 * more color available than the code length, so there's always at least
 * one "decoy" color you can rule out as you deduce the secret.
 */
public record MastermindDifficultySettings(int maxAttempts, boolean allowRepeats, int colorCount, int codeLength) {

    public static final int MAX_COLOR_COUNT = 8;
    public static final int MAX_CODE_LENGTH = 7;

    public static MastermindDifficultySettings forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new MastermindDifficultySettings(12, false, 5, 4);
            case NORMAL -> new MastermindDifficultySettings(10, false, 6, 5);
            case HARD -> new MastermindDifficultySettings(10, true, 7, 6);
            case EXPERT -> new MastermindDifficultySettings(8, true, 8, 7);
        };
    }
}