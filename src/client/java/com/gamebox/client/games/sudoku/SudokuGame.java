package com.gamebox.client.games.sudoku;

import com.gamebox.client.core.Difficulty;
import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.DifficultySelectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SudokuGame implements MiniGame {

    public static final String ID = "sudoku";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.sudoku.name"),
            Component.translatable("gamebox.games.sudoku.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new DifficultySelectScreen(parent, METADATA.displayName(), getInstructions(), ID,
                (difficultyScreen, difficulty) -> new SudokuScreen(difficultyScreen, difficulty));
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.sudoku.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        List<Component> lines = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            SudokuRecord record = RecordManager.getRecord(
                    ID, difficulty.name(), SudokuRecord.class, new SudokuRecord(0, 0));

            if (record.getGamesCompleted() == 0) {
                lines.add(Component.translatable("gamebox.games.sudoku.record_line_empty", difficulty.getDisplayName()));
            } else {
                lines.add(Component.translatable("gamebox.games.sudoku.record_line",
                        difficulty.getDisplayName(), formatTime(record.getBestTimeMs()), record.getGamesCompleted()));
            }
        }
        return lines;
    }

    @Override
    public int getTotalGamesPlayed() {
        int total = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            SudokuRecord record = RecordManager.getRecord(
                    ID, difficulty.name(), SudokuRecord.class, new SudokuRecord(0, 0));
            total += record.getGamesCompleted();
        }
        return total;
    }

    private String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}