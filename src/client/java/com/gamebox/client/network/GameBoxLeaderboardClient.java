package com.gamebox.client.network;

import com.gamebox.network.ClientLeaderboardEntry;
import com.gamebox.network.LeaderboardRequestPayload;
import com.gamebox.network.LeaderboardResponsePayload;
import com.gamebox.network.SubmitScorePayload;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client-side entry point for the shared server leaderboard feature.
 * Mini-game screens call submitScore(...) after a finished game, and
 * requestLeaderboard(...) with a callback to display a server-wide
 * leaderboard - both are safe no-ops if the connected server doesn't
 * support GameBox.
 *
 * Note: only one leaderboard request can be "in flight" at a time (a
 * single pending callback) - fine for now since we only show one
 * leaderboard screen at once, but worth revisiting if that ever changes.
 */
public final class GameBoxLeaderboardClient {

    private static final Gson GSON = new Gson();
    private static Consumer<List<ClientLeaderboardEntry>> pendingCallback;

    private GameBoxLeaderboardClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(LeaderboardResponsePayload.TYPE, (payload, context) -> {
            Type listType = new TypeToken<List<ClientLeaderboardEntry>>() {
            }.getType();
            List<ClientLeaderboardEntry> entries = GSON.fromJson(payload.entriesJson(), listType);

            Consumer<List<ClientLeaderboardEntry>> callback = pendingCallback;
            pendingCallback = null;
            if (callback != null) {
                context.client().execute(() -> callback.accept(entries));
            }
        });
    }

    public static void submitScore(String gameId, String difficultyKey, long value, boolean higherIsBetter) {
        if (!GameBoxClientNetworking.isServerAvailable()) {
            return;
        }
        ClientPlayNetworking.send(new SubmitScorePayload(gameId, difficultyKey, value, higherIsBetter));
    }

    public static void requestLeaderboard(String gameId, String difficultyKey, Consumer<List<ClientLeaderboardEntry>> callback) {
        if (!GameBoxClientNetworking.isServerAvailable()) {
            return;
        }
        pendingCallback = callback;
        ClientPlayNetworking.send(new LeaderboardRequestPayload(gameId, difficultyKey));
    }
}