package com.gamebox.client.games.sudoku;

import com.gamebox.client.core.Difficulty;

/**
 * Translates the generic Difficulty enum into concrete Sudoku parameters.
 * The board is always 9x9 (standard Sudoku), so difficulty only affects how
 * many of the 81 cells start empty.
 */
public record SudokuDifficultySettings(int cellsToRemove) {

    public static SudokuDifficultySettings forDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new SudokuDifficultySettings(35);   // ~46 starting clues
            case NORMAL -> new SudokuDifficultySettings(45); // ~36 starting clues
            case HARD -> new SudokuDifficultySettings(52);   // ~29 starting clues
            case EXPERT -> new SudokuDifficultySettings(58); // ~23 starting clues
        };
    }
}