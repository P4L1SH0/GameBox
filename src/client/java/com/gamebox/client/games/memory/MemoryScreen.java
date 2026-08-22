package com.gamebox.client.games.memory;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MemoryScreen extends Screen {

    private static final int MIN_CARD_SIZE = 14;
    private static final int MAX_CARD_SIZE = 64;
    private static final int TOP_MARGIN = 6;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_MARGIN = 10;
    private static final int CARD_GAP = 4;
    private static final int BUTTON_GAP = 10;
    private static final int LEADERBOARD_GAP = 8;
    private static final int FLIP_ANIMATION_TICKS = 6;
    private static final float MIN_SCALE_TO_SHOW_ICON = 0.15F;

    private static final int CARD_FACE_DOWN_COLOR_LIGHT = 0xFFA9AFB9;
    private static final int CARD_FACE_DOWN_HOVER_COLOR_LIGHT = 0xFFBCC2CB;
    private static final int CARD_FACE_DOWN_COLOR_DARK = 0xFF3A3F47;
    private static final int CARD_FACE_DOWN_HOVER_COLOR_DARK = 0xFF4A505A;
    private static final int CARD_FACE_UP_COLOR_LIGHT = 0xFFF4F5F7;
    private static final int CARD_FACE_UP_COLOR_DARK = 0xFF3D4759;
    private static final int CARD_MATCHED_COLOR_LIGHT = 0xFFCDEFD8;
    private static final int CARD_MATCHED_COLOR_DARK = 0xFF2F4C3B;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;

    private static final Item[] SYMBOL_ITEMS = {
            Items.APPLE, Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
            Items.REDSTONE, Items.COAL, Items.STICK, Items.BONE, Items.FEATHER,
            Items.ENDER_PEARL, Items.BLAZE_ROD, Items.GUNPOWDER, Items.SLIME_BALL, Items.NETHER_STAR
    };

    private final Screen parentScreen;
    private final Difficulty difficulty;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private MemoryLogic logic;
    private ItemStack[] symbolStacks;
    private int columns;
    private int rows;
    private int cardSize;
    private int boardX;
    private int boardY;
    private int elapsedTicks;
    private boolean recordSaved;
    private boolean isNewBestMoves;
    private boolean isNewBestTime;
    private int lastMouseX;
    private int lastMouseY;

    private int[] flipTicksRemaining;
    private boolean[] flipTargetFaceUp;

    public MemoryScreen(Screen parentScreen, Difficulty difficulty) {
        super(Component.translatable("gamebox.games.memory.name"));
        this.parentScreen = parentScreen;
        this.difficulty = difficulty;
    }

    @Override
    protected void init() {
        MemoryDifficultySettings settings = MemoryDifficultySettings.forDifficulty(difficulty);
        this.logic = new MemoryLogic(settings);
        this.elapsedTicks = 0;
        this.recordSaved = false;

        this.symbolStacks = new ItemStack[logic.getPairCount()];
        for (int i = 0; i < symbolStacks.length; i++) {
            symbolStacks[i] = new ItemStack(SYMBOL_ITEMS[i % SYMBOL_ITEMS.length]);
        }

        int cardCount = logic.getCardCount();
        this.flipTicksRemaining = new int[cardCount];
        this.flipTargetFaceUp = new boolean[cardCount];

        this.columns = (int) Math.ceil(Math.sqrt(cardCount));
        this.rows = (int) Math.ceil(cardCount / (double) columns);

        leaderboardPanel.refresh(MemoryGame.ID, difficulty.name());

        // Reserve room for the leaderboard panel BEFORE computing card size,
        // so a wide grid (e.g. Memory on Expert) shrinks enough to leave a
        // real left margin, instead of the panel getting hidden entirely.
        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int availableHeight = this.height - TOP_MARGIN - HudBar.reservedHeight() - BOTTOM_MARGIN;
        int maxCardByWidth = (availableWidth - (columns - 1) * CARD_GAP) / columns;
        int maxCardByHeight = (availableHeight - (rows - 1) * CARD_GAP) / rows;
        this.cardSize = Math.max(MIN_CARD_SIZE, Math.min(MAX_CARD_SIZE, Math.min(maxCardByWidth, maxCardByHeight)));

        int boardPixelWidth = columns * cardSize + (columns - 1) * CARD_GAP;
        int boardPixelHeight = rows * cardSize + (rows - 1) * CARD_GAP;
        this.boardX = this.width / 2 - boardPixelWidth / 2;
        this.boardY = TOP_MARGIN + HudBar.reservedHeight();

        int totalButtonsWidth = 100 + BUTTON_GAP + 100;
        int startX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);

        this.addRenderableWidget(new GameBoxButton(
                startX, this.height - 24, 100, 20,
                Component.translatable("gamebox.games.memory.restart"),
                () -> {
                    logic.restart();
                    elapsedTicks = 0;
                    recordSaved = false;
                    java.util.Arrays.fill(flipTicksRemaining, 0);
                    leaderboardPanel.refresh(MemoryGame.ID, difficulty.name());
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
        if (!logic.isWon()) {
            elapsedTicks++;
        } else if (!recordSaved) {
            recordSaved = true;
            saveRecord();
            GameBoxSounds.play(SoundEvents.PLAYER_LEVELUP);
        }

        for (int i = 0; i < flipTicksRemaining.length; i++) {
            if (flipTicksRemaining[i] > 0) {
                flipTicksRemaining[i]--;
            }
        }
    }

    private void saveRecord() {
        String difficultyKey = difficulty.name();
        MemoryRecord current = RecordManager.getRecord(
                MemoryGame.ID, difficultyKey, MemoryRecord.class, new MemoryRecord(0, 0, 0));

        long timeMs = elapsedTicks * 50L;
        int moves = logic.getMoveCount();
        boolean firstEver = current.getGamesCompleted() == 0;

        this.isNewBestMoves = firstEver || moves < current.getBestMoves();
        this.isNewBestTime = firstEver || timeMs < current.getBestTimeMs();

        int newBestMoves = firstEver ? moves : Math.min(current.getBestMoves(), moves);
        long newBestTime = firstEver ? timeMs : Math.min(current.getBestTimeMs(), timeMs);
        int newGamesCompleted = current.getGamesCompleted() + 1;

        RecordManager.putRecord(MemoryGame.ID, difficultyKey,
                new MemoryRecord(newBestMoves, newBestTime, newGamesCompleted));

        GameBoxLeaderboardClient.submitScore(MemoryGame.ID, difficultyKey, moves, false);
        leaderboardPanel.refresh(MemoryGame.ID, difficultyKey);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (logic.isWon()) {
            return false;
        }

        int localX = lastMouseX - boardX;
        int localY = lastMouseY - boardY;
        if (localX < 0 || localY < 0) {
            return false;
        }
        int col = localX / (cardSize + CARD_GAP);
        int row = localY / (cardSize + CARD_GAP);
        if (col >= columns || row >= rows) {
            return false;
        }
        int index = row * columns + col;
        if (index >= logic.getCardCount()) {
            return false;
        }

        boolean[] wasFaceUpBefore = new boolean[logic.getCardCount()];
        for (int i = 0; i < wasFaceUpBefore.length; i++) {
            wasFaceUpBefore[i] = logic.isFaceUp(i);
        }

        boolean completedCheck = logic.flip(index);

        for (int i = 0; i < wasFaceUpBefore.length; i++) {
            boolean isFaceUpNow = logic.isFaceUp(i);
            if (wasFaceUpBefore[i] != isFaceUpNow) {
                flipTicksRemaining[i] = FLIP_ANIMATION_TICKS;
                flipTargetFaceUp[i] = isFaceUpNow;
            }
        }

        if (completedCheck) {
            if (logic.wasLastPairAMatch()) {
                GameBoxSounds.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F);
            } else {
                GameBoxSounds.play(SoundEvents.ITEM_BREAK, 1.2F);
            }
        } else {
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

        int boardPixelWidth = columns * cardSize + (columns - 1) * CARD_GAP;
        int boardPixelHeight = rows * cardSize + (rows - 1) * CARD_GAP;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String movesText = Component.translatable("gamebox.games.memory.moves", logic.getMoveCount()).getString();
            String timeText = Component.translatable("gamebox.games.memory.time", formatElapsed()).getString();
            HudBar.draw(graphics, this.font, boardX, TOP_MARGIN, boardPixelWidth, movesText, timeText);
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int index = row * columns + col;
                if (index < logic.getCardCount()) {
                    drawCard(graphics, index, row, col, mouseX, mouseY, delta);
                }
            }
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = boardX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, boardY);
        }

        if (logic.isWon()) {
            graphics.fill(boardX, boardY, boardX + boardPixelWidth, boardY + boardPixelHeight, OVERLAY_COLOR);

            String title = Component.translatable("gamebox.games.memory.won").getString();
            int titleWidth = this.font.width(title);
            graphics.text(this.font, title, boardX + boardPixelWidth / 2 - titleWidth / 2,
                    boardY + boardPixelHeight / 2 - 10, VICTORY_COLOR, false);

            String detail = (isNewBestMoves || isNewBestTime)
                    ? Component.translatable("gamebox.games.memory.new_best").getString()
                    : Component.translatable("gamebox.games.memory.won_detail", logic.getMoveCount(), formatElapsed()).getString();
            int detailWidth = this.font.width(detail);
            graphics.text(this.font, detail, boardX + boardPixelWidth / 2 - detailWidth / 2,
                    boardY + boardPixelHeight / 2 + 6, OVERLAY_TEXT_COLOR, false);
        }
    }

    private void drawCard(GuiGraphicsExtractor graphics, int index, int row, int col, int mouseX, int mouseY, float delta) {
        int cardX = boardX + col * (cardSize + CARD_GAP);
        int cardY = boardY + row * (cardSize + CARD_GAP);
        boolean hovered = mouseX >= cardX && mouseX < cardX + cardSize && mouseY >= cardY && mouseY < cardY + cardSize;

        boolean animating = flipTicksRemaining[index] > 0;

        if (!animating) {
            drawCardFace(graphics, cardX, cardY, cardSize, index, logic.isFaceUp(index), logic.isMatched(index), hovered);
            return;
        }

        float progress = 1.0F - (flipTicksRemaining[index] - delta) / (float) FLIP_ANIMATION_TICKS;
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        float scaleX = (float) Math.abs(Math.cos(progress * Math.PI));

        boolean showingNewFace = progress >= 0.5F;
        boolean faceUpToShow = showingNewFace ? flipTargetFaceUp[index] : !flipTargetFaceUp[index];
        boolean matchedToShow = showingNewFace && logic.isMatched(index);

        int drawWidth = Math.max(1, Math.round(cardSize * scaleX));
        int xOffset = (cardSize - drawWidth) / 2;

        drawCardFace(graphics, cardX + xOffset, cardY, drawWidth, index, faceUpToShow, matchedToShow, false,
                scaleX >= MIN_SCALE_TO_SHOW_ICON);
    }

    private void drawCardFace(GuiGraphicsExtractor graphics, int x, int y, int width, int index,
                              boolean faceUp, boolean matched, boolean hovered) {
        drawCardFace(graphics, x, y, width, index, faceUp, matched, hovered, true);
    }

    private void drawCardFace(GuiGraphicsExtractor graphics, int x, int y, int width, int index,
                              boolean faceUp, boolean matched, boolean hovered, boolean showIcon) {
        boolean dark = GameBoxConfig.get().isDarkTheme();

        int color;
        if (matched) {
            color = dark ? CARD_MATCHED_COLOR_DARK : CARD_MATCHED_COLOR_LIGHT;
        } else if (faceUp) {
            color = dark ? CARD_FACE_UP_COLOR_DARK : CARD_FACE_UP_COLOR_LIGHT;
        } else if (hovered && !logic.isWon()) {
            color = dark ? CARD_FACE_DOWN_HOVER_COLOR_DARK : CARD_FACE_DOWN_HOVER_COLOR_LIGHT;
        } else {
            color = dark ? CARD_FACE_DOWN_COLOR_DARK : CARD_FACE_DOWN_COLOR_LIGHT;
        }

        graphics.fill(x, y, x + width, y + cardSize, color);
        graphics.outline(x, y, width, cardSize, GameBoxTheme.cellBorder());

        if (faceUp && showIcon) {
            ItemStack stack = symbolStacks[logic.getSymbolId(index)];
            ItemIconRenderer.drawCentered(graphics, stack, x, y, Math.min(width, cardSize));
        }
    }

    private String formatElapsed() {
        int totalSeconds = elapsedTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}