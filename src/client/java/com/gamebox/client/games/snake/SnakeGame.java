package com.gamebox.client.games.snake;

import com.gamebox.client.core.Difficulty;
import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.DifficultySelectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SnakeGame implements MiniGame {

    public static final String ID = "snake";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.snake.name"),
            Component.translatable("gamebox.games.snake.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new DifficultySelectScreen(parent, METADATA.displayName(), getInstructions(), ID,
                (difficultyScreen, difficulty) -> new SnakeScreen(difficultyScreen, difficulty));
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.snake.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        List<Component> lines = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            SnakeRecord record = RecordManager.getRecord(ID, difficulty.name(), SnakeRecord.class, new SnakeRecord(0, 0));
            if (record.getGamesPlayed() == 0) {
                lines.add(Component.translatable("gamebox.games.snake.record_line_empty", difficulty.getDisplayName()));
            } else {
                lines.add(Component.translatable("gamebox.games.snake.record_line",
                        difficulty.getDisplayName(), record.getHighScore(), record.getGamesPlayed()));
            }
        }
        return lines;
    }

    @Override
    public int getTotalGamesPlayed() {
        int total = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            SnakeRecord record = RecordManager.getRecord(ID, difficulty.name(), SnakeRecord.class, new SnakeRecord(0, 0));
            total += record.getGamesPlayed();
        }
        return total;
    }
}