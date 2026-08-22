package com.gamebox.client.games.game2048;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.GameBoxButton;
import com.gamebox.client.ui.HowToPlayScreen;
import com.gamebox.client.ui.HudBar;
import com.gamebox.client.ui.ServerLeaderboardPanel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Game2048Screen extends Screen {

    private static final int BOARD_SIZE = 4;
    private static final int MIN_CELL_SIZE = 20;
    private static final int MAX_CELL_SIZE = 70;
    private static final int TOP_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_MARGIN = 10;
    private static final int CELL_GAP = 4;
    private static final int BUTTON_GAP = 8;
    private static final int LEADERBOARD_GAP = 8;
    private static final int SLIDE_ANIMATION_TICKS = 4;

    private static final int EMPTY_CELL_COLOR_LIGHT = 0xFFDCE0E4;
    private static final int EMPTY_CELL_COLOR_DARK = 0xFF2B2F38;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;
    private static final int TEXT_DARK = 0xFF2B2E33;
    private static final int TEXT_LIGHT = 0xFFF9F6F2;

    private final Screen parentScreen;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private Game2048Logic logic;
    private int cellSize;
    private int boardX;
    private int boardY;
    private boolean recordSaved;
    private boolean isNewBestScore;
    private boolean reachedBannerShown;
    private int bestScoreAtStart;

    private List<Game2048Logic.TileMove> animatedMoves = List.of();
    private int animationTicksRemaining;

    public Game2048Screen(Screen parentScreen) {
        super(Component.translatable("gamebox.games.game2048.name"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        this.logic = new Game2048Logic();
        this.recordSaved = false;
        this.reachedBannerShown = false;
        this.animatedMoves = List.of();
        this.animationTicksRemaining = 0;
        refreshBestScore();

        leaderboardPanel.refresh(Game2048Game.ID, "career");

        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int availableHeight = this.height - TOP_MARGIN - HudBar.reservedHeight() - BOTTOM_MARGIN;
        int maxCellByWidth = (availableWidth - (BOARD_SIZE - 1) * CELL_GAP) / BOARD_SIZE;
        int maxCellByHeight = (availableHeight - (BOARD_SIZE - 1) * CELL_GAP) / BOARD_SIZE;
        this.cellSize = Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, Math.min(maxCellByWidth, maxCellByHeight)));

        int boardPixelSize = BOARD_SIZE * cellSize + (BOARD_SIZE - 1) * CELL_GAP;
        this.boardX = this.width / 2 - boardPixelSize / 2;
        this.boardY = TOP_MARGIN + HudBar.reservedHeight();

        int totalButtonsWidth = 90 + BUTTON_GAP + 90 + BUTTON_GAP + 90;
        int startX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);
        int buttonY = this.height - 24;

        this.addRenderableWidget(new GameBoxButton(
                startX, buttonY, 90, 20,
                Component.translatable("gamebox.common.how_to_play"),
                () -> this.minecraft.gui.setScreen(new HowToPlayScreen(this, this.getTitle(), getInstructions()))
        ));

        this.addRenderableWidget(new GameBoxButton(
                startX + 90 + BUTTON_GAP, buttonY, 90, 20,
                Component.translatable("gamebox.games.game2048.restart"),
                this::restart
        ));

        this.addRenderableWidget(new GameBoxButton(
                startX + 90 + BUTTON_GAP + 90 + BUTTON_GAP, buttonY, 90, 20,
                Component.translatable("gamebox.common.back"),
                this::onClose
        ));
    }

    private Component getInstructions() {
        return Component.translatable("gamebox.games.game2048.howto");
    }

    private void restart() {
        this.logic = new Game2048Logic();
        this.recordSaved = false;
        this.reachedBannerShown = false;
        this.animatedMoves = List.of();
        this.animationTicksRemaining = 0;
        refreshBestScore();
        leaderboardPanel.refresh(Game2048Game.ID, "career");
    }

    private void refreshBestScore() {
        this.bestScoreAtStart = RecordManager
                .getRecord(Game2048Game.ID, "career", Game2048Record.class, new Game2048Record(0, 0))
                .getBestScore();
    }

    @Override
    public void tick() {
        super.tick();
        if (animationTicksRemaining > 0) {
            animationTicksRemaining--;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (logic.isGameOver()) {
            return super.keyPressed(event);
        }

        int key = event.key();
        Game2048Logic.Direction direction = null;
        if (key == InputConstants.KEY_W || key == InputConstants.KEY_UP) {
            direction = Game2048Logic.Direction.UP;
        } else if (key == InputConstants.KEY_S || key == InputConstants.KEY_DOWN) {
            direction = Game2048Logic.Direction.DOWN;
        } else if (key == InputConstants.KEY_A || key == InputConstants.KEY_LEFT) {
            direction = Game2048Logic.Direction.LEFT;
        } else if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT) {
            direction = Game2048Logic.Direction.RIGHT;
        }

        if (direction == null) {
            return super.keyPressed(event);
        }

        boolean moved = logic.move(direction);
        if (moved) {
            this.animatedMoves = logic.getLastMoves();
            this.animationTicksRemaining = SLIDE_ANIMATION_TICKS;

            GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.2F);

            if (!reachedBannerShown && logic.hasReached2048()) {
                reachedBannerShown = true;
                GameBoxSounds.play(SoundEvents.PLAYER_LEVELUP);
            }

            if (logic.isGameOver() && !recordSaved) {
                recordSaved = true;
                saveRecord();
                GameBoxSounds.play(SoundEvents.ITEM_BREAK);
            }
        }
        return true;
    }

    private void saveRecord() {
        Game2048Record current = RecordManager.getRecord(Game2048Game.ID, "career", Game2048Record.class, new Game2048Record(0, 0));
        this.isNewBestScore = logic.getScore() > current.getBestScore();
        int newBestScore = Math.max(current.getBestScore(), logic.getScore());
        int newGamesPlayed = current.getGamesPlayed() + 1;
        RecordManager.putRecord(Game2048Game.ID, "career", new Game2048Record(newBestScore, newGamesPlayed));

        GameBoxLeaderboardClient.submitScore(Game2048Game.ID, "career", logic.getScore(), true);
        leaderboardPanel.refresh(Game2048Game.ID, "career");
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int boardPixelSize = BOARD_SIZE * cellSize + (BOARD_SIZE - 1) * CELL_GAP;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String scoreText = Component.translatable("gamebox.games.game2048.score", logic.getScore()).getString();
            String bestText = Component.translatable("gamebox.games.game2048.best", bestScoreAtStart).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelSize, scoreText, bestText);
        }

        boolean animating = animationTicksRemaining > 0 && !animatedMoves.isEmpty();
        Set<Long> suppressedDestinations = new HashSet<>();
        if (animating) {
            for (Game2048Logic.TileMove move : animatedMoves) {
                suppressedDestinations.add(cellKey(move.toRow(), move.toCol()));
            }
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boolean suppressed = animating && suppressedDestinations.contains(cellKey(row, col));
                drawCell(graphics, row, col, suppressed);
            }
        }

        if (animating) {
            float progress = 1.0F - (animationTicksRemaining - delta) / (float) SLIDE_ANIMATION_TICKS;
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            for (Game2048Logic.TileMove move : animatedMoves) {
                drawSlidingTile(graphics, move, progress);
            }
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (logic.isGameOver()) {
            graphics.fill(boardX, boardY, boardX + boardPixelSize, boardY + boardPixelSize, OVERLAY_COLOR);

            String title = isNewBestScore
                    ? Component.translatable("gamebox.games.game2048.new_best").getString()
                    : Component.translatable("gamebox.games.game2048.game_over").getString();
            int titleWidth = this.font.width(title);
            graphics.text(this.font, title, boardX + boardPixelSize / 2 - titleWidth / 2,
                    boardY + boardPixelSize / 2 - 10, isNewBestScore ? VICTORY_COLOR : OVERLAY_TEXT_COLOR, false);

            String hint = Component.translatable("gamebox.games.game2048.restart_hint").getString();
            int hintWidth = this.font.width(hint);
            graphics.text(this.font, hint, boardX + boardPixelSize / 2 - hintWidth / 2,
                    boardY + boardPixelSize / 2 + 4, OVERLAY_TEXT_COLOR, false);
        }
    }

    private long cellKey(int row, int col) {
        return row * 100L + col;
    }

    private int cellPixelX(int col) {
        return boardX + col * (cellSize + CELL_GAP);
    }

    private int cellPixelY(int row) {
        return boardY + row * (cellSize + CELL_GAP);
    }

    private void drawCell(GuiGraphicsExtractor graphics, int row, int col, boolean suppressed) {
        int cellX = cellPixelX(col);
        int cellY = cellPixelY(row);
        int value = suppressed ? 0 : logic.getValue(row, col);

        if (value == 0) {
            int emptyColor = GameBoxConfig.get().isDarkTheme() ? EMPTY_CELL_COLOR_DARK : EMPTY_CELL_COLOR_LIGHT;
            graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, emptyColor);
            graphics.outline(cellX, cellY, cellSize, cellSize, GameBoxTheme.cellBorder());
            return;
        }

        drawTile(graphics, cellX, cellY, cellSize, value);
    }

    private void drawSlidingTile(GuiGraphicsExtractor graphics, Game2048Logic.TileMove move, float progress) {
        int fromX = cellPixelX(move.fromCol());
        int fromY = cellPixelY(move.fromRow());
        int toX = cellPixelX(move.toCol());
        int toY = cellPixelY(move.toRow());

        int currentX = Math.round(fromX + (toX - fromX) * progress);
        int currentY = Math.round(fromY + (toY - fromY) * progress);

        drawTile(graphics, currentX, currentY, cellSize, move.displayValue());
    }

    private void drawTile(GuiGraphicsExtractor graphics, int x, int y, int size, int value) {
        graphics.fill(x, y, x + size, y + size, tileColor(value));
        graphics.outline(x, y, size, size, GameBoxTheme.cellBorder());

        String text = String.valueOf(value);
        int textWidth = this.font.width(text);
        graphics.text(this.font, text,
                x + size / 2 - textWidth / 2,
                y + size / 2 - 4,
                value <= 4 ? TEXT_DARK : TEXT_LIGHT, false);
    }

    private int tileColor(int value) {
        return switch (value) {
            case 2 -> 0xFFEEE4DA;
            case 4 -> 0xFFEDE0C8;
            case 8 -> 0xFFF2B179;
            case 16 -> 0xFFF59563;
            case 32 -> 0xFFF67C5F;
            case 64 -> 0xFFF65E3B;
            case 128 -> 0xFFEDCF72;
            case 256 -> 0xFFEDCC61;
            case 512 -> 0xFFEDC850;
            case 1024 -> 0xFFEDC53F;
            case 2048 -> 0xFFEDC22E;
            default -> 0xFF3C3A32;
        };
    }
}