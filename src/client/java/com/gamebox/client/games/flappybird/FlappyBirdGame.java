package com.gamebox.client.games.flappybird;

import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FlappyBirdGame implements MiniGame {

    public static final String ID = "flappy_bird";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.flappy_bird.name"),
            Component.translatable("gamebox.games.flappy_bird.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new FlappyBirdScreen(parent);
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.flappy_bird.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        FlappyBirdRecord record = RecordManager.getRecord(ID, "career", FlappyBirdRecord.class, new FlappyBirdRecord(0, 0));
        if (record.getGamesPlayed() == 0) {
            return List.of(Component.translatable("gamebox.games.flappy_bird.record_line_empty"));
        }
        return List.of(Component.translatable("gamebox.games.flappy_bird.record_line",
                record.getBestScore(), record.getGamesPlayed()));
    }

    @Override
    public int getTotalGamesPlayed() {
        return RecordManager.getRecord(ID, "career", FlappyBirdRecord.class, new FlappyBirdRecord(0, 0)).getGamesPlayed();
    }
}