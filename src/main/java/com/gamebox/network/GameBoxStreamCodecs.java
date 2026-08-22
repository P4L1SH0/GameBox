package com.gamebox.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Small hand-built StreamCodecs for primitive types, using only the raw
 * buffer read/write methods that have been stable across many Minecraft
 * versions - this avoids depending on a specific set of pre-built
 * ByteBufCodecs constants, whose exact names can vary between versions.
 */
final class GameBoxStreamCodecs {

    private GameBoxStreamCodecs() {
    }

    static final StreamCodec<RegistryFriendlyByteBuf, String> STRING =
            StreamCodec.of((buf, value) -> buf.writeUtf(value), RegistryFriendlyByteBuf::readUtf);

    static final StreamCodec<RegistryFriendlyByteBuf, Long> LONG =
            StreamCodec.of((buf, value) -> buf.writeLong(value), RegistryFriendlyByteBuf::readLong);

    static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOLEAN =
            StreamCodec.of((buf, value) -> buf.writeBoolean(value), RegistryFriendlyByteBuf::readBoolean);
}