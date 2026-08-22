package com.gamebox.client.games.minesweeper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure game logic for Minesweeper. Contains no Minecraft classes or
 * rendering code on purpose, so it can be understood, modified and tested
 * in complete isolation from the game engine.
 */
public class MinesweeperLogic {

    private final MinesweeperDifficultySettings settings;
    private final Random random;

    private final Set<GridPosition> mines = new HashSet<>();
    private final boolean[][] revealed;
    private final boolean[][] flagged;

    private boolean minesPlaced;
    private boolean exploded;
    private boolean won;
    private int revealedCount;

    public MinesweeperLogic(MinesweeperDifficultySettings settings) {
        this(settings, new Random().nextLong());
    }

    public MinesweeperLogic(MinesweeperDifficultySettings settings, long seed) {
        this.settings = settings;
        this.random = new Random(seed);
        this.revealed = new boolean[settings.boardWidth()][settings.boardHeight()];
        this.flagged = new boolean[settings.boardWidth()][settings.boardHeight()];
    }

    /**
     * Reveals the cell at (x, y). On the very first call, mines are placed
     * across the board, guaranteed to avoid (x, y) and its neighbors. Does
     * nothing if the game is already finished, or the cell is flagged or
     * already revealed.
     */
    public void reveal(int x, int y) {
        if (isFinished() || flagged[x][y] || revealed[x][y]) {
            return;
        }

        if (!minesPlaced) {
            placeMines(x, y);
            minesPlaced = true;
        }

        if (mines.contains(new GridPosition(x, y))) {
            revealed[x][y] = true;
            exploded = true;
            return;
        }

        floodReveal(x, y);
        checkForWin();
    }

    public void toggleFlag(int x, int y) {
        if (isFinished() || revealed[x][y]) {
            return;
        }
        flagged[x][y] = !flagged[x][y];
    }

    /**
     * "Chord" reveal: if the number of flagged neighbors around an already
     * revealed cell matches its adjacent mine count, reveals every
     * remaining (non-flagged, non-revealed) neighbor at once. If any
     * flag was placed incorrectly, this can trigger an explosion, exactly
     * like clicking that mine directly would.
     *
     * @return true if this actually revealed anything (i.e. the flag count
     * matched and there was at least one neighbor to reveal).
     */
    public boolean chord(int x, int y) {
        if (isFinished() || !revealed[x][y]) {
            return false;
        }

        int adjacentMines = getAdjacentMineCount(x, y);
        int flaggedNeighbors = 0;
        List<GridPosition> toReveal = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (!isInBounds(nx, ny)) {
                    continue;
                }
                if (flagged[nx][ny]) {
                    flaggedNeighbors++;
                } else if (!revealed[nx][ny]) {
                    toReveal.add(new GridPosition(nx, ny));
                }
            }
        }

        if (flaggedNeighbors != adjacentMines || toReveal.isEmpty()) {
            return false;
        }

        for (GridPosition position : toReveal) {
            reveal(position.x(), position.y());
        }
        return true;
    }

    private void placeMines(int excludeX, int excludeY) {
        Set<GridPosition> excluded = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                excluded.add(new GridPosition(excludeX + dx, excludeY + dy));
            }
        }

        List<GridPosition> candidates = new ArrayList<>();
        for (int x = 0; x < settings.boardWidth(); x++) {
            for (int y = 0; y < settings.boardHeight(); y++) {
                GridPosition position = new GridPosition(x, y);
                if (!excluded.contains(position)) {
                    candidates.add(position);
                }
            }
        }
        Collections.shuffle(candidates, random);

        int mineCount = Math.min(settings.mineCount(), candidates.size());
        for (int i = 0; i < mineCount; i++) {
            mines.add(candidates.get(i));
        }
    }

    private void floodReveal(int startX, int startY) {
        Deque<GridPosition> pending = new ArrayDeque<>();
        pending.push(new GridPosition(startX, startY));

        while (!pending.isEmpty()) {
            GridPosition current = pending.pop();
            int x = current.x();
            int y = current.y();

            if (!isInBounds(x, y) || revealed[x][y] || flagged[x][y]) {
                continue;
            }

            revealed[x][y] = true;
            revealedCount++;

            if (getAdjacentMineCount(x, y) == 0) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            pending.push(new GridPosition(x + dx, y + dy));
                        }
                    }
                }
            }
        }
    }

    private void checkForWin() {
        int totalCells = settings.boardWidth() * settings.boardHeight();
        int safeCells = totalCells - mines.size();
        if (revealedCount >= safeCells) {
            won = true;
        }
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < settings.boardWidth() && y >= 0 && y < settings.boardHeight();
    }

    public int getAdjacentMineCount(int x, int y) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx != 0 || dy != 0) && mines.contains(new GridPosition(x + dx, y + dy))) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public boolean isRevealed(int x, int y) {
        return revealed[x][y];
    }

    public boolean isFlagged(int x, int y) {
        return flagged[x][y];
    }

    public boolean isMine(int x, int y) {
        return mines.contains(new GridPosition(x, y));
    }

    public int getBoardWidth() {
        return settings.boardWidth();
    }

    public int getBoardHeight() {
        return settings.boardHeight();
    }

    public int getMineCount() {
        return settings.mineCount();
    }

    public int getFlagsRemaining() {
        int flagsPlaced = 0;
        for (boolean[] row : flagged) {
            for (boolean cell : row) {
                if (cell) {
                    flagsPlaced++;
                }
            }
        }
        return settings.mineCount() - flagsPlaced;
    }

    public boolean isExploded() {
        return exploded;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isFinished() {
        return exploded || won;
    }

    public MinesweeperDifficultySettings getSettings() {
        return settings;
    }
}