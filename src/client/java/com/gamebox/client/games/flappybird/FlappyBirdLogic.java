package com.gamebox.client.games.flappybird;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Flappy Bird. Contains no Minecraft classes or
 * rendering code on purpose, so it can be understood, modified and tested
 * in complete isolation from the game engine.
 *
 * Unlike every other mini-game so far, this one uses continuous position
 * and simple physics (gravity + velocity) instead of a discrete grid -
 * it's simulated in an abstract world space (WORLD_WIDTH x WORLD_HEIGHT),
 * not real screen pixels. The screen layer maps this world onto whatever
 * screen space is actually available.
 */
public class FlappyBirdLogic {

    public static final float WORLD_WIDTH = 300f;
    public static final float WORLD_HEIGHT = 400f;

    public static final float BIRD_X = 60f;
    public static final float BIRD_RADIUS = 8f;

    private static final float GRAVITY = 0.22f;
    private static final float FLAP_VELOCITY = -4.3f;
    private static final float MAX_FALL_SPEED = 6.0f;

    public static final float PIPE_WIDTH = 32f;
    public static final float PIPE_GAP = 95f;
    private static final float PIPE_SPACING = 150f;
    private static final float SCROLL_SPEED = 2.0f;
    private static final float PIPE_MARGIN = 40f; // keep the gap away from the very top/bottom

    /**
     * A pipe pair (top + bottom, sharing one gap) at a given x position.
     * scored tracks whether the bird has already passed it, so it's only
     * counted once.
     */
    public static final class Pipe {
        private float x;
        private final float gapCenterY;
        private boolean scored;

        private Pipe(float x, float gapCenterY) {
            this.x = x;
            this.gapCenterY = gapCenterY;
        }

        public float getX() {
            return x;
        }

        public float getGapTop() {
            return gapCenterY - PIPE_GAP / 2f;
        }

        public float getGapBottom() {
            return gapCenterY + PIPE_GAP / 2f;
        }
    }

    private final Random random;
    private final List<Pipe> pipes = new ArrayList<>();

    private float birdY;
    private float velocityY;
    private int score;
    private boolean gameOver;

    public FlappyBirdLogic() {
        this(new Random().nextLong());
    }

    public FlappyBirdLogic(long seed) {
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        this.birdY = WORLD_HEIGHT / 2f;
        this.velocityY = 0f;
        this.score = 0;
        this.gameOver = false;
        this.pipes.clear();
        // The first pipe spawns right at the visible edge (instead of a
        // full PIPE_SPACING further out) so it starts entering the play
        // area immediately, rather than the player waiting several
        // seconds before anything shows up.
        spawnPipe(WORLD_WIDTH);
        spawnPipe(WORLD_WIDTH + PIPE_SPACING);
    }

    private void spawnPipe(float x) {
        float minCenter = PIPE_MARGIN + PIPE_GAP / 2f;
        float maxCenter = WORLD_HEIGHT - PIPE_MARGIN - PIPE_GAP / 2f;
        float gapCenterY = minCenter + random.nextFloat() * (maxCenter - minCenter);
        pipes.add(new Pipe(x, gapCenterY));
    }

    /**
     * Gives the bird an upward flap. Does nothing once the game is over.
     */
    public void flap() {
        if (!gameOver) {
            this.velocityY = FLAP_VELOCITY;
        }
    }

    /**
     * Advances the simulation by one tick: gravity, movement, pipe
     * scrolling/spawning, scoring, and collision detection. Does nothing
     * once the game is over.
     */
    public void tick() {
        if (gameOver) {
            return;
        }

        velocityY = Math.min(MAX_FALL_SPEED, velocityY + GRAVITY);
        birdY += velocityY;

        if (birdY - BIRD_RADIUS <= 0f || birdY + BIRD_RADIUS >= WORLD_HEIGHT) {
            gameOver = true;
            return;
        }

        for (Pipe pipe : pipes) {
            pipe.x -= SCROLL_SPEED;
        }
        pipes.removeIf(pipe -> pipe.x + PIPE_WIDTH < 0f);

        Pipe lastPipe = pipes.isEmpty() ? null : pipes.get(pipes.size() - 1);
        if (lastPipe == null || lastPipe.x <= WORLD_WIDTH - PIPE_SPACING + SCROLL_SPEED) {
            float lastX = lastPipe == null ? WORLD_WIDTH : lastPipe.x;
            spawnPipe(lastX + PIPE_SPACING);
        }

        for (Pipe pipe : pipes) {
            if (!pipe.scored && pipe.x + PIPE_WIDTH < BIRD_X) {
                pipe.scored = true;
                score++;
            }

            boolean overlapsHorizontally = BIRD_X + BIRD_RADIUS > pipe.x && BIRD_X - BIRD_RADIUS < pipe.x + PIPE_WIDTH;
            if (overlapsHorizontally) {
                boolean hitsGap = birdY - BIRD_RADIUS < pipe.getGapTop() || birdY + BIRD_RADIUS > pipe.getGapBottom();
                if (hitsGap) {
                    gameOver = true;
                    return;
                }
            }
        }
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public float getBirdY() {
        return birdY;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public List<Pipe> getPipes() {
        return List.copyOf(pipes);
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}