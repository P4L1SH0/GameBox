package com.gamebox.client.core;

import net.minecraft.network.chat.Component;

/**
 * Static, descriptive information about a mini-game.
 *
 * @param id          unique internal identifier, e.g. "snake".
 * @param displayName the name shown as a button in the menu.
 * @param description a short description shown in tooltips or records screens.
 */
public record MiniGameMetadata(String id, Component displayName, Component description) {
}