package com.gamebox.network;

import com.gamebox.GameBox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent by the client right after joining a server, to check whether that
 * server also has GameBox installed. Carries no data - just its presence
 * (and the server's HelloAckClientboundPayload reply) is the signal.
 */
public record HelloServerboundPayload() implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(GameBox.MOD_ID, "hello");
    public static final CustomPacketPayload.Type<HelloServerboundPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloServerboundPayload> CODEC =
            StreamCodec.unit(new HelloServerboundPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}