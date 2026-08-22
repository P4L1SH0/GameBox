package com.gamebox.client.games.game2048;

import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class Game2048Game implements MiniGame {

    public static final String ID = "2048";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.game2048.name"),
            Component.translatable("gamebox.games.game2048.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new Game2048Screen(parent);
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.game2048.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        Game2048Record record = RecordManager.getRecord(ID, "career", Game2048Record.class, new Game2048Record(0, 0));
        if (record.getGamesPlayed() == 0) {
            return List.of(Component.translatable("gamebox.games.game2048.record_line_empty"));
        }
        return List.of(Component.translatable("gamebox.games.game2048.record_line",
                record.getBestScore(), record.getGamesPlayed()));
    }

    @Override
    public int getTotalGamesPlayed() {
        return RecordManager.getRecord(ID, "career", Game2048Record.class, new Game2048Record(0, 0)).getGamesPlayed();
    }
}