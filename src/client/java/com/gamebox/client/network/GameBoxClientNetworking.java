package com.gamebox.client.network;

import com.gamebox.GameBox;
import com.gamebox.network.HelloAckClientboundPayload;
import com.gamebox.network.HelloServerboundPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side networking wiring. Sends a small "hello" packet whenever the
 * player joins a *real remote* server, and listens for the server's reply
 * to know whether that server also has GameBox installed.
 *
 * Singleplayer worlds (and "Open to LAN" games hosted by this same client)
 * always run an internal integrated server that necessarily has the exact
 * same mods as the client - so technically it "has GameBox" too, but that's
 * not a meaningful multiplayer scenario for shared leaderboards. We
 * deliberately skip the handshake entirely for those, via isLocalServer(),
 * so isServerAvailable() only ever reflects a genuine remote server.
 *
 * If no reply ever arrives (a vanilla server, or one without GameBox),
 * isServerAvailable() simply stays false - the mod works exactly as
 * before, fully locally, with no error or interruption.
 */
public final class GameBoxClientNetworking {

    private static boolean serverAvailable;

    private GameBoxClientNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(HelloAckClientboundPayload.TYPE, (payload, context) -> {
            serverAvailable = true;
            GameBox.LOGGER.info("[GameBox] Connected server supports shared leaderboards.");
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            serverAvailable = false;

            if (client.isLocalServer()) {
                // Singleplayer / self-hosted LAN: not a real remote server,
                // so the shared-leaderboard handshake is skipped entirely.
                return;
            }

            if (ClientPlayNetworking.canSend(HelloServerboundPayload.TYPE)) {
                ClientPlayNetworking.send(new HelloServerboundPayload());
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> serverAvailable = false);
    }

    public static boolean isServerAvailable() {
        return serverAvailable;
    }
}