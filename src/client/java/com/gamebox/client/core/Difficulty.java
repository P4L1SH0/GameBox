package com.gamebox.client.core;

import net.minecraft.network.chat.Component;

/**
 * Generic difficulty levels shared across all mini-games.
 * Each mini-game is responsible for translating this into its own
 * concrete settings (speed, board size, etc.).
 */
public enum Difficulty {
    EASY,
    NORMAL,
    HARD,
    EXPERT;

    public Component getDisplayName() {
        return Component.translatable("gamebox.difficulty." + this.name().toLowerCase());
    }
}