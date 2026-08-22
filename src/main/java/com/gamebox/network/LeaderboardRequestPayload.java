package com.gamebox.network;

import com.gamebox.GameBox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent by the client to ask the server for the current leaderboard of a
 * given mini-game and difficulty key.
 */
public record LeaderboardRequestPayload(String gameId, String difficultyKey) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(GameBox.MOD_ID, "leaderboard_request");
    public static final CustomPacketPayload.Type<LeaderboardRequestPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardRequestPayload> CODEC = StreamCodec.composite(
            GameBoxStreamCodecs.STRING, LeaderboardRequestPayload::gameId,
            GameBoxStreamCodecs.STRING, LeaderboardRequestPayload::difficultyKey,
            LeaderboardRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}