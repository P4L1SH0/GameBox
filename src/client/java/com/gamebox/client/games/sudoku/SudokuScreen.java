package com.gamebox.client.games.sudoku;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.core.Difficulty;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.GameBoxButton;
import com.gamebox.client.ui.HudBar;
import com.gamebox.client.ui.ServerLeaderboardPanel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class SudokuScreen extends Screen {

    private static final int BOARD_SIZE = 9;
    private static final int MIN_CELL_SIZE = 10;
    private static final int MAX_CELL_SIZE = 46;
    private static final int MIN_CELL_SIZE_FOR_NOTES = 24;
    private static final int TOP_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_MARGIN = 10;
    private static final int BOX_BORDER_THICKNESS = 2;
    private static final int BUTTON_GAP = 8;
    private static final int LEADERBOARD_GAP = 8;

    private static final int PEER_HIGHLIGHT_COLOR = 0x401A56DB;
    private static final int SAME_VALUE_HIGHLIGHT_COLOR = 0x552D6A4F;
    private static final int CONFLICT_TEXT_COLOR = 0xFFD64550;
    private static final int ENTERED_TEXT_COLOR_LIGHT = 0xFF1A56DB;
    private static final int ENTERED_TEXT_COLOR_DARK = 0xFF5B8DEF;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;

    private final Screen parentScreen;
    private final Difficulty difficulty;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private SudokuLogic logic;
    private int cellSize;
    private int boardX;
    private int boardY;
    private int elapsedTicks;
    private boolean recordSaved;
    private boolean isNewBestTime;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean notesMode;
    private GameBoxButton notesButton;

    public SudokuScreen(Screen parentScreen, Difficulty difficulty) {
        super(Component.translatable("gamebox.games.sudoku.name"));
        this.parentScreen = parentScreen;
        this.difficulty = difficulty;
    }

    @Override
    protected void init() {
        SudokuDifficultySettings settings = SudokuDifficultySettings.forDifficulty(difficulty);
        this.logic = new SudokuLogic(settings);
        this.elapsedTicks = 0;
        this.recordSaved = false;
        this.selectedRow = -1;
        this.selectedCol = -1;
        this.notesMode = false;

        leaderboardPanel.refresh(SudokuGame.ID, difficulty.name());

        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int availableHeight = this.height - TOP_MARGIN - HudBar.reservedHeight() - BOTTOM_MARGIN;
        int maxCellByWidth = availableWidth / BOARD_SIZE;
        int maxCellByHeight = availableHeight / BOARD_SIZE;
        this.cellSize = Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, Math.min(maxCellByWidth, maxCellByHeight)));

        int boardPixelSize = BOARD_SIZE * cellSize;
        this.boardX = this.width / 2 - boardPixelSize / 2;
        this.boardY = TOP_MARGIN + HudBar.reservedHeight();

        int totalButtonsWidth = 80 + BUTTON_GAP + 90 + BUTTON_GAP + 90;
        int startX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);
        int buttonY = this.height - 24;

        this.notesButton = new GameBoxButton(startX, buttonY, 80, 20, buildNotesLabel(), this::toggleNotesMode);
        this.addRenderableWidget(this.notesButton);

        this.addRenderableWidget(new GameBoxButton(
                startX + 80 + BUTTON_GAP, buttonY, 90, 20,
                Component.translatable("gamebox.games.sudoku.restart"),
                () -> {
                    logic = new SudokuLogic(settings);
                    elapsedTicks = 0;
                    recordSaved = false;
                    selectedRow = -1;
                    selectedCol = -1;
                    notesMode = false;
                    notesButton.setMessage(buildNotesLabel());
                    leaderboardPanel.refresh(SudokuGame.ID, difficulty.name());
                }
        ));
        this.addRenderableWidget(new GameBoxButton(
                startX + 80 + BUTTON_GAP + 90 + BUTTON_GAP, buttonY, 90, 20,
                Component.translatable("gamebox.common.back"),
                this::onClose
        ));
    }

    private void toggleNotesMode() {
        notesMode = !notesMode;
        notesButton.setMessage(buildNotesLabel());
    }

    private Component buildNotesLabel() {
        Component state = notesMode
                ? Component.translatable("gamebox.settings.on")
                : Component.translatable("gamebox.settings.off");
        return Component.translatable("gamebox.games.sudoku.notes", state);
    }

    @Override
    public void tick() {
        super.tick();
        if (!logic.isSolved()) {
            elapsedTicks++;
        }
    }

    private void checkWin() {
        if (logic.isSolved() && !recordSaved) {
            recordSaved = true;
            saveRecord();
            GameBoxSounds.play(SoundEvents.PLAYER_LEVELUP);
        }
    }

    private void saveRecord() {
        String difficultyKey = difficulty.name();
        SudokuRecord current = RecordManager.getRecord(SudokuGame.ID, difficultyKey, SudokuRecord.class, new SudokuRecord(0, 0));

        long timeMs = elapsedTicks * 50L;
        boolean firstEver = current.getGamesCompleted() == 0;
        this.isNewBestTime = firstEver || timeMs < current.getBestTimeMs();

        long newBestTime = firstEver ? timeMs : Math.min(current.getBestTimeMs(), timeMs);
        int newGamesCompleted = current.getGamesCompleted() + 1;

        RecordManager.putRecord(SudokuGame.ID, difficultyKey, new SudokuRecord(newBestTime, newGamesCompleted));

        GameBoxLeaderboardClient.submitScore(SudokuGame.ID, difficultyKey, timeMs, false);
        leaderboardPanel.refresh(SudokuGame.ID, difficultyKey);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (logic.isSolved()) {
            return false;
        }

        int localX = (int) event.x() - boardX;
        int localY = (int) event.y() - boardY;
        if (localX < 0 || localY < 0) {
            return false;
        }
        int col = localX / cellSize;
        int row = localY / cellSize;
        if (col >= BOARD_SIZE || row >= BOARD_SIZE) {
            return false;
        }

        selectedRow = row;
        selectedCol = col;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (selectedRow >= 0 && selectedCol >= 0 && !logic.isSolved()) {
            int digit = digitFromKey(key);
            if (digit > 0) {
                if (notesMode) {
                    logic.toggleNote(selectedRow, selectedCol, digit);
                    GameBoxSounds.play(SoundEvents.UI_BUTTON_CLICK, 1.6F);
                } else {
                    logic.setValue(selectedRow, selectedCol, digit);
                    GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.2F);
                    checkWin();
                }
                return true;
            }
            if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
                if (notesMode) {
                    logic.clearNotes(selectedRow, selectedCol);
                } else {
                    logic.clearValue(selectedRow, selectedCol);
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private int digitFromKey(int key) {
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
            return key - InputConstants.KEY_1 + 1;
        }
        if (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_9) {
            return key - GLFW.GLFW_KEY_KP_1 + 1;
        }
        return -1;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int boardPixelSize = BOARD_SIZE * cellSize;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String timeText = Component.translatable("gamebox.games.sudoku.time", formatElapsed()).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelSize, null, timeText);
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                drawCell(graphics, row, col);
            }
        }

        for (int i = 0; i <= BOARD_SIZE; i += 3) {
            int x = boardX + i * cellSize;
            graphics.fill(x - BOX_BORDER_THICKNESS / 2, boardY, x + BOX_BORDER_THICKNESS / 2, boardY + boardPixelSize, GameBoxTheme.panelBorder());
            int y = boardY + i * cellSize;
            graphics.fill(boardX, y - BOX_BORDER_THICKNESS / 2, boardX + boardPixelSize, y + BOX_BORDER_THICKNESS / 2, GameBoxTheme.panelBorder());
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (logic.isSolved()) {
            graphics.fill(boardX, boardY, boardX + boardPixelSize, boardY + boardPixelSize, OVERLAY_COLOR);

            String title = Component.translatable("gamebox.games.sudoku.solved").getString();
            int titleWidth = this.font.width(title);
            graphics.text(this.font, title, boardX + boardPixelSize / 2 - titleWidth / 2,
                    boardY + boardPixelSize / 2 - 10, VICTORY_COLOR, false);

            String detail = isNewBestTime
                    ? Component.translatable("gamebox.games.sudoku.new_best").getString()
                    : Component.translatable("gamebox.games.sudoku.solved_detail", formatElapsed()).getString();
            int detailWidth = this.font.width(detail);
            graphics.text(this.font, detail, boardX + boardPixelSize / 2 - detailWidth / 2,
                    boardY + boardPixelSize / 2 + 6, OVERLAY_TEXT_COLOR, false);
        }
    }

    private void drawCell(GuiGraphicsExtractor graphics, int row, int col) {
        int cellX = boardX + col * cellSize;
        int cellY = boardY + row * cellSize;

        int boxIndex = (row / 3) + (col / 3);
        int boxBackground = (boxIndex % 2 == 0) ? GameBoxTheme.sudokuBoxBackgroundA() : GameBoxTheme.sudokuBoxBackgroundB();
        graphics.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, boxBackground);

        boolean isSelected = row == selectedRow && col == selectedCol;
        int selectedValue = (selectedRow >= 0) ? logic.getValue(selectedRow, selectedCol) : 0;

        if (selectedRow >= 0 && !isSelected && logic.isPeer(row, col, selectedRow, selectedCol)) {
            graphics.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, PEER_HIGHLIGHT_COLOR);
        }
        if (selectedValue != 0 && !isSelected && logic.getValue(row, col) == selectedValue) {
            graphics.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, SAME_VALUE_HIGHLIGHT_COLOR);
        }
        if (isSelected) {
            graphics.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, GameBoxTheme.selectedCellColor());
        }

        graphics.outline(cellX, cellY, cellSize, cellSize, GameBoxTheme.cellBorder());

        int value = logic.getValue(row, col);
        if (value != 0) {
            int textColor;
            if (logic.hasConflict(row, col)) {
                textColor = CONFLICT_TEXT_COLOR;
            } else if (logic.isGiven(row, col)) {
                textColor = GameBoxTheme.primaryTextColor();
            } else {
                textColor = GameBoxConfig.get().isDarkTheme() ? ENTERED_TEXT_COLOR_DARK : ENTERED_TEXT_COLOR_LIGHT;
            }

            String text = String.valueOf(value);
            int textWidth = this.font.width(text);
            graphics.text(this.font, text,
                    cellX + cellSize / 2 - textWidth / 2,
                    cellY + cellSize / 2 - 4,
                    textColor, false);
            return;
        }

        if (cellSize >= MIN_CELL_SIZE_FOR_NOTES && logic.hasAnyNote(row, col)) {
            drawNotes(graphics, row, col, cellX, cellY);
        }
    }

    private void drawNotes(GuiGraphicsExtractor graphics, int row, int col, int cellX, int cellY) {
        int subCell = cellSize / 3;
        for (int digit = 1; digit <= 9; digit++) {
            if (!logic.hasNote(row, col, digit)) {
                continue;
            }
            int index = digit - 1;
            int subRow = index / 3;
            int subCol = index % 3;

            String text = String.valueOf(digit);
            int textWidth = this.font.width(text);
            int x = cellX + subCol * subCell + subCell / 2 - textWidth / 2;
            int y = cellY + subRow * subCell + subCell / 2 - 3;
            graphics.text(this.font, text, x, y, GameBoxTheme.subtitleColor(), false);
        }
    }

    private String formatElapsed() {
        int totalSeconds = elapsedTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}