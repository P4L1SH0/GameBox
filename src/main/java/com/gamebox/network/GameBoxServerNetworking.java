package com.gamebox.network;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side (common initializer) networking handlers. Only meaningful
 * when GameBox is installed on the server too.
 */
public final class GameBoxServerNetworking {

    private static final Gson GSON = new Gson();

    private GameBoxServerNetworking() {
    }

    public static void registerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(HelloServerboundPayload.TYPE, (payload, context) ->
                ServerPlayNetworking.send(context.player(), new HelloAckClientboundPayload())
        );

        ServerPlayNetworking.registerGlobalReceiver(SubmitScorePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLeaderboardManager.submitScore(
                    payload.gameId(),
                    payload.difficultyKey(),
                    player.getUUID(),
                    player.getName().getString(),
                    payload.value(),
                    payload.higherIsBetter()
            );
        });

        ServerPlayNetworking.registerGlobalReceiver(LeaderboardRequestPayload.TYPE, (payload, context) -> {
            List<LeaderboardEntry> entries = ServerLeaderboardManager.getLeaderboard(payload.gameId(), payload.difficultyKey());
            List<ClientLeaderboardEntry> clientEntries = new ArrayList<>();
            for (LeaderboardEntry entry : entries) {
                clientEntries.add(new ClientLeaderboardEntry(
                        entry.getPlayerId().toString(),
                        entry.getPlayerName(),
                        entry.getValue()
                ));
            }
            String json = GSON.toJson(clientEntries);
            ServerPlayNetworking.send(context.player(),
                    new LeaderboardResponsePayload(payload.gameId(), payload.difficultyKey(), json));
        });
    }
}