package com.gamebox.client.records;

import com.gamebox.GameBox;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generic local-record storage, shared by every mini-game. Stores one JSON
 * object per (gameId, difficultyKey) pair, in a single file under the mod's
 * config folder.
 *
 * The file also carries a checksum so that manual edits (e.g. opening the
 * file in a text editor and changing a number) are detected on load and the
 * affected records are reset, rather than trusted blindly. This is NOT real
 * anti-cheat protection - it only deters casual tampering. A determined
 * player with access to the mod's source can always reproduce the checksum.
 * Real protection would require server-side validation, relevant only if
 * GameBox ever gets online leaderboards.
 */
public final class RecordManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "records.json";
    private static final String INTEGRITY_SALT = "GameBox-v1-4f8a2c";

    private static JsonObject data;
    private static boolean tampered;

    private RecordManager() {
    }

    /**
     * Loads records.json from disk into memory. Safe to call multiple times;
     * subsequent calls do nothing once records are already loaded. Missing,
     * corrupted, or hand-edited files result in an empty (but valid) record
     * set, rather than crashing the game or trusting altered data.
     */
    public static void load() {
        if (data != null) {
            return;
        }

        Path path = getRecordsFile();
        JsonObject fileRoot = null;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                fileRoot = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException | JsonParseException e) {
                GameBox.LOGGER.warn("[GameBox] Could not read records.json, starting with empty records.", e);
            }
        }

        if (fileRoot != null && fileRoot.has("data") && fileRoot.has("checksum")) {
            JsonObject loadedData = fileRoot.getAsJsonObject("data");
            String expectedChecksum = fileRoot.get("checksum").getAsString();
            if (computeChecksum(loadedData).equals(expectedChecksum)) {
                data = loadedData;
            } else {
                GameBox.LOGGER.warn("[GameBox] records.json failed its integrity check "
                        + "(it looks like it was edited outside the game) - resetting records.");
                tampered = true;
                data = new JsonObject();
            }
        } else {
            data = new JsonObject();
        }
    }

    public static void save() {
        Path path = getRecordsFile();
        try {
            Files.createDirectories(path.getParent());

            JsonObject fileRoot = new JsonObject();
            fileRoot.add("data", data);
            fileRoot.addProperty("checksum", computeChecksum(data));

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(fileRoot, writer);
            }
        } catch (IOException e) {
            GameBox.LOGGER.warn("[GameBox] Could not write records.json.", e);
        }
    }

    /**
     * @return true if the last load() detected a hand-edited (or corrupted)
     * records file and had to reset it. Useful for showing the player a
     * one-time explanation instead of silently wiping their records.
     */
    public static boolean wasTampered() {
        return tampered;
    }

    public static <T> T getRecord(String gameId, String difficultyKey, Class<T> type, T defaultValue) {
        load();
        JsonObject gameSection = data.getAsJsonObject(gameId);
        if (gameSection == null || !gameSection.has(difficultyKey)) {
            return defaultValue;
        }
        return GSON.fromJson(gameSection.get(difficultyKey), type);
    }

    public static void putRecord(String gameId, String difficultyKey, Object record) {
        load();
        JsonObject gameSection = data.getAsJsonObject(gameId);
        if (gameSection == null) {
            gameSection = new JsonObject();
            data.add(gameId, gameSection);
        }
        gameSection.add(difficultyKey, GSON.toJsonTree(record));
        save();
    }

    private static String computeChecksum(JsonObject dataObject) {
        try {
            String canonicalJson = GSON.toJson(dataObject);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(INTEGRITY_SALT.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JDK algorithm, this should never happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static Path getRecordsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("gamebox").resolve(FILE_NAME);
    }
}