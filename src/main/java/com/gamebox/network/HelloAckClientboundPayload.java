package com.gamebox.network;

import com.gamebox.GameBox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent by the server in reply to HelloServerboundPayload, confirming that
 * this server also has GameBox installed and supports the shared
 * leaderboard features (added in a later step).
 */
public record HelloAckClientboundPayload() implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(GameBox.MOD_ID, "hello_ack");
    public static final CustomPacketPayload.Type<HelloAckClientboundPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloAckClientboundPayload> CODEC =
            StreamCodec.unit(new HelloAckClientboundPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}