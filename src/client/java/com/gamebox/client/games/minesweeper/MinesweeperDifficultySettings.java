package com.gamebox.client.games.minesweeper;

import com.gamebox.client.core.Difficulty;

/**
 * Translates the generic Difficulty enum into concrete Minesweeper parameters.
 */
public record MinesweeperDifficultySettings(int boardWidth, int boardHeight, int mineCount) {

    public static MinesweeperDifficultySettings forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new MinesweeperDifficultySettings(8, 8, 8);
            case NORMAL -> new MinesweeperDifficultySettings(10, 10, 15);
            case HARD -> new MinesweeperDifficultySettings(12, 12, 28);
            case EXPERT -> new MinesweeperDifficultySettings(14, 14, 40);
        };
    }
}