package com.gamebox.client.games.lightsout;

import com.gamebox.client.core.Difficulty;
import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.DifficultySelectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class LightsOutGame implements MiniGame {

    public static final String ID = "lights_out";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.lights_out.name"),
            Component.translatable("gamebox.games.lights_out.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new DifficultySelectScreen(parent, METADATA.displayName(), getInstructions(), ID,
                (difficultyScreen, difficulty) -> new LightsOutScreen(difficultyScreen, difficulty));
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.lights_out.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        List<Component> lines = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            LightsOutRecord record = RecordManager.getRecord(
                    ID, difficulty.name(), LightsOutRecord.class, new LightsOutRecord(0, 0, 0));

            if (record.getGamesCompleted() == 0) {
                lines.add(Component.translatable("gamebox.games.lights_out.record_line_empty", difficulty.getDisplayName()));
            } else {
                lines.add(Component.translatable("gamebox.games.lights_out.record_line",
                        difficulty.getDisplayName(), record.getBestMoves(), record.getGamesCompleted()));
            }
        }
        return lines;
    }

    @Override
    public int getTotalGamesPlayed() {
        int total = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            LightsOutRecord record = RecordManager.getRecord(
                    ID, difficulty.name(), LightsOutRecord.class, new LightsOutRecord(0, 0, 0));
            total += record.getGamesCompleted();
        }
        return total;
    }
}