package com.gamebox.network;

import java.util.UUID;

/**
 * A single entry in a shared server leaderboard: one player's best result
 * for a given (gameId, difficultyKey) pair.
 */
public class LeaderboardEntry {

    private String playerId;
    private String playerName;
    private long value;

    // Required by Gson for deserialization.
    public LeaderboardEntry() {
    }

    public LeaderboardEntry(UUID playerId, String playerName, long value) {
        this.playerId = playerId.toString();
        this.playerName = playerName;
        this.value = value;
    }

    public UUID getPlayerId() {
        return UUID.fromString(playerId);
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getValue() {
        return value;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setValue(long value) {
        this.value = value;
    }
}