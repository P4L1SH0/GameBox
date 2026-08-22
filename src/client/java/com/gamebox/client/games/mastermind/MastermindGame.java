package com.gamebox.client.games.mastermind;

import com.gamebox.client.core.Difficulty;
import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.DifficultySelectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MastermindGame implements MiniGame {

    public static final String ID = "mastermind";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.mastermind.name"),
            Component.translatable("gamebox.games.mastermind.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new DifficultySelectScreen(parent, METADATA.displayName(), getInstructions(), ID,
                (difficultyScreen, difficulty) -> new MastermindScreen(difficultyScreen, difficulty));
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.mastermind.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        List<Component> lines = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            MastermindRecord record = RecordManager.getRecord(ID, difficulty.name(), MastermindRecord.class, new MastermindRecord(0, 0));
            if (record.getGamesWon() == 0) {
                lines.add(Component.translatable("gamebox.games.mastermind.record_line_empty", difficulty.getDisplayName()));
            } else {
                lines.add(Component.translatable("gamebox.games.mastermind.record_line",
                        difficulty.getDisplayName(), record.getBestAttempts(), record.getGamesWon()));
            }
        }
        return lines;
    }

    @Override
    public int getTotalGamesPlayed() {
        int total = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            MastermindRecord record = RecordManager.getRecord(ID, difficulty.name(), MastermindRecord.class, new MastermindRecord(0, 0));
            total += record.getGamesWon();
        }
        return total;
    }
}