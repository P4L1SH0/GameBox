package com.gamebox.client.core;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Represents a mini-game that can be launched from the GameBox menu.
 * Implementations must stay free of any Minecraft-specific rendering logic
 * beyond creating their entry screen; the actual game logic belongs in a
 * separate, Minecraft-free class (see SnakeLogic for an example).
 */
public interface MiniGame {

    MiniGameMetadata getMetadata();

    /**
     * Creates the entry screen for this mini-game. The returned screen is
     * responsible for returning to {@code parent} when it closes.
     */
    Screen createScreen(Screen parent);

    /**
     * One formatted line of text per difficulty (or per whatever grouping
     * makes sense for this game), used by the Records screen.
     */
    List<Component> getRecordSummaryLines();

    /**
     * Instructions for how to play this game, shown on the "How to Play"
     * screen. Lines are separated with "\n" within the single returned
     * Component - HowToPlayScreen splits on that to render each line.
     */
    Component getInstructions();

    /**
     * @return the total number of games ever played/completed for this
     * mini-game, summed across every difficulty (or just its single
     * "career" record for games without difficulties). Used by the
     * Statistics screen to compute totals and find the most-played game.
     */
    int getTotalGamesPlayed();
}