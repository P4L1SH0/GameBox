package com.gamebox.client.games.snake;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.core.Difficulty;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.GameBoxButton;
import com.gamebox.client.ui.HudBar;
import com.gamebox.client.ui.ItemIconRenderer;
import com.gamebox.client.ui.ServerLeaderboardPanel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class SnakeScreen extends Screen {

    private static final int MIN_CELL_SIZE = 6;
    private static final int MAX_CELL_SIZE = 24;
    private static final int TOP_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_MARGIN = 10;
    private static final int COUNTDOWN_SECONDS = 3;
    private static final int LEADERBOARD_GAP = 8;

    private static final int BOARD_BACKGROUND = 0xFFE8EAED;
    private static final int BOARD_BORDER = 0xFFB8BFC7;
    private static final int SNAKE_HEAD_COLOR = 0xFF2D6A4F;
    private static final int SNAKE_BODY_COLOR = 0xFF40916C;
    private static final int SNAKE_NOSE_COLOR = 0xFFDFF5E8;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int NEW_RECORD_COLOR = 0xFFFFD86B;

    private final Screen parentScreen;
    private final Difficulty difficulty;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private SnakeLogic logic;
    private int cellSize;
    private int boardX;
    private int boardY;
    private int tickCounter;
    private int ticksPerMove;
    private int countdownTicksRemaining;
    private boolean recordSaved;
    private boolean isNewRecord;
    private int bestScoreAtStart;
    private ItemStack foodItemStack;
    private ItemStack obstacleItemStack;

    public SnakeScreen(Screen parentScreen, Difficulty difficulty) {
        super(Component.translatable("gamebox.games.snake.name"));
        this.parentScreen = parentScreen;
        this.difficulty = difficulty;
    }

    @Override
    protected void init() {
        SnakeDifficultySettings settings = SnakeDifficultySettings.forDifficulty(difficulty);
        this.logic = new SnakeLogic(settings);
        this.ticksPerMove = Math.max(1, settings.tickIntervalMs() / 50);
        this.tickCounter = 0;
        this.foodItemStack = new ItemStack(Items.APPLE);
        this.obstacleItemStack = new ItemStack(Items.TNT);
        refreshBestScore();
        startCountdown();

        leaderboardPanel.refresh(SnakeGame.ID, difficulty.name());

        // Reserve room for the leaderboard panel (twice its width + gap)
        // before computing cell size, so the centered board naturally
        // shrinks enough to leave a real left margin for it - no need to
        // reposition the panel or hide it later.
        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int availableHeight = this.height - TOP_MARGIN - HudBar.reservedHeight() - BOTTOM_MARGIN;
        int maxCellByWidth = availableWidth / settings.boardWidth();
        int maxCellByHeight = availableHeight / settings.boardHeight();
        this.cellSize = Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, Math.min(maxCellByWidth, maxCellByHeight)));

        int boardPixelWidth = settings.boardWidth() * cellSize;
        int boardPixelHeight = settings.boardHeight() * cellSize;
        this.boardX = this.width / 2 - boardPixelWidth / 2;
        this.boardY = TOP_MARGIN + HudBar.reservedHeight();

        this.addRenderableWidget(new GameBoxButton(
                this.width / 2 - 50, this.height - 24, 100, 20,
                Component.translatable("gamebox.common.back"),
                this::onClose
        ));
    }

    private void refreshBestScore() {
        this.bestScoreAtStart = RecordManager
                .getRecord(SnakeGame.ID, difficulty.name(), SnakeRecord.class, new SnakeRecord(0, 0))
                .getHighScore();
    }

    private void startCountdown() {
        this.countdownTicksRemaining = COUNTDOWN_SECONDS * 20;
        this.tickCounter = 0;
        this.recordSaved = false;
        this.isNewRecord = false;
    }

    @Override
    public void tick() {
        super.tick();

        if (countdownTicksRemaining > 0) {
            countdownTicksRemaining--;
            return;
        }

        tickCounter++;
        if (tickCounter >= ticksPerMove) {
            tickCounter = 0;

            int scoreBefore = logic.getScore();
            logic.tick();

            if (logic.getScore() > scoreBefore) {
                GameBoxSounds.play(SoundEvents.EXPERIENCE_ORB_PICKUP);
            }

            if (logic.isGameOver() && !recordSaved) {
                recordSaved = true;
                saveRecord();
                if (isNewRecord) {
                    GameBoxSounds.play(SoundEvents.PLAYER_LEVELUP);
                } else {
                    GameBoxSounds.play(SoundEvents.ITEM_BREAK);
                }
            }
        }
    }

    private void saveRecord() {
        String difficultyKey = difficulty.name();
        SnakeRecord current = RecordManager.getRecord(SnakeGame.ID, difficultyKey, SnakeRecord.class, new SnakeRecord(0, 0));
        this.isNewRecord = logic.getScore() > current.getHighScore() && logic.getScore() > 0;
        int newHighScore = Math.max(current.getHighScore(), logic.getScore());
        int newGamesPlayed = current.getGamesPlayed() + 1;
        RecordManager.putRecord(SnakeGame.ID, difficultyKey, new SnakeRecord(newHighScore, newGamesPlayed));

        GameBoxLeaderboardClient.submitScore(SnakeGame.ID, difficultyKey, logic.getScore(), true);
        leaderboardPanel.refresh(SnakeGame.ID, difficultyKey);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_W || key == InputConstants.KEY_UP) {
            logic.changeDirection(Direction.UP);
            return true;
        }
        if (key == InputConstants.KEY_S || key == InputConstants.KEY_DOWN) {
            logic.changeDirection(Direction.DOWN);
            return true;
        }
        if (key == InputConstants.KEY_A || key == InputConstants.KEY_LEFT) {
            logic.changeDirection(Direction.LEFT);
            return true;
        }
        if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT) {
            logic.changeDirection(Direction.RIGHT);
            return true;
        }
        if (key == InputConstants.KEY_SPACE) {
            if (logic.isGameOver()) {
                logic.restart();
                refreshBestScore();
                startCountdown();
                leaderboardPanel.refresh(SnakeGame.ID, difficulty.name());
            } else {
                logic.togglePause();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int boardPixelWidth = logic.getBoardWidth() * cellSize;
        int boardPixelHeight = logic.getBoardHeight() * cellSize;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String scoreText = Component.translatable("gamebox.games.snake.score", logic.getScore()).getString();
            String bestText = Component.translatable("gamebox.games.snake.best", bestScoreAtStart).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelWidth, scoreText, bestText);
        }

        graphics.fill(boardX, boardY, boardX + boardPixelWidth, boardY + boardPixelHeight, BOARD_BACKGROUND);
        graphics.outline(boardX, boardY, boardPixelWidth, boardPixelHeight, BOARD_BORDER);

        for (GridPosition obstacle : logic.getObstacles()) {
            int x = boardX + obstacle.x() * cellSize;
            int y = boardY + obstacle.y() * cellSize;
            ItemIconRenderer.drawCentered(graphics, obstacleItemStack, x, y, cellSize);
        }

        GridPosition food = logic.getFood();
        int foodX = boardX + food.x() * cellSize;
        int foodY = boardY + food.y() * cellSize;
        ItemIconRenderer.drawCentered(graphics, foodItemStack, foodX, foodY, cellSize);

        List<GridPosition> body = logic.getBody();
        for (int i = body.size() - 1; i >= 1; i--) {
            drawSnakeCell(graphics, body.get(i), SNAKE_BODY_COLOR);
        }
        if (!body.isEmpty()) {
            drawSnakeHead(graphics, body.get(0), logic.getDirection());
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (countdownTicksRemaining > 0) {
            int secondsLeft = (int) Math.ceil(countdownTicksRemaining / 20.0);
            drawOverlay(graphics, boardPixelWidth, boardPixelHeight,
                    String.valueOf(secondsLeft),
                    Component.translatable("gamebox.games.snake.get_ready").getString(),
                    OVERLAY_TEXT_COLOR);
        } else if (logic.isGameOver()) {
            String title = isNewRecord
                    ? Component.translatable("gamebox.games.snake.new_record").getString()
                    : Component.translatable("gamebox.games.snake.game_over").getString();
            drawOverlay(graphics, boardPixelWidth, boardPixelHeight,
                    title,
                    Component.translatable("gamebox.games.snake.restart_hint").getString(),
                    isNewRecord ? NEW_RECORD_COLOR : OVERLAY_TEXT_COLOR);
        } else if (logic.isPaused()) {
            drawOverlay(graphics, boardPixelWidth, boardPixelHeight,
                    Component.translatable("gamebox.games.snake.paused").getString(),
                    Component.translatable("gamebox.games.snake.resume_hint").getString(),
                    OVERLAY_TEXT_COLOR);
        }
    }

    private void drawSnakeCell(GuiGraphicsExtractor graphics, GridPosition position, int color) {
        int x = boardX + position.x() * cellSize;
        int y = boardY + position.y() * cellSize;
        graphics.fill(x + 1, y + 1, x + cellSize - 1, y + cellSize - 1, color);
    }

    private void drawSnakeHead(GuiGraphicsExtractor graphics, GridPosition position, Direction direction) {
        int x = boardX + position.x() * cellSize;
        int y = boardY + position.y() * cellSize;
        graphics.fill(x + 1, y + 1, x + cellSize - 1, y + cellSize - 1, SNAKE_HEAD_COLOR);

        int noseSize = Math.max(2, cellSize / 5);
        int noseX;
        int noseY;

        switch (direction) {
            case UP -> {
                noseX = x + cellSize / 2 - noseSize / 2;
                noseY = y + 2;
            }
            case DOWN -> {
                noseX = x + cellSize / 2 - noseSize / 2;
                noseY = y + cellSize - 2 - noseSize;
            }
            case LEFT -> {
                noseX = x + 2;
                noseY = y + cellSize / 2 - noseSize / 2;
            }
            default -> {
                noseX = x + cellSize - 2 - noseSize;
                noseY = y + cellSize / 2 - noseSize / 2;
            }
        }

        graphics.fill(noseX, noseY, noseX + noseSize, noseY + noseSize, SNAKE_NOSE_COLOR);
    }

    private void drawOverlay(GuiGraphicsExtractor graphics, int boardPixelWidth, int boardPixelHeight, String title, String hint, int titleColor) {
        graphics.fill(boardX, boardY, boardX + boardPixelWidth, boardY + boardPixelHeight, OVERLAY_COLOR);

        int titleWidth = this.font.width(title);
        graphics.text(this.font, title, boardX + boardPixelWidth / 2 - titleWidth / 2,
                boardY + boardPixelHeight / 2 - 10, titleColor, false);

        int hintWidth = this.font.width(hint);
        graphics.text(this.font, hint, boardX + boardPixelWidth / 2 - hintWidth / 2,
                boardY + boardPixelHeight / 2 + 4, OVERLAY_TEXT_COLOR, false);
    }
}