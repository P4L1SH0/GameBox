package com.gamebox.client.games.minesweeper;

import com.gamebox.client.core.Difficulty;
import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.DifficultySelectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MinesweeperGame implements MiniGame {

    public static final String ID = "minesweeper";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.minesweeper.name"),
            Component.translatable("gamebox.games.minesweeper.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new DifficultySelectScreen(parent, METADATA.displayName(), getInstructions(), ID,
                (difficultyScreen, difficulty) -> new MinesweeperScreen(difficultyScreen, difficulty));
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.minesweeper.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        List<Component> lines = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            MinesweeperRecord record = RecordManager.getRecord(
                    ID, difficulty.name(), MinesweeperRecord.class, new MinesweeperRecord(0, 0));

            if (record.getGamesWon() == 0) {
                lines.add(Component.translatable("gamebox.games.minesweeper.record_line_empty", difficulty.getDisplayName()));
            } else {
                lines.add(Component.translatable("gamebox.games.minesweeper.record_line",
                        difficulty.getDisplayName(), formatTime(record.getBestTimeMs()), record.getGamesWon()));
            }
        }
        return lines;
    }

    @Override
    public int getTotalGamesPlayed() {
        int total = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            MinesweeperRecord record = RecordManager.getRecord(
                    ID, difficulty.name(), MinesweeperRecord.class, new MinesweeperRecord(0, 0));
            total += record.getGamesWon();
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