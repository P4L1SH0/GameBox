package com.gamebox;

import com.gamebox.network.GameBoxNetworking;
import com.gamebox.network.GameBoxServerNetworking;
import com.gamebox.network.ServerLeaderboardManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameBox implements ModInitializer {

    public static final String MOD_ID = "gamebox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[GameBox] Common entrypoint initialized.");
        GameBoxNetworking.registerPayloadTypes();
        GameBoxServerNetworking.registerHandlers();
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLeaderboardManager::load);
    }
}