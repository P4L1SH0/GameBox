package com.gamebox.client.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry of every mini-game available in GameBox.
 * The main menu iterates this registry to build its buttons, so registering
 * a new mini-game here is the only step needed to make it appear in the menu.
 */
public final class MiniGameRegistry {

    private static final Map<String, MiniGame> GAMES = new LinkedHashMap<>();

    private MiniGameRegistry() {
    }

    public static void register(MiniGame game) {
        GAMES.put(game.getMetadata().id(), game);
    }

    public static Collection<MiniGame> getAll() {
        return Collections.unmodifiableCollection(GAMES.values());
    }
}