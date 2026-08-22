package com.gamebox.client.games.lightsout;

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
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public class LightsOutScreen extends Screen {

    private static final int MIN_CELL_SIZE = 8;
    private static final int MAX_CELL_SIZE = 50;
    private static final int TOP_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_MARGIN = 10;
    private static final int BUTTON_GAP = 10;
    private static final int LEADERBOARD_GAP = 8;

    private static final Identifier LAMP_ON_TEXTURE = Identifier.withDefaultNamespace("textures/block/redstone_lamp_on.png");
    private static final Identifier LAMP_OFF_TEXTURE = Identifier.withDefaultNamespace("textures/block/redstone_lamp.png");

    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;

    private final Screen parentScreen;
    private final Difficulty difficulty;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private LightsOutLogic logic;
    private int cellSize;
    private int boardX;
    private int boardY;
    private int elapsedTicks;
    private boolean recordSaved;
    private boolean isNewBestMoves;
    private boolean isNewBestTime;
    private int lastMouseX;
    private int lastMouseY;

    public LightsOutScreen(Screen parentScreen, Difficulty difficulty) {
        super(Component.translatable("gamebox.games.lights_out.name"));
        this.parentScreen = parentScreen;
        this.difficulty = difficulty;
    }

    @Override
    protected void init() {
        LightsOutDifficultySettings settings = LightsOutDifficultySettings.forDifficulty(difficulty);
        this.logic = new LightsOutLogic(settings);
        this.elapsedTicks = 0;
        this.recordSaved = false;

        leaderboardPanel.refresh(LightsOutGame.ID, difficulty.name());

        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int availableHeight = this.height - TOP_MARGIN - HudBar.reservedHeight() - BOTTOM_MARGIN;
        int maxCellByWidth = availableWidth / settings.boardSize();
        int maxCellByHeight = availableHeight / settings.boardSize();
        this.cellSize = Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, Math.min(maxCellByWidth, maxCellByHeight)));

        int boardPixelSize = settings.boardSize() * cellSize;
        this.boardX = this.width / 2 - boardPixelSize / 2;
        this.boardY = TOP_MARGIN + HudBar.reservedHeight();

        int totalButtonsWidth = 100 + BUTTON_GAP + 100;
        int startX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);

        this.addRenderableWidget(new GameBoxButton(
                startX, this.height - 24, 100, 20,
                Component.translatable("gamebox.games.lights_out.restart"),
                () -> {
                    logic.restart();
                    elapsedTicks = 0;
                    recordSaved = false;
                    leaderboardPanel.refresh(LightsOutGame.ID, difficulty.name());
                }
        ));

        this.addRenderableWidget(new GameBoxButton(
                startX + 100 + BUTTON_GAP, this.height - 24, 100, 20,
                Component.translatable("gamebox.common.back"),
                this::onClose
        ));
    }

    @Override
    public void tick() {
        super.tick();
        if (!logic.isSolved()) {
            elapsedTicks++;
        } else if (!recordSaved) {
            recordSaved = true;
            saveRecord();
            GameBoxSounds.play(SoundEvents.PLAYER_LEVELUP);
        }
    }

    private void saveRecord() {
        String difficultyKey = difficulty.name();
        LightsOutRecord current = RecordManager.getRecord(
                LightsOutGame.ID, difficultyKey, LightsOutRecord.class, new LightsOutRecord(0, 0, 0));

        long timeMs = elapsedTicks * 50L;
        int moves = logic.getMoveCount();
        boolean firstEver = current.getGamesCompleted() == 0;

        this.isNewBestMoves = firstEver || moves < current.getBestMoves();
        this.isNewBestTime = firstEver || timeMs < current.getBestTimeMs();

        int newBestMoves = firstEver ? moves : Math.min(current.getBestMoves(), moves);
        long newBestTime = firstEver ? timeMs : Math.min(current.getBestTimeMs(), timeMs);
        int newGamesCompleted = current.getGamesCompleted() + 1;

        RecordManager.putRecord(LightsOutGame.ID, difficultyKey,
                new LightsOutRecord(newBestMoves, newBestTime, newGamesCompleted));

        GameBoxLeaderboardClient.submitScore(LightsOutGame.ID, difficultyKey, moves, false);
        leaderboardPanel.refresh(LightsOutGame.ID, difficultyKey);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (logic.isSolved()) {
            return false;
        }

        int localX = lastMouseX - boardX;
        int localY = lastMouseY - boardY;
        if (localX < 0 || localY < 0) {
            return false;
        }
        int cellX = localX / cellSize;
        int cellY = localY / cellSize;
        if (cellX >= logic.getBoardSize() || cellY >= logic.getBoardSize()) {
            return false;
        }

        logic.click(cellX, cellY);
        if (!logic.isSolved()) {
            GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.4F);
        }
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int size = logic.getBoardSize();
        int boardPixelSize = size * cellSize;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String movesText = Component.translatable("gamebox.games.lights_out.moves", logic.getMoveCount()).getString();
            String timeText = Component.translatable("gamebox.games.lights_out.time", formatElapsed()).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelSize, movesText, timeText);
        }

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                drawCell(graphics, x, y);
            }
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (logic.isSolved()) {
            graphics.fill(boardX, boardY, boardX + boardPixelSize, boardY + boardPixelSize, OVERLAY_COLOR);

            String title = Component.translatable("gamebox.games.lights_out.solved").getString();
            int titleWidth = this.font.width(title);
            graphics.text(this.font, title, boardX + boardPixelSize / 2 - titleWidth / 2,
                    boardY + boardPixelSize / 2 - 10, VICTORY_COLOR, false);

            String detail = (isNewBestMoves || isNewBestTime)
                    ? Component.translatable("gamebox.games.lights_out.new_best").getString()
                    : Component.translatable("gamebox.games.lights_out.solved_detail", logic.getMoveCount(), formatElapsed()).getString();
            int detailWidth = this.font.width(detail);
            graphics.text(this.font, detail, boardX + boardPixelSize / 2 - detailWidth / 2,
                    boardY + boardPixelSize / 2 + 4, OVERLAY_TEXT_COLOR, false);
        }
    }

    private void drawCell(GuiGraphicsExtractor graphics, int x, int y) {
        int cellX = boardX + x * cellSize;
        int cellY = boardY + y * cellSize;

        Identifier texture = logic.isLightOn(x, y) ? LAMP_ON_TEXTURE : LAMP_OFF_TEXTURE;
        graphics.blit(texture,
                cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1,
                0.0F, 1.0F, 0.0F, 1.0F);

        graphics.outline(cellX, cellY, cellSize, cellSize, GameBoxTheme.cellBorder());
    }

    private String formatElapsed() {
        int totalSeconds = elapsedTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}