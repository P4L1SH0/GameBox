package com.gamebox.network;

import com.gamebox.GameBox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent by the client to submit a score for a given mini-game and
 * difficulty key. higherIsBetter tells the server how to compare this
 * value against the player's existing entry - some games rank by highest
 * score, others by fewest moves/attempts (lower is better).
 */
public record SubmitScorePayload(String gameId, String difficultyKey, long value, boolean higherIsBetter)
        implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(GameBox.MOD_ID, "submit_score");
    public static final CustomPacketPayload.Type<SubmitScorePayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitScorePayload> CODEC = StreamCodec.composite(
            GameBoxStreamCodecs.STRING, SubmitScorePayload::gameId,
            GameBoxStreamCodecs.STRING, SubmitScorePayload::difficultyKey,
            GameBoxStreamCodecs.LONG, SubmitScorePayload::value,
            GameBoxStreamCodecs.BOOLEAN, SubmitScorePayload::higherIsBetter,
            SubmitScorePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}