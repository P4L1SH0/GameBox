package com.gamebox.client.games.game1010;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.GameBoxButton;
import com.gamebox.client.ui.HowToPlayScreen;
import com.gamebox.client.ui.HudBar;
import com.gamebox.client.ui.ServerLeaderboardPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class Game1010Screen extends Screen {

    private static final int BOARD_SIZE = Game1010Logic.BOARD_SIZE;
    private static final int MIN_CELL_SIZE = 10;
    private static final int MAX_CELL_SIZE = 32;
    private static final int TOP_MARGIN = 6;
    private static final int SIDE_MARGIN = 10;
    private static final int SECTION_GAP = 8;
    private static final int BOTTOM_MARGIN = 10;
    private static final int BUTTON_ROW_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int LEADERBOARD_GAP = 8;

    private static final int PREVIEW_BOX_SIZE = 64;
    private static final int PREVIEW_BOX_GAP = 8;
    private static final int PREVIEW_CELL_SIZE = 12;

    private static final int CELL_BACKGROUND_LIGHT = 0xFFDCE0E4;
    private static final int CELL_BACKGROUND_DARK = 0xFF2B2F38;
    private static final int PREVIEW_BOX_BACKGROUND_LIGHT = 0xFFF4F5F7;
    private static final int PREVIEW_BOX_BACKGROUND_DARK = 0xFF23262E;
    private static final int PREVIEW_BOX_EMPTY_LIGHT = 0xFFDCE0E4;
    private static final int PREVIEW_BOX_EMPTY_DARK = 0xFF2B2F38;
    private static final int PREVIEW_BOX_SELECTED_BORDER = 0xFFFFD86B;
    private static final int PLACEMENT_VALID_OVERLAY = 0x8039FF6A;
    private static final int PLACEMENT_INVALID_OVERLAY = 0x80FF4C3B;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;

    private final Screen parentScreen;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private Game1010Logic logic;
    private int cellSize;
    private int boardX;
    private int boardY;
    private int previewRowX;
    private int previewRowY;
    private int lastMouseX;
    private int lastMouseY;
    private int selectedSlot = -1;

    private int grabRow;
    private int grabCol;

    private boolean recordSaved;
    private boolean isNewBestScore;
    private int bestScoreAtStart;

    public Game1010Screen(Screen parentScreen) {
        super(Component.translatable("gamebox.games.game1010.name"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        this.logic = new Game1010Logic();
        this.selectedSlot = -1;
        this.grabRow = 0;
        this.grabCol = 0;
        this.recordSaved = false;
        refreshBestScore();

        leaderboardPanel.refresh(Game1010Game.ID, "career");

        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int reservedHeight = TOP_MARGIN + HudBar.reservedHeight() + SECTION_GAP
                + PREVIEW_BOX_SIZE + SECTION_GAP + BUTTON_ROW_HEIGHT + BOTTOM_MARGIN;
        int availableHeightForBoard = this.height - reservedHeight;

        int maxCellByWidth = availableWidth / BOARD_SIZE;
        int maxCellByHeight = availableHeightForBoard / BOARD_SIZE;
        this.cellSize = Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, Math.min(maxCellByWidth, maxCellByHeight)));

        int boardPixelSize = BOARD_SIZE * cellSize;
        this.boardX = this.width / 2 - boardPixelSize / 2;
        this.boardY = TOP_MARGIN + HudBar.reservedHeight();

        int previewRowWidth = Game1010Logic.PIECE_SLOTS * PREVIEW_BOX_SIZE + (Game1010Logic.PIECE_SLOTS - 1) * PREVIEW_BOX_GAP;
        this.previewRowX = this.width / 2 - previewRowWidth / 2;
        this.previewRowY = boardY + boardPixelSize + SECTION_GAP;

        int buttonY = this.previewRowY + PREVIEW_BOX_SIZE + SECTION_GAP;
        int totalButtonsWidth = 90 + BUTTON_GAP + 90 + BUTTON_GAP + 90;
        int buttonStartX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);

        this.addRenderableWidget(new GameBoxButton(buttonStartX, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.common.how_to_play"),
                () -> this.minecraft.setScreen(new HowToPlayScreen(this, this.getTitle(), getInstructions()))));

        this.addRenderableWidget(new GameBoxButton(buttonStartX + 90 + BUTTON_GAP, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.games.game1010.restart"), this::restart));

        this.addRenderableWidget(new GameBoxButton(buttonStartX + 90 + BUTTON_GAP + 90 + BUTTON_GAP, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.common.back"), this::onClose));
    }

    private Component getInstructions() {
        return Component.translatable("gamebox.games.game1010.howto");
    }

    private void restart() {
        this.logic.restart();
        this.selectedSlot = -1;
        this.grabRow = 0;
        this.grabCol = 0;
        this.recordSaved = false;
        refreshBestScore();
        leaderboardPanel.refresh(Game1010Game.ID, "career");
    }

    private void refreshBestScore() {
        this.bestScoreAtStart = RecordManager
                .getRecord(Game1010Game.ID, "career", Game1010Record.class, new Game1010Record(0, 0))
                .getBestScore();
    }

    private void saveRecordIfNeeded() {
        if (recordSaved) {
            return;
        }
        recordSaved = true;

        Game1010Record current = RecordManager.getRecord(Game1010Game.ID, "career", Game1010Record.class, new Game1010Record(0, 0));
        this.isNewBestScore = logic.getScore() > current.getBestScore();
        int newBestScore = Math.max(current.getBestScore(), logic.getScore());
        int newGamesPlayed = current.getGamesPlayed() + 1;
        RecordManager.putRecord(Game1010Game.ID, "career", new Game1010Record(newBestScore, newGamesPlayed));

        GameBoxLeaderboardClient.submitScore(Game1010Game.ID, "career", logic.getScore(), true);
        leaderboardPanel.refresh(Game1010Game.ID, "career");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (logic.isGameOver()) {
            return false;
        }

        int x = (int) event.x();
        int y = (int) event.y();

        for (int slot = 0; slot < Game1010Logic.PIECE_SLOTS; slot++) {
            int boxX = previewRowX + slot * (PREVIEW_BOX_SIZE + PREVIEW_BOX_GAP);
            if (withinBounds(x, y, boxX, previewRowY, PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE)) {
                if (logic.getPiece(slot) != null) {
                    selectedSlot = slot;
                    int[] center = computeCenterGrab(logic.getPiece(slot));
                    grabRow = center[0];
                    grabCol = center[1];
                    GameBoxSounds.play(SoundEvents.UI_BUTTON_CLICK, 1.3F);
                }
                return true;
            }
        }

        if (selectedSlot != -1) {
            int[] origin = computeBoardOrigin(x, y);
            if (origin != null && logic.canPlace(selectedSlot, origin[0], origin[1])) {
                int cellCountPlaced = logic.getPiece(selectedSlot).cells().length;
                int scoreBefore = logic.getScore();
                logic.placePiece(selectedSlot, origin[0], origin[1]);
                selectedSlot = -1;

                boolean linesCleared = logic.getScore() > scoreBefore + cellCountPlaced;
                if (linesCleared) {
                    GameBoxSounds.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.1F);
                } else {
                    GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.1F);
                }

                if (logic.isGameOver()) {
                    GameBoxSounds.play(SoundEvents.ITEM_BREAK);
                    saveRecordIfNeeded();
                }
                return true;
            }
        }

        return false;
    }

    private int[] computeCenterGrab(Game1010Logic.Piece piece) {
        int centerRow = Math.round((piece.height() - 1) / 2.0F);
        int centerCol = Math.round((piece.width() - 1) / 2.0F);
        return new int[]{centerRow, centerCol};
    }

    private int[] computeBoardOrigin(int mouseX, int mouseY) {
        int localX = mouseX - boardX;
        int localY = mouseY - boardY;
        if (localX < 0 || localY < 0) {
            return null;
        }
        int hoverCol = localX / cellSize;
        int hoverRow = localY / cellSize;
        if (hoverRow >= BOARD_SIZE || hoverCol >= BOARD_SIZE) {
            return null;
        }
        return new int[]{hoverRow - grabRow, hoverCol - grabCol};
    }

    private boolean withinBounds(int x, int y, int boxX, int boxY, int boxWidth, int boxHeight) {
        return x >= boxX && x < boxX + boxWidth && y >= boxY && y < boxY + boxHeight;
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

        int boardPixelSize = BOARD_SIZE * cellSize;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String scoreText = Component.translatable("gamebox.games.game1010.score", logic.getScore()).getString();
            String bestText = Component.translatable("gamebox.games.game1010.best", bestScoreAtStart).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelSize, scoreText, bestText);
        }

        drawBoard(graphics);
        drawPlacementPreview(graphics);
        drawPiecePreviews(graphics);

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (logic.isGameOver()) {
            drawGameOverOverlay(graphics, boardPixelSize);
        }
    }

    private void drawBoard(GuiGraphicsExtractor graphics) {
        boolean dark = GameBoxConfig.get().isDarkTheme();
        int emptyColor = dark ? CELL_BACKGROUND_DARK : CELL_BACKGROUND_LIGHT;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int cellX = boardX + col * cellSize;
                int cellY = boardY + row * cellSize;
                int color = logic.isCellFilled(row, col) ? logic.getCellColor(row, col) : emptyColor;
                graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, color);
                graphics.outline(cellX, cellY, cellSize, cellSize, GameBoxTheme.cellBorder());
            }
        }
    }

    private void drawPlacementPreview(GuiGraphicsExtractor graphics) {
        if (selectedSlot == -1 || logic.getPiece(selectedSlot) == null) {
            return;
        }

        int[] origin = computeBoardOrigin(lastMouseX, lastMouseY);
        if (origin == null) {
            return;
        }
        int row = origin[0];
        int col = origin[1];

        boolean valid = logic.canPlace(selectedSlot, row, col);
        int overlayColor = valid ? PLACEMENT_VALID_OVERLAY : PLACEMENT_INVALID_OVERLAY;

        for (int[] cell : logic.getPiece(selectedSlot).cells()) {
            int r = row + cell[0];
            int c = col + cell[1];
            if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                continue;
            }
            int cellX = boardX + c * cellSize;
            int cellY = boardY + r * cellSize;
            graphics.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, overlayColor);
        }
    }

    private void drawPiecePreviews(GuiGraphicsExtractor graphics) {
        boolean dark = GameBoxConfig.get().isDarkTheme();
        int boxBackground = dark ? PREVIEW_BOX_BACKGROUND_DARK : PREVIEW_BOX_BACKGROUND_LIGHT;
        int emptyBoxBackground = dark ? PREVIEW_BOX_EMPTY_DARK : PREVIEW_BOX_EMPTY_LIGHT;

        for (int slot = 0; slot < Game1010Logic.PIECE_SLOTS; slot++) {
            int boxX = previewRowX + slot * (PREVIEW_BOX_SIZE + PREVIEW_BOX_GAP);
            Game1010Logic.Piece piece = logic.getPiece(slot);

            graphics.fill(boxX, previewRowY, boxX + PREVIEW_BOX_SIZE, previewRowY + PREVIEW_BOX_SIZE,
                    piece == null ? emptyBoxBackground : boxBackground);

            int borderColor = (slot == selectedSlot) ? PREVIEW_BOX_SELECTED_BORDER : GameBoxTheme.cellBorder();
            graphics.outline(boxX, previewRowY, PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE, borderColor);

            if (piece != null) {
                int pieceWidth = piece.width() * PREVIEW_CELL_SIZE;
                int pieceHeight = piece.height() * PREVIEW_CELL_SIZE;
                int originX = boxX + (PREVIEW_BOX_SIZE - pieceWidth) / 2;
                int originY = previewRowY + (PREVIEW_BOX_SIZE - pieceHeight) / 2;

                for (int[] cell : piece.cells()) {
                    int cellX = originX + cell[1] * PREVIEW_CELL_SIZE;
                    int cellY = originY + cell[0] * PREVIEW_CELL_SIZE;
                    graphics.fill(cellX + 1, cellY + 1, cellX + PREVIEW_CELL_SIZE - 1, cellY + PREVIEW_CELL_SIZE - 1, piece.color());
                    graphics.outline(cellX, cellY, PREVIEW_CELL_SIZE, PREVIEW_CELL_SIZE, GameBoxTheme.cellBorder());
                }
            }
        }
    }

    private void drawGameOverOverlay(GuiGraphicsExtractor graphics, int boardPixelSize) {
        graphics.fill(boardX, boardY, boardX + boardPixelSize, boardY + boardPixelSize, OVERLAY_COLOR);

        String title = isNewBestScore
                ? Component.translatable("gamebox.games.game1010.new_best").getString()
                : Component.translatable("gamebox.games.game1010.game_over").getString();
        int titleWidth = this.font.width(title);
        graphics.text(this.font, title, boardX + boardPixelSize / 2 - titleWidth / 2,
                boardY + boardPixelSize / 2 - 10, isNewBestScore ? VICTORY_COLOR : OVERLAY_TEXT_COLOR, false);

        String hint = Component.translatable("gamebox.games.game1010.restart_hint").getString();
        int hintWidth = this.font.width(hint);
        graphics.text(this.font, hint, boardX + boardPixelSize / 2 - hintWidth / 2,
                boardY + boardPixelSize / 2 + 4, OVERLAY_TEXT_COLOR, false);
    }
}