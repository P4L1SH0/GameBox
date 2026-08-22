package com.gamebox.client.games.lightsout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Lights Out. Contains no Minecraft classes or rendering
 * code on purpose, so it can be understood, modified and tested in complete
 * isolation from the game engine.
 */
public class LightsOutLogic {

    private final LightsOutDifficultySettings settings;
    private final Random random;

    private boolean[][] lights;
    private int moveCount;
    private boolean solved;

    public LightsOutLogic(LightsOutDifficultySettings settings) {
        this(settings, new Random().nextLong());
    }

    public LightsOutLogic(LightsOutDifficultySettings settings, long seed) {
        this.settings = settings;
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        int size = settings.boardSize();
        this.lights = new boolean[size][size];
        this.moveCount = 0;
        this.solved = false;
        generateSolvableBoard();
    }

    private void generateSolvableBoard() {
        int size = settings.boardSize();

        // Use distinct cells for the scramble, rather than fully random
        // picks with repetition. Repeated picks can cancel each other out
        // (toggling the same cell twice undoes it), which was silently
        // making higher difficulties much easier than intended.
        List<int[]> allCells = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                allCells.add(new int[]{x, y});
            }
        }
        Collections.shuffle(allCells, random);

        int movesToApply = Math.min(settings.scrambleMoves(), allCells.size());
        for (int i = 0; i < movesToApply; i++) {
            int[] cell = allCells.get(i);
            applyToggle(cell[0], cell[1]);
        }

        if (isAllOff()) {
            // Extremely unlikely (the chosen toggles happened to cancel out
            // completely through board overlap), but guard against
            // generating an already-solved puzzle.
            applyToggle(random.nextInt(size), random.nextInt(size));
        }
    }

    public void click(int x, int y) {
        if (solved) {
            return;
        }
        applyToggle(x, y);
        moveCount++;
        if (isAllOff()) {
            solved = true;
        }
    }

    private void applyToggle(int x, int y) {
        toggleCell(x, y);
        toggleCell(x - 1, y);
        toggleCell(x + 1, y);
        toggleCell(x, y - 1);
        toggleCell(x, y + 1);
    }

    private void toggleCell(int x, int y) {
        int size = settings.boardSize();
        if (x >= 0 && x < size && y >= 0 && y < size) {
            lights[x][y] = !lights[x][y];
        }
    }

    private boolean isAllOff() {
        for (boolean[] row : lights) {
            for (boolean cell : row) {
                if (cell) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public boolean isLightOn(int x, int y) {
        return lights[x][y];
    }

    public int getBoardSize() {
        return settings.boardSize();
    }

    public int getMoveCount() {
        return moveCount;
    }

    public boolean isSolved() {
        return solved;
    }

    public LightsOutDifficultySettings getSettings() {
        return settings;
    }
}