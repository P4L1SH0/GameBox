package com.gamebox.network;

/**
 * A single leaderboard entry as sent to the client - name, value, and the
 * player's UUID (as a string) so the client can look up their skin if
 * they happen to be currently connected to the same server.
 */
public class ClientLeaderboardEntry {

    private String playerId;
    private String playerName;
    private long value;

    // Required by Gson for deserialization.
    public ClientLeaderboardEntry() {
    }

    public ClientLeaderboardEntry(String playerId, String playerName, long value) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.value = value;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getValue() {
        return value;
    }
}