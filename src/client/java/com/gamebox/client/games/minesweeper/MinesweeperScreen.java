package com.gamebox.client.games.minesweeper;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.core.Difficulty;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.GameBoxButton;
import com.gamebox.client.ui.HudBar;
import com.gamebox.client.ui.ItemIconRenderer;
import com.gamebox.client.ui.ServerLeaderboardPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class MinesweeperScreen extends Screen {

    private static final int MIN_CELL_SIZE = 8;
    private static final int MAX_CELL_SIZE = 36;
    private static final int TOP_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_MARGIN = 10;
    private static final int BUTTON_GAP = 10;
    private static final int LEADERBOARD_GAP = 8;

    private static final int CELL_HIDDEN_COLOR = 0xFF8C939C;
    private static final int CELL_HIDDEN_HOVER_COLOR = 0xFFA0A7B0;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;

    private static final int[] NUMBER_COLORS_LIGHT = {
            0x00000000, 0xFF1A56DB, 0xFF15803D, 0xFFD64550, 0xFF5B21B6,
            0xFF9A3412, 0xFF0E7490, 0xFF2B2E33, 0xFF6B7178
    };
    private static final int[] NUMBER_COLORS_DARK = {
            0x00000000, 0xFF5B8DEF, 0xFF4ADE80, 0xFFFF7A7A, 0xFFC084FC,
            0xFFFFA95E, 0xFF5EEAD4, 0xFFF0F1F3, 0xFFA9AFB9
    };

    private final Screen parentScreen;
    private final Difficulty difficulty;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private MinesweeperLogic logic;
    private int cellSize;
    private int boardX;
    private int boardY;
    private int elapsedTicks;
    private boolean recordSaved;
    private boolean isNewBestTime;
    private int lastMouseX;
    private int lastMouseY;
    private ItemStack mineItemStack;
    private ItemStack flagItemStack;

    public MinesweeperScreen(Screen parentScreen, Difficulty difficulty) {
        super(Component.translatable("gamebox.games.minesweeper.name"));
        this.parentScreen = parentScreen;
        this.difficulty = difficulty;
    }

    @Override
    protected void init() {
        MinesweeperDifficultySettings settings = MinesweeperDifficultySettings.forDifficulty(difficulty);
        this.logic = new MinesweeperLogic(settings);
        this.elapsedTicks = 0;
        this.recordSaved = false;
        this.mineItemStack = new ItemStack(Items.TNT);
        this.flagItemStack = new ItemStack(Items.BANNER.red());

        leaderboardPanel.refresh(MinesweeperGame.ID, difficulty.name());

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

        int totalButtonsWidth = 100 + BUTTON_GAP + 100;
        int startX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);

        this.addRenderableWidget(new GameBoxButton(
                startX, this.height - 24, 100, 20,
                Component.translatable("gamebox.games.minesweeper.restart"),
                () -> {
                    logic = new MinesweeperLogic(settings);
                    elapsedTicks = 0;
                    recordSaved = false;
                    leaderboardPanel.refresh(MinesweeperGame.ID, difficulty.name());
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
        if (!logic.isFinished()) {
            elapsedTicks++;
        } else if (!recordSaved) {
            recordSaved = true;
            if (logic.isWon()) {
                saveRecord();
                GameBoxSounds.play(SoundEvents.PLAYER_LEVELUP);
            } else {
                GameBoxSounds.play(SoundEvents.ITEM_BREAK);
            }
        }
    }

    private void saveRecord() {
        String difficultyKey = difficulty.name();
        MinesweeperRecord current = RecordManager.getRecord(
                MinesweeperGame.ID, difficultyKey, MinesweeperRecord.class, new MinesweeperRecord(0, 0));

        long timeMs = elapsedTicks * 50L;
        boolean firstWin = current.getGamesWon() == 0;
        this.isNewBestTime = firstWin || timeMs < current.getBestTimeMs();

        long newBestTime = firstWin ? timeMs : Math.min(current.getBestTimeMs(), timeMs);
        int newGamesWon = current.getGamesWon() + 1;

        RecordManager.putRecord(MinesweeperGame.ID, difficultyKey, new MinesweeperRecord(newBestTime, newGamesWon));

        GameBoxLeaderboardClient.submitScore(MinesweeperGame.ID, difficultyKey, timeMs, false);
        leaderboardPanel.refresh(MinesweeperGame.ID, difficultyKey);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (logic.isFinished()) {
            return false;
        }

        int localX = lastMouseX - boardX;
        int localY = lastMouseY - boardY;
        if (localX < 0 || localY < 0) {
            return false;
        }
        int cellX = localX / cellSize;
        int cellY = localY / cellSize;
        if (cellX >= logic.getBoardWidth() || cellY >= logic.getBoardHeight()) {
            return false;
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            logic.toggleFlag(cellX, cellY);
            GameBoxSounds.play(SoundEvents.UI_BUTTON_CLICK, 1.6F);
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (logic.isRevealed(cellX, cellY)) {
                boolean revealedSomething = logic.chord(cellX, cellY);
                if (revealedSomething && !logic.isFinished()) {
                    GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.4F);
                }
            } else {
                logic.reveal(cellX, cellY);
                if (!logic.isFinished()) {
                    GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.2F);
                }
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int width = logic.getBoardWidth();
        int height = logic.getBoardHeight();
        int boardPixelWidth = width * cellSize;
        int boardPixelHeight = height * cellSize;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String minesText = Component.translatable("gamebox.games.minesweeper.mines", logic.getFlagsRemaining()).getString();
            String timeText = Component.translatable("gamebox.games.minesweeper.time", formatElapsed()).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelWidth, minesText, timeText);
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                drawCell(graphics, x, y, mouseX, mouseY);
            }
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (logic.isFinished()) {
            graphics.fill(boardX, boardY, boardX + boardPixelWidth, boardY + boardPixelHeight, OVERLAY_COLOR);

            String title = logic.isWon()
                    ? Component.translatable("gamebox.games.minesweeper.won").getString()
                    : Component.translatable("gamebox.games.minesweeper.exploded").getString();
            int titleWidth = this.font.width(title);
            graphics.text(this.font, title, boardX + boardPixelWidth / 2 - titleWidth / 2,
                    boardY + boardPixelHeight / 2 - 16, logic.isWon() ? VICTORY_COLOR : OVERLAY_TEXT_COLOR, false);

            String detail;
            if (logic.isWon() && isNewBestTime) {
                detail = Component.translatable("gamebox.games.minesweeper.new_best").getString();
            } else if (logic.isWon()) {
                detail = Component.translatable("gamebox.games.minesweeper.won_detail", formatElapsed()).getString();
            } else {
                detail = Component.translatable("gamebox.games.minesweeper.restart_hint").getString();
            }
            int detailWidth = this.font.width(detail);
            graphics.text(this.font, detail, boardX + boardPixelWidth / 2 - detailWidth / 2,
                    boardY + boardPixelHeight / 2, OVERLAY_TEXT_COLOR, false);
        }
    }

    private void drawCell(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        int cellX = boardX + x * cellSize;
        int cellY = boardY + y * cellSize;
        boolean hovered = mouseX >= cellX && mouseX < cellX + cellSize && mouseY >= cellY && mouseY < cellY + cellSize;

        int iconSize = Math.max(6, (int) (cellSize * 0.7F));
        int iconOffset = (cellSize - iconSize) / 2;

        boolean showAsRevealed = logic.isRevealed(x, y)
                || (logic.isFinished() && !logic.isWon() && logic.isMine(x, y));

        if (!showAsRevealed) {
            int color = (hovered && !logic.isFinished()) ? CELL_HIDDEN_HOVER_COLOR : CELL_HIDDEN_COLOR;
            graphics.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, color);
            graphics.outline(cellX, cellY, cellSize, cellSize, GameBoxTheme.cellBorder());

            if (logic.isFlagged(x, y)) {
                ItemIconRenderer.drawCentered(graphics, flagItemStack, cellX + iconOffset, cellY + iconOffset, iconSize);
            }
            return;
        }

        graphics.fill(cellX + 1, cellY + 1, cellX + cellSize - 1, cellY + cellSize - 1, GameBoxTheme.boardBackground());
        graphics.outline(cellX, cellY, cellSize, cellSize, GameBoxTheme.cellBorder());

        if (logic.isMine(x, y)) {
            ItemIconRenderer.drawCentered(graphics, mineItemStack, cellX + iconOffset, cellY + iconOffset, iconSize);
            return;
        }

        int adjacentMines = logic.getAdjacentMineCount(x, y);
        if (adjacentMines > 0) {
            int[] palette = GameBoxConfig.get().isDarkTheme() ? NUMBER_COLORS_DARK : NUMBER_COLORS_LIGHT;
            drawCenteredText(graphics, String.valueOf(adjacentMines), cellX, cellY, palette[adjacentMines]);
        }
    }

    private void drawCenteredText(GuiGraphicsExtractor graphics, String text, int cellX, int cellY, int color) {
        int textWidth = this.font.width(text);
        graphics.text(this.font, text,
                cellX + cellSize / 2 - textWidth / 2,
                cellY + cellSize / 2 - 4,
                color, false);
    }

    private String formatElapsed() {
        int totalSeconds = elapsedTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}