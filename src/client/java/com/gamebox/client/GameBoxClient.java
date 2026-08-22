package com.gamebox.client;

import com.gamebox.GameBox;
import com.gamebox.client.core.MiniGameRegistry;
import com.gamebox.client.games.flappybird.FlappyBirdGame;
import com.gamebox.client.games.game1010.Game1010Game;
import com.gamebox.client.games.game2048.Game2048Game;
import com.gamebox.client.games.lightsout.LightsOutGame;
import com.gamebox.client.games.mastermind.MastermindGame;
import com.gamebox.client.games.memory.MemoryGame;
import com.gamebox.client.games.minesweeper.MinesweeperGame;
import com.gamebox.client.games.simon.SimonGame;
import com.gamebox.client.games.snake.SnakeGame;
import com.gamebox.client.games.sudoku.SudokuGame;
import com.gamebox.client.network.GameBoxClientNetworking;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.MainMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class GameBoxClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(GameBox.MOD_ID, "general")
    );

    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        GameBox.LOGGER.info("[GameBox] Client initialized.");

        RecordManager.load();
        MiniGameRegistry.register(new SnakeGame());
        MiniGameRegistry.register(new LightsOutGame());
        MiniGameRegistry.register(new MinesweeperGame());
        MiniGameRegistry.register(new SudokuGame());
        MiniGameRegistry.register(new MemoryGame());
        MiniGameRegistry.register(new Game2048Game());
        MiniGameRegistry.register(new SimonGame());
        MiniGameRegistry.register(new MastermindGame());
        MiniGameRegistry.register(new Game1010Game());
        MiniGameRegistry.register(new FlappyBirdGame());

        GameBoxClientNetworking.init();
        GameBoxLeaderboardClient.init();

        openMenuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.gamebox.open_menu",
                        InputConstants.Type.KEYSYM,
                        InputConstants.KEY_G,
                        CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new MainMenuScreen());
                }
            }
        });
    }
}