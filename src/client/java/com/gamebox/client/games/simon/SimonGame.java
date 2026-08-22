package com.gamebox.client.games.simon;

import com.gamebox.client.core.Difficulty;
import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.DifficultySelectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SimonGame implements MiniGame {

    public static final String ID = "simon";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.simon.name"),
            Component.translatable("gamebox.games.simon.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new DifficultySelectScreen(parent, METADATA.displayName(), getInstructions(), ID,
                (difficultyScreen, difficulty) -> new SimonScreen(difficultyScreen, difficulty));
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.simon.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        List<Component> lines = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            SimonRecord record = RecordManager.getRecord(ID, difficulty.name(), SimonRecord.class, new SimonRecord(0, 0));
            if (record.getGamesPlayed() == 0) {
                lines.add(Component.translatable("gamebox.games.simon.record_line_empty", difficulty.getDisplayName()));
            } else {
                lines.add(Component.translatable("gamebox.games.simon.record_line",
                        difficulty.getDisplayName(), record.getBestScore(), record.getGamesPlayed()));
            }
        }
        return lines;
    }

    @Override
    public int getTotalGamesPlayed() {
        int total = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            SimonRecord record = RecordManager.getRecord(ID, difficulty.name(), SimonRecord.class, new SimonRecord(0, 0));
            total += record.getGamesPlayed();
        }
        return total;
    }
}