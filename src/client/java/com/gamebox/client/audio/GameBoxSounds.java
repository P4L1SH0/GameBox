package com.gamebox.client.audio;

import com.gamebox.client.config.GameBoxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/**
 * Small wrapper around Minecraft's client-side sound system, used for all
 * of GameBox's UI feedback sounds (eating, solving, button clicks...).
 *
 * These are purely client-side, cosmetic sounds - not tied to any entity or
 * world position - so SimpleSoundInstance is the correct tool here, unlike
 * Level#playSound (which is for server-synced, world-based sounds).
 *
 * Two sets of overloads exist because vanilla's SoundEvents constants are
 * not all the same type: some are plain SoundEvent, others are
 * Holder<SoundEvent>. Having both means callers don't need to care which
 * one a given constant happens to be. Holder<SoundEvent> is unwrapped to
 * its underlying SoundEvent via .value() before use, since
 * SimpleSoundInstance.forUI's 3-argument (pitch + volume) overload only
 * accepts a plain SoundEvent.
 */
public final class GameBoxSounds {

    private GameBoxSounds() {
    }

    public static void play(SoundEvent sound) {
        play(sound, 1.0F);
    }

    public static void play(SoundEvent sound, float pitch) {
        play(sound, pitch, 1.0F);
    }

    public static void play(SoundEvent sound, float pitch, float volume) {
        if (!GameBoxConfig.get().isSoundEnabled()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void play(Holder<SoundEvent> sound) {
        play(sound, 1.0F);
    }

    public static void play(Holder<SoundEvent> sound, float pitch) {
        play(sound, pitch, 1.0F);
    }

    public static void play(Holder<SoundEvent> sound, float pitch, float volume) {
        play(sound.value(), pitch, volume);
    }
}