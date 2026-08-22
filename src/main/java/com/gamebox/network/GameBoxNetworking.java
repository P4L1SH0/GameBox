package com.gamebox.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers GameBox's custom network payloads. Called once from the common
 * (main) entrypoint, so both the client and server code paths know how to
 * (de)serialize these payloads regardless of which side is running.
 */
public final class GameBoxNetworking {

    private GameBoxNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(HelloServerboundPayload.TYPE, HelloServerboundPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HelloAckClientboundPayload.TYPE, HelloAckClientboundPayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(SubmitScorePayload.TYPE, SubmitScorePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LeaderboardRequestPayload.TYPE, LeaderboardRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LeaderboardResponsePayload.TYPE, LeaderboardResponsePayload.CODEC);
    }
}