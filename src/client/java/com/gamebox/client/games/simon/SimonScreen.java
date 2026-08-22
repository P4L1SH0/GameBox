package com.gamebox.client.games.simon;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.core.Difficulty;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.GameBoxButton;
import com.gamebox.client.ui.HudBar;
import com.gamebox.client.ui.ServerLeaderboardPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class SimonScreen extends Screen {

    private enum Phase {
        INITIAL_WAIT, SHOW_COLOR, SHOW_GAP, WAITING_INPUT, INPUT_FEEDBACK, GAME_OVER
    }

    private static final int GRID_SIZE = 2;
    private static final int MIN_CELL_SIZE = 40;
    private static final int MAX_CELL_SIZE = 120;
    private static final int CELL_GAP = 6;
    private static final int TOP_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_MARGIN = 10;
    private static final int INPUT_FEEDBACK_TICKS = 4;
    private static final int INITIAL_WAIT_TICKS = 16;
    private static final int LEADERBOARD_GAP = 8;

    private static final int[] BASE_COLOR = {0xFF1D8348, 0xFFB03A2E, 0xFFB7950B, 0xFF1F618D};
    private static final int[] ACTIVE_COLOR = {0xFF39FF6A, 0xFFFF4C3B, 0xFFFFE135, 0xFF3DA8FF};
    private static final float[] COLOR_PITCH = {0.7F, 0.9F, 1.1F, 1.4F};
    private static final float SOUND_VOLUME_BOOST = 2.5F;

    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;

    private final Screen parentScreen;
    private final Difficulty difficulty;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private SimonLogic logic;
    private int cellSize;
    private int boardX;
    private int boardY;
    private int ticksPerFlash;
    private int ticksPerGap;

    private Phase phase;
    private int phaseTicks;
    private int playbackIndex;
    private int activeColor = -1;
    private boolean pendingPlaybackRestart;
    private boolean recordSaved;
    private boolean isNewBestScore;
    private int bestScoreAtStart;

    public SimonScreen(Screen parentScreen, Difficulty difficulty) {
        super(Component.translatable("gamebox.games.simon.name"));
        this.parentScreen = parentScreen;
        this.difficulty = difficulty;
    }

    @Override
    protected void init() {
        SimonDifficultySettings settings = SimonDifficultySettings.forDifficulty(difficulty);
        this.logic = new SimonLogic(settings);
        this.ticksPerFlash = Math.max(1, settings.flashDurationMs() / 50);
        this.ticksPerGap = Math.max(1, settings.pauseBetweenMs() / 50);
        this.recordSaved = false;
        refreshBestScore();

        leaderboardPanel.refresh(SimonGame.ID, difficulty.name());

        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int availableHeight = this.height - TOP_MARGIN - HudBar.reservedHeight() - BOTTOM_MARGIN;
        int maxCellByWidth = (availableWidth - (GRID_SIZE - 1) * CELL_GAP) / GRID_SIZE;
        int maxCellByHeight = (availableHeight - (GRID_SIZE - 1) * CELL_GAP) / GRID_SIZE;
        this.cellSize = Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, Math.min(maxCellByWidth, maxCellByHeight)));

        int boardPixelSize = GRID_SIZE * cellSize + (GRID_SIZE - 1) * CELL_GAP;
        this.boardX = this.width / 2 - boardPixelSize / 2;
        this.boardY = TOP_MARGIN + HudBar.reservedHeight();

        this.addRenderableWidget(new GameBoxButton(
                this.width / 2 - 50, this.height - 24, 100, 20,
                Component.translatable("gamebox.common.back"),
                this::onClose
        ));

        startPlayback();
    }

    private void refreshBestScore() {
        this.bestScoreAtStart = RecordManager
                .getRecord(SimonGame.ID, difficulty.name(), SimonRecord.class, new SimonRecord(0, 0))
                .getBestScore();
    }

    private void startPlayback() {
        this.playbackIndex = 0;
        this.activeColor = -1;
        this.phase = Phase.INITIAL_WAIT;
        this.phaseTicks = INITIAL_WAIT_TICKS;
    }

    private void showPlaybackColor(int colorIndex) {
        this.activeColor = colorIndex;
        this.phase = Phase.SHOW_COLOR;
        this.phaseTicks = ticksPerFlash;
        GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HARP, COLOR_PITCH[colorIndex], SOUND_VOLUME_BOOST);
    }

    @Override
    public void tick() {
        super.tick();
        if (phase == Phase.WAITING_INPUT || phase == Phase.GAME_OVER) {
            return;
        }
        if (phaseTicks > 0) {
            phaseTicks--;
            return;
        }

        switch (phase) {
            case INITIAL_WAIT -> showPlaybackColor(logic.getSequence().get(0));
            case SHOW_COLOR -> {
                activeColor = -1;
                phase = Phase.SHOW_GAP;
                phaseTicks = ticksPerGap;
            }
            case SHOW_GAP -> {
                playbackIndex++;
                if (playbackIndex >= logic.getSequence().size()) {
                    phase = Phase.WAITING_INPUT;
                    activeColor = -1;
                } else {
                    showPlaybackColor(logic.getSequence().get(playbackIndex));
                }
            }
            case INPUT_FEEDBACK -> {
                activeColor = -1;
                if (logic.isGameOver()) {
                    phase = Phase.GAME_OVER;
                    saveRecordIfNeeded();
                } else if (pendingPlaybackRestart) {
                    pendingPlaybackRestart = false;
                    startPlayback();
                } else {
                    phase = Phase.WAITING_INPUT;
                }
            }
            default -> {
            }
        }
    }

    private void saveRecordIfNeeded() {
        if (recordSaved) {
            return;
        }
        recordSaved = true;

        String difficultyKey = difficulty.name();
        SimonRecord current = RecordManager.getRecord(SimonGame.ID, difficultyKey, SimonRecord.class, new SimonRecord(0, 0));
        this.isNewBestScore = logic.getScore() > current.getBestScore();
        int newBestScore = Math.max(current.getBestScore(), logic.getScore());
        int newGamesPlayed = current.getGamesPlayed() + 1;
        RecordManager.putRecord(SimonGame.ID, difficultyKey, new SimonRecord(newBestScore, newGamesPlayed));

        GameBoxLeaderboardClient.submitScore(SimonGame.ID, difficultyKey, logic.getScore(), true);
        leaderboardPanel.refresh(SimonGame.ID, difficultyKey);

        GameBoxSounds.play(SoundEvents.ITEM_BREAK);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (phase == Phase.GAME_OVER) {
            return handleGameOverClick();
        }
        if (phase != Phase.WAITING_INPUT) {
            return false;
        }

        int localX = (int) event.x() - boardX;
        int localY = (int) event.y() - boardY;
        if (localX < 0 || localY < 0) {
            return false;
        }
        int col = localX / (cellSize + CELL_GAP);
        int row = localY / (cellSize + CELL_GAP);
        if (col >= GRID_SIZE || row >= GRID_SIZE) {
            return false;
        }

        int colorIndex = row * GRID_SIZE + col;
        int sizeBefore = logic.getSequence().size();
        logic.playerInput(colorIndex);

        activeColor = colorIndex;
        phase = Phase.INPUT_FEEDBACK;
        phaseTicks = INPUT_FEEDBACK_TICKS;
        pendingPlaybackRestart = !logic.isGameOver() && logic.getSequence().size() > sizeBefore;

        if (!logic.isGameOver()) {
            GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HARP, COLOR_PITCH[colorIndex], SOUND_VOLUME_BOOST);
        }
        return true;
    }

    private boolean handleGameOverClick() {
        this.logic.restart();
        this.recordSaved = false;
        refreshBestScore();
        leaderboardPanel.refresh(SimonGame.ID, difficulty.name());
        startPlayback();
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int boardPixelSize = GRID_SIZE * cellSize + (GRID_SIZE - 1) * CELL_GAP;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String scoreText = Component.translatable("gamebox.games.simon.score", logic.getScore()).getString();
            String bestText = Component.translatable("gamebox.games.simon.best", bestScoreAtStart).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelSize, scoreText, bestText);
        }

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                drawCell(graphics, row, col);
            }
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (phase == Phase.WAITING_INPUT) {
            String hint = Component.translatable("gamebox.games.simon.your_turn").getString();
            int hintWidth = this.font.width(hint);
            graphics.text(this.font, hint, boardX + boardPixelSize / 2 - hintWidth / 2,
                    boardY + boardPixelSize + 6, GameBoxTheme.lineColor(), false);
        }

        if (phase == Phase.GAME_OVER) {
            graphics.fill(boardX, boardY, boardX + boardPixelSize, boardY + boardPixelSize, OVERLAY_COLOR);

            String title = isNewBestScore
                    ? Component.translatable("gamebox.games.simon.new_best").getString()
                    : Component.translatable("gamebox.games.simon.game_over").getString();
            int titleWidth = this.font.width(title);
            graphics.text(this.font, title, boardX + boardPixelSize / 2 - titleWidth / 2,
                    boardY + boardPixelSize / 2 - 10, isNewBestScore ? VICTORY_COLOR : OVERLAY_TEXT_COLOR, false);

            String hint = Component.translatable("gamebox.games.simon.restart_hint").getString();
            int hintWidth = this.font.width(hint);
            graphics.text(this.font, hint, boardX + boardPixelSize / 2 - hintWidth / 2,
                    boardY + boardPixelSize / 2 + 4, OVERLAY_TEXT_COLOR, false);
        }
    }

    private void drawCell(GuiGraphicsExtractor graphics, int row, int col) {
        int index = row * GRID_SIZE + col;
        int cellX = boardX + col * (cellSize + CELL_GAP);
        int cellY = boardY + row * (cellSize + CELL_GAP);

        int color = (activeColor == index) ? ACTIVE_COLOR[index] : BASE_COLOR[index];
        graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, color);
        graphics.outline(cellX, cellY, cellSize, cellSize, GameBoxTheme.cellBorder());
    }
}