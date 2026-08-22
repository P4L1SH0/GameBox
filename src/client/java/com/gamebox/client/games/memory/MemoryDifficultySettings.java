package com.gamebox.client.games.memory;

import com.gamebox.client.core.Difficulty;

/**
 * Translates the generic Difficulty enum into concrete Memory parameters.
 * pairCount determines the board size: the board always has exactly
 * 2 * pairCount cards, arranged as close to a square grid as possible by
 * the screen layer (this class only cares about the count, not the shape).
 */
public record MemoryDifficultySettings(int pairCount) {

    public static MemoryDifficultySettings forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new MemoryDifficultySettings(6);    // 12 cards, 3x4
            case NORMAL -> new MemoryDifficultySettings(8);  // 16 cards, 4x4
            case HARD -> new MemoryDifficultySettings(10);   // 20 cards, 4x5
            case EXPERT -> new MemoryDifficultySettings(15); // 30 cards, 5x6
        };
    }
}