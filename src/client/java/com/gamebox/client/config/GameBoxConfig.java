package com.gamebox.client.config;

import com.gamebox.GameBox;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persisted user preferences for GameBox, separate from RecordManager since
 * these represent settings rather than game history.
 */
public final class GameBoxConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "settings.json";

    private static GameBoxConfig instance;

    private boolean showScoreOverlay = true;
    private boolean soundEnabled = true;
    private boolean darkTheme = true;

    private GameBoxConfig() {
    }

    public static GameBoxConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    private static void load() {
        Path path = getConfigFile();
        GameBoxConfig loaded = null;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                loaded = GSON.fromJson(reader, GameBoxConfig.class);
            } catch (IOException | JsonParseException e) {
                GameBox.LOGGER.warn("[GameBox] Could not read settings.json, using defaults.", e);
            }
        }

        instance = loaded != null ? loaded : new GameBoxConfig();
    }

    public void save() {
        Path path = getConfigFile();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            GameBox.LOGGER.warn("[GameBox] Could not write settings.json.", e);
        }
    }

    private static Path getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("gamebox").resolve(FILE_NAME);
    }

    public boolean isShowScoreOverlay() {
        return showScoreOverlay;
    }

    public void setShowScoreOverlay(boolean showScoreOverlay) {
        this.showScoreOverlay = showScoreOverlay;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }
}