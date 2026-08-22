package com.gamebox.client.games.game1010;

import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameMetadata;
import com.gamebox.client.records.RecordManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class Game1010Game implements MiniGame {

    public static final String ID = "1010";

    private static final MiniGameMetadata METADATA = new MiniGameMetadata(
            ID,
            Component.translatable("gamebox.games.game1010.name"),
            Component.translatable("gamebox.games.game1010.description")
    );

    @Override
    public MiniGameMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public Screen createScreen(Screen parent) {
        return new Game1010Screen(parent);
    }

    @Override
    public Component getInstructions() {
        return Component.translatable("gamebox.games.game1010.howto");
    }

    @Override
    public List<Component> getRecordSummaryLines() {
        Game1010Record record = RecordManager.getRecord(ID, "career", Game1010Record.class, new Game1010Record(0, 0));
        if (record.getGamesPlayed() == 0) {
            return List.of(Component.translatable("gamebox.games.game1010.record_line_empty"));
        }
        return List.of(Component.translatable("gamebox.games.game1010.record_line",
                record.getBestScore(), record.getGamesPlayed()));
    }

    @Override
    public int getTotalGamesPlayed() {
        return RecordManager.getRecord(ID, "career", Game1010Record.class, new Game1010Record(0, 0)).getGamesPlayed();
    }
}