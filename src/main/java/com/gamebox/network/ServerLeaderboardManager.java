package com.gamebox.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Server-side shared leaderboard storage. Lives entirely on the server -
 * every connected player's client sees the same data, unlike the purely
 * local RecordManager on the client side.
 *
 * Stored as a single JSON file per server/world, keyed by "gameId:difficultyKey"
 * to a list of entries (one per player who has ever submitted a score for
 * that key).
 */
public final class ServerLeaderboardManager {

    private static final Logger LOGGER = Logger.getLogger("GameBox");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "gamebox_leaderboards.json";
    private static final int MAX_ENTRIES_PER_KEY = 100;

    private static Map<String, List<LeaderboardEntry>> leaderboards;
    private static Path currentFilePath;

    private ServerLeaderboardManager() {
    }

    /**
     * Loads (or re-loads) the leaderboard file for the given server. Safe
     * to call every time a server starts, since each server/world gets its
     * own file based on its own run directory.
     */
    public static void load(MinecraftServer server) {
        currentFilePath = server.getServerDirectory().resolve("gamebox").resolve(FILE_NAME);
        leaderboards = null;

        if (Files.exists(currentFilePath)) {
            try (Reader reader = Files.newBufferedReader(currentFilePath)) {
                Type mapType = new TypeToken<Map<String, List<LeaderboardEntry>>>() {
                }.getType();
                leaderboards = GSON.fromJson(reader, mapType);
            } catch (IOException | JsonParseException e) {
                LOGGER.warning("[GameBox] Could not read " + FILE_NAME + ", starting with an empty leaderboard.");
            }
        }

        if (leaderboards == null) {
            leaderboards = new HashMap<>();
        }
    }

    private static void save() {
        if (currentFilePath == null) {
            return;
        }
        try {
            Files.createDirectories(currentFilePath.getParent());
            try (Writer writer = Files.newBufferedWriter(currentFilePath)) {
                GSON.toJson(leaderboards, writer);
            }
        } catch (IOException e) {
            LOGGER.warning("[GameBox] Could not write " + FILE_NAME + ".");
        }
    }

    private static String keyFor(String gameId, String difficultyKey) {
        return gameId + ":" + difficultyKey;
    }

    /**
     * Submits a player's score for (gameId, difficultyKey). If the player
     * already has an entry, it's only replaced if the new value is better
     * (per {@code higherIsBetter}) - otherwise the submission is ignored.
     * The leaderboard is capped at MAX_ENTRIES_PER_KEY entries, keeping
     * only the best ones, so it can't grow unbounded on a long-running
     * server with many different players.
     *
     * @return true if this submission changed the stored leaderboard.
     */
    public static boolean submitScore(String gameId, String difficultyKey, UUID playerId, String playerName,
                                      long value, boolean higherIsBetter) {
        String key = keyFor(gameId, difficultyKey);
        List<LeaderboardEntry> entries = leaderboards.computeIfAbsent(key, k -> new ArrayList<>());

        LeaderboardEntry existing = null;
        for (LeaderboardEntry entry : entries) {
            if (entry.getPlayerId().equals(playerId)) {
                existing = entry;
                break;
            }
        }

        boolean changed;
        if (existing == null) {
            entries.add(new LeaderboardEntry(playerId, playerName, value));
            changed = true;
        } else {
            boolean isBetter = higherIsBetter ? value > existing.getValue() : value < existing.getValue();
            if (isBetter) {
                existing.setValue(value);
                existing.setPlayerName(playerName); // keep the display name current
                changed = true;
            } else {
                changed = false;
            }
        }

        if (changed) {
            sortAndTrim(entries, higherIsBetter);
            save();
        }
        return changed;
    }

    private static void sortAndTrim(List<LeaderboardEntry> entries, boolean higherIsBetter) {
        Comparator<LeaderboardEntry> comparator = Comparator.comparingLong(LeaderboardEntry::getValue);
        if (higherIsBetter) {
            comparator = comparator.reversed();
        }
        entries.sort(comparator);
        while (entries.size() > MAX_ENTRIES_PER_KEY) {
            entries.remove(entries.size() - 1);
        }
    }

    /**
     * @return the current leaderboard entries for (gameId, difficultyKey),
     * already sorted best-first, or an empty list if nobody has submitted
     * a score for that key yet.
     */
    public static List<LeaderboardEntry> getLeaderboard(String gameId, String difficultyKey) {
        return List.copyOf(leaderboards.getOrDefault(keyFor(gameId, difficultyKey), List.of()));
    }
}