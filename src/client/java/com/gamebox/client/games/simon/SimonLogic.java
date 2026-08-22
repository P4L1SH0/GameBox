package com.gamebox.client.games.simon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Simon. Contains no Minecraft classes or rendering
 * code on purpose, so it can be understood, modified and tested in complete
 * isolation from the game engine.
 *
 * This class has no concept of timing or flashing - it only tracks the
 * target sequence and the player's replay progress. Displaying the
 * sequence with delays between flashes is entirely the screen layer's
 * responsibility.
 */
public class SimonLogic {

    public static final int COLOR_COUNT = 4;

    private final SimonDifficultySettings settings;
    private final Random random;

    private final List<Integer> sequence = new ArrayList<>();
    private int playerProgress;
    private boolean gameOver;

    public SimonLogic(SimonDifficultySettings settings) {
        this(settings, new Random().nextLong());
    }

    public SimonLogic(SimonDifficultySettings settings, long seed) {
        this.settings = settings;
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        sequence.clear();
        playerProgress = 0;
        gameOver = false;
        sequence.add(random.nextInt(COLOR_COUNT));
    }

    /**
     * Registers a player press on the given color (0..COLOR_COUNT-1).
     * If it matches the expected next color in the sequence, progress
     * advances; if the sequence is fully replayed, a new color is appended
     * and progress resets for the next round. A wrong color ends the game.
     * Does nothing once the game is over.
     */
    public void playerInput(int colorIndex) {
        if (gameOver) {
            return;
        }

        if (sequence.get(playerProgress) != colorIndex) {
            gameOver = true;
            return;
        }

        playerProgress++;
        if (playerProgress == sequence.size()) {
            playerProgress = 0;
            sequence.add(random.nextInt(COLOR_COUNT));
        }
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public List<Integer> getSequence() {
        return List.copyOf(sequence);
    }

    public int getPlayerProgress() {
        return playerProgress;
    }

    /**
     * @return the number of fully completed rounds - i.e. the score.
     * The sequence always has one more color than completed rounds
     * (the round currently being played), so this is sequence size - 1.
     */
    public int getScore() {
        return sequence.size() - 1;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public SimonDifficultySettings getSettings() {
        return settings;
    }
}