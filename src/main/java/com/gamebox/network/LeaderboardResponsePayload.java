package com.gamebox.network;

import com.gamebox.GameBox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent by the server in reply to a LeaderboardRequestPayload. entriesJson
 * is a small Gson-serialized JSON array of ClientLeaderboardEntry objects.
 */
public record LeaderboardResponsePayload(String gameId, String difficultyKey, String entriesJson)
        implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(GameBox.MOD_ID, "leaderboard_response");
    public static final CustomPacketPayload.Type<LeaderboardResponsePayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardResponsePayload> CODEC = StreamCodec.composite(
            GameBoxStreamCodecs.STRING, LeaderboardResponsePayload::gameId,
            GameBoxStreamCodecs.STRING, LeaderboardResponsePayload::difficultyKey,
            GameBoxStreamCodecs.STRING, LeaderboardResponsePayload::entriesJson,
            LeaderboardResponsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}