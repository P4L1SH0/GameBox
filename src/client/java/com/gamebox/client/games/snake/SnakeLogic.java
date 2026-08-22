package com.gamebox.client.games.snake;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure game logic for Snake. Contains no Minecraft classes or rendering code
 * on purpose, so it can be understood, modified and tested in complete
 * isolation from the game engine.
 */
public class SnakeLogic {

    private final SnakeDifficultySettings settings;
    private final Random random;
    private final Set<GridPosition> obstacles = new HashSet<>();

    private Deque<GridPosition> body;
    private Direction direction;
    private Direction pendingDirection;
    private GridPosition food;

    private int score;
    private boolean gameOver;
    private boolean paused;

    public SnakeLogic(SnakeDifficultySettings settings) {
        this(settings, new Random().nextLong());
    }

    public SnakeLogic(SnakeDifficultySettings settings, long seed) {
        this.settings = settings;
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        this.body = new ArrayDeque<>();
        int centerX = settings.boardWidth() / 2;
        int centerY = settings.boardHeight() / 2;
        this.body.addFirst(new GridPosition(centerX, centerY));
        this.direction = Direction.RIGHT;
        this.pendingDirection = Direction.RIGHT;
        this.score = 0;
        this.gameOver = false;
        this.paused = false;

        this.obstacles.clear();
        generateObstacles();

        spawnFood();
    }

    private void generateObstacles() {
        int placed = 0;
        int attempts = 0;
        while (placed < settings.obstacleCount() && attempts < settings.obstacleCount() * 50) {
            attempts++;
            GridPosition candidate = new GridPosition(
                    random.nextInt(settings.boardWidth()),
                    random.nextInt(settings.boardHeight())
            );
            if (!body.contains(candidate) && !obstacles.contains(candidate) && !isNearCenter(candidate)) {
                obstacles.add(candidate);
                placed++;
            }
        }
    }

    private boolean isNearCenter(GridPosition position) {
        int centerX = settings.boardWidth() / 2;
        int centerY = settings.boardHeight() / 2;
        return Math.abs(position.x() - centerX) <= 2 && Math.abs(position.y() - centerY) <= 2;
    }

    public void changeDirection(Direction newDirection) {
        if (newDirection != direction.opposite()) {
            this.pendingDirection = newDirection;
        }
    }

    public void togglePause() {
        if (!gameOver) {
            this.paused = !this.paused;
        }
    }

    public void tick() {
        if (gameOver || paused) {
            return;
        }

        this.direction = pendingDirection;
        GridPosition head = body.peekFirst();
        GridPosition newHead = head.translate(direction.dx, direction.dy);

        if (isOutOfBounds(newHead) || isSelfCollision(newHead) || obstacles.contains(newHead)) {
            this.gameOver = true;
            return;
        }

        body.addFirst(newHead);

        if (newHead.equals(food)) {
            score++;
            spawnFood();
        } else {
            body.removeLast();
        }
    }

    private boolean isOutOfBounds(GridPosition position) {
        return position.x() < 0 || position.y() < 0
                || position.x() >= settings.boardWidth() || position.y() >= settings.boardHeight();
    }

    private boolean isSelfCollision(GridPosition newHead) {
        return body.contains(newHead);
    }

    private void spawnFood() {
        int totalCells = settings.boardWidth() * settings.boardHeight();
        int maxAttempts = Math.max(totalCells * 4, 20);

        GridPosition candidate;
        int attempts = 0;
        do {
            candidate = new GridPosition(random.nextInt(settings.boardWidth()), random.nextInt(settings.boardHeight()));
            attempts++;
        } while ((body.contains(candidate) || obstacles.contains(candidate)) && attempts < maxAttempts);

        this.food = candidate;
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public SnakeDifficultySettings getSettings() {
        return settings;
    }

    public List<GridPosition> getBody() {
        return List.copyOf(body);
    }

    public Set<GridPosition> getObstacles() {
        return Set.copyOf(obstacles);
    }

    public GridPosition getFood() {
        return food;
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getBoardWidth() {
        return settings.boardWidth();
    }

    public int getBoardHeight() {
        return settings.boardHeight();
    }

    /**
     * @return the direction the snake is currently moving/facing, used by
     * the screen layer to orient the head sprite.
     */
    public Direction getDirection() {
        return direction;
    }
}