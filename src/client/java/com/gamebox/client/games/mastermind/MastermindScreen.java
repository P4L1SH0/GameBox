package com.gamebox.client.games.mastermind;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.core.Difficulty;
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

import java.util.Arrays;
import java.util.List;

public class MastermindScreen extends Screen {

    private static final int PEG_SIZE = 20;
    private static final int PEG_GAP = 4;
    private static final int PEG_FRAME_THICKNESS = 2;
    private static final int ROW_GAP = 4;
    private static final int PALETTE_SWATCH_SIZE = 24;
    private static final int PALETTE_GAP = 6;
    private static final int TOP_MARGIN = 6;
    private static final int SECTION_GAP = 10;
    private static final int BUTTON_ROW_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_GAP = 4;
    private static final int SCROLLBAR_TRACK_COLOR = 0x40000000;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFF8C939C;
    private static final int LEADERBOARD_GAP = 8;

    private static final int[] PALETTE_COLORS = {
            0xFFD64550, 0xFFF2C94C, 0xFF2F80ED, 0xFF9B51E0, 0xFF17C3B2, 0xFFEB5DA8, 0xFF8B5E34, 0xFF56CCF2
    };

    private static final int STATUS_EXACT_COLOR = 0xFF2ECC71;
    private static final int STATUS_COLOR_MATCH_COLOR = 0xFFE67E22;
    private static final int STATUS_NONE_COLOR = 0xFF8C939C;

    private static final int SLOT_EMPTY_COLOR_LIGHT = 0xFFDCE0E4;
    private static final int SLOT_EMPTY_COLOR_DARK = 0xFF2B2F38;

    private static final int RESULT_TEXT_COLOR_WIN = 0xFFFFD86B;
    private static final int RESULT_TEXT_COLOR_LOSS = 0xFFD64550;

    private final Screen parentScreen;
    private final Difficulty difficulty;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private MastermindLogic logic;
    private int[] currentGuess;
    private boolean recordSaved;
    private boolean isNewBestAttempts;

    private int pegRowX;
    private int paletteRowX;
    private int historyTop;
    private int historyBottom;
    private int currentGuessY;
    private int paletteY;
    private int scrollOffset;
    private int maxScroll;
    private int fullHistoryHeight;

    private GameBoxButton submitButton;

    public MastermindScreen(Screen parentScreen, Difficulty difficulty) {
        super(Component.translatable("gamebox.games.mastermind.name"));
        this.parentScreen = parentScreen;
        this.difficulty = difficulty;
    }

    @Override
    protected void init() {
        MastermindDifficultySettings settings = MastermindDifficultySettings.forDifficulty(difficulty);
        this.logic = new MastermindLogic(settings);
        this.currentGuess = emptyGuess();
        this.recordSaved = false;

        leaderboardPanel.refresh(MastermindGame.ID, difficulty.name());

        // The peg rows and palette are each centered on screen based on
        // their own width - reserve room on both sides here too, so the
        // leaderboard panel always has space to its left of the pegs.
        int leaderboardReserve = leaderboardPanel.isVisible() ? ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP : 0;
        this.pegRowX = Math.max(leaderboardReserve + 4, this.width / 2 - historyRowWidth() / 2);
        this.paletteRowX = this.width / 2 - paletteRowWidth() / 2;

        int rowHeight = PEG_SIZE + ROW_GAP;

        this.historyTop = TOP_MARGIN + HudBar.reservedHeight();

        int reservedBottom = SECTION_GAP + PEG_SIZE + SECTION_GAP + PALETTE_SWATCH_SIZE
                + SECTION_GAP + BUTTON_ROW_HEIGHT + SECTION_GAP;
        int availableHistoryHeight = this.height - historyTop - reservedBottom;
        this.fullHistoryHeight = settings.maxAttempts() * rowHeight;
        int viewportHeight = Math.max(rowHeight, Math.min(fullHistoryHeight, availableHistoryHeight));

        this.historyBottom = historyTop + viewportHeight;
        this.maxScroll = Math.max(0, fullHistoryHeight - viewportHeight);
        this.scrollOffset = 0;

        this.currentGuessY = historyBottom + SECTION_GAP;
        this.paletteY = currentGuessY + PEG_SIZE + SECTION_GAP;

        int buttonY = this.paletteY + PALETTE_SWATCH_SIZE + SECTION_GAP;
        int totalButtonsWidth = 90 + BUTTON_GAP + 90 + BUTTON_GAP + 90 + BUTTON_GAP + 90;
        int buttonStartX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);

        this.addRenderableWidget(new GameBoxButton(buttonStartX, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.common.how_to_play"),
                () -> this.minecraft.gui.setScreen(new HowToPlayScreen(this, this.getTitle(), getInstructions()))));

        this.submitButton = new GameBoxButton(buttonStartX + 90 + BUTTON_GAP, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.games.mastermind.submit"), this::submitGuess);
        this.submitButton.active = false;
        this.addRenderableWidget(this.submitButton);

        this.addRenderableWidget(new GameBoxButton(buttonStartX + 90 + BUTTON_GAP + 90 + BUTTON_GAP, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.games.mastermind.restart"), this::restart));

        this.addRenderableWidget(new GameBoxButton(buttonStartX + 90 + BUTTON_GAP + 90 + BUTTON_GAP + 90 + BUTTON_GAP, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.common.back"), this::onClose));
    }

    private Component getInstructions() {
        return Component.translatable("gamebox.games.mastermind.howto");
    }

    private int[] emptyGuess() {
        int codeLength = logic == null
                ? MastermindDifficultySettings.forDifficulty(difficulty).codeLength()
                : logic.getSettings().codeLength();
        int[] guess = new int[codeLength];
        Arrays.fill(guess, -1);
        return guess;
    }

    private int historyRowWidth() {
        int codeLength = logic == null
                ? MastermindDifficultySettings.forDifficulty(difficulty).codeLength()
                : logic.getSettings().codeLength();
        return codeLength * PEG_SIZE + (codeLength - 1) * PEG_GAP;
    }

    private int paletteRowWidth() {
        int colorCount = logic == null ? MastermindDifficultySettings.MAX_COLOR_COUNT : logic.getSettings().colorCount();
        return colorCount * PALETTE_SWATCH_SIZE + (colorCount - 1) * PALETTE_GAP;
    }

    private void restart() {
        this.logic.restart();
        this.currentGuess = emptyGuess();
        this.recordSaved = false;
        this.scrollOffset = 0;
        leaderboardPanel.refresh(MastermindGame.ID, difficulty.name());
    }

    private void submitGuess() {
        if (logic.isFinished() || !isGuessComplete()) {
            return;
        }

        MastermindLogic.GuessResult result = logic.submitGuess(currentGuess);
        this.currentGuess = emptyGuess();

        if (result != null) {
            if (logic.isWon()) {
                GameBoxSounds.play(SoundEvents.PLAYER_LEVELUP);
                saveRecordIfNeeded();
            } else if (logic.isOutOfAttempts()) {
                GameBoxSounds.play(SoundEvents.ITEM_BREAK);
            } else {
                float pitch = 0.8F + result.exactMatches() * 0.15F;
                GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HARP, pitch);
            }
        }

        int viewportHeight = historyBottom - historyTop;
        this.maxScroll = Math.max(0, fullHistoryHeight - viewportHeight);
        int rowHeight = PEG_SIZE + ROW_GAP;
        int latestRowBottom = logic.getAttemptsUsed() * rowHeight;
        this.scrollOffset = Math.max(0, Math.min(maxScroll, latestRowBottom - viewportHeight));
    }

    private void saveRecordIfNeeded() {
        if (recordSaved) {
            return;
        }
        recordSaved = true;

        String difficultyKey = difficulty.name();
        MastermindRecord current = RecordManager.getRecord(MastermindGame.ID, difficultyKey, MastermindRecord.class, new MastermindRecord(0, 0));
        boolean firstWin = current.getGamesWon() == 0;
        this.isNewBestAttempts = firstWin || logic.getAttemptsUsed() < current.getBestAttempts();

        int newBestAttempts = firstWin ? logic.getAttemptsUsed() : Math.min(current.getBestAttempts(), logic.getAttemptsUsed());
        int newGamesWon = current.getGamesWon() + 1;
        RecordManager.putRecord(MastermindGame.ID, difficultyKey, new MastermindRecord(newBestAttempts, newGamesWon));

        GameBoxLeaderboardClient.submitScore(MastermindGame.ID, difficultyKey, logic.getAttemptsUsed(), false);
        leaderboardPanel.refresh(MastermindGame.ID, difficultyKey);
    }

    private boolean isGuessComplete() {
        for (int color : currentGuess) {
            if (color == -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int rowHeight = PEG_SIZE + ROW_GAP;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * rowHeight));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (logic.isFinished()) {
            return false;
        }

        int x = (int) event.x();
        int y = (int) event.y();

        for (int i = 0; i < currentGuess.length; i++) {
            int slotX = pegRowX + i * (PEG_SIZE + PEG_GAP);
            if (currentGuess[i] != -1 && withinBounds(x, y, slotX, currentGuessY, PEG_SIZE, PEG_SIZE)) {
                currentGuess[i] = -1;
                GameBoxSounds.play(SoundEvents.UI_BUTTON_CLICK, 1.3F);
                return true;
            }
        }

        int colorCount = logic.getSettings().colorCount();
        for (int i = 0; i < colorCount; i++) {
            int swatchX = paletteRowX + i * (PALETTE_SWATCH_SIZE + PALETTE_GAP);
            if (withinBounds(x, y, swatchX, paletteY, PALETTE_SWATCH_SIZE, PALETTE_SWATCH_SIZE)) {
                fillFirstEmptySlot(i);
                return true;
            }
        }

        return false;
    }

    private boolean withinBounds(int x, int y, int boxX, int boxY, int boxWidth, int boxHeight) {
        return x >= boxX && x < boxX + boxWidth && y >= boxY && y < boxY + boxHeight;
    }

    private void fillFirstEmptySlot(int colorIndex) {
        for (int i = 0; i < currentGuess.length; i++) {
            if (currentGuess[i] == -1) {
                currentGuess[i] = colorIndex;
                GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.0F + colorIndex * 0.1F);
                return;
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.submitButton.active = !logic.isFinished() && isGuessComplete();

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int hudWidth = Math.max(paletteRowWidth(), historyRowWidth());
        int hudX = this.width / 2 - hudWidth / 2;

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String attemptsText = Component.translatable("gamebox.games.mastermind.attempts",
                    logic.getAttemptsUsed(), logic.getSettings().maxAttempts()).getString();
            String statusText = "";
            if (logic.isWon()) {
                statusText = Component.translatable("gamebox.games.mastermind.solved").getString();
            } else if (logic.isOutOfAttempts()) {
                statusText = Component.translatable("gamebox.games.mastermind.failed").getString();
            }
            HudBar.draw(graphics, this.font, hudX, TOP_MARGIN, hudWidth, attemptsText, statusText);
        }

        graphics.enableScissor(pegRowX, historyTop, pegRowX + historyRowWidth(), historyBottom);
        drawHistory(graphics);
        graphics.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(graphics);
        }

        if (leaderboardPanel.isVisible()) {
            int panelX = pegRowX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, historyTop);
        }

        drawCurrentGuessOrSecret(graphics);
        drawPalette(graphics);

        if (logic.isFinished()) {
            drawResultBanner(graphics);
        }
    }

    private void drawHistory(GuiGraphicsExtractor graphics) {
        List<MastermindLogic.GuessResult> guesses = logic.getGuesses();
        int rowHeight = PEG_SIZE + ROW_GAP;
        int y = historyTop - scrollOffset;

        for (MastermindLogic.GuessResult result : guesses) {
            for (int i = 0; i < result.guess().length; i++) {
                int pegX = pegRowX + i * (PEG_SIZE + PEG_GAP);
                int statusColor = statusColorFor(result.positionStatus()[i]);
                drawFramedPeg(graphics, pegX, y, PALETTE_COLORS[result.guess()[i]], statusColor);
            }
            y += rowHeight;
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        int trackHeight = historyBottom - historyTop;
        int trackX = pegRowX + historyRowWidth() + SCROLLBAR_GAP;

        graphics.fill(trackX, historyTop, trackX + SCROLLBAR_WIDTH, historyBottom, SCROLLBAR_TRACK_COLOR);

        int thumbHeight = Math.max(10, (int) ((float) trackHeight / fullHistoryHeight * trackHeight));
        int scrollableTrack = trackHeight - thumbHeight;
        int thumbY = historyTop + (maxScroll == 0 ? 0 : (int) ((float) scrollOffset / maxScroll * scrollableTrack));

        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
    }

    private int statusColorFor(MastermindLogic.PegStatus status) {
        return switch (status) {
            case EXACT -> STATUS_EXACT_COLOR;
            case COLOR_MATCH -> STATUS_COLOR_MATCH_COLOR;
            case NONE -> STATUS_NONE_COLOR;
        };
    }

    private void drawCurrentGuessOrSecret(GuiGraphicsExtractor graphics) {
        boolean showSecret = logic.isFinished();
        int[] colorsToShow = showSecret ? logic.getSecret() : currentGuess;

        for (int i = 0; i < colorsToShow.length; i++) {
            int slotX = pegRowX + i * (PEG_SIZE + PEG_GAP);
            int colorIndex = colorsToShow[i];
            if (colorIndex == -1) {
                boolean dark = GameBoxConfig.get().isDarkTheme();
                int emptyColor = dark ? SLOT_EMPTY_COLOR_DARK : SLOT_EMPTY_COLOR_LIGHT;
                graphics.fill(slotX, currentGuessY, slotX + PEG_SIZE, currentGuessY + PEG_SIZE, emptyColor);
                graphics.outline(slotX, currentGuessY, PEG_SIZE, PEG_SIZE, GameBoxTheme.cellBorder());
            } else {
                drawPlainPeg(graphics, slotX, currentGuessY, PALETTE_COLORS[colorIndex]);
            }
        }
    }

    private void drawPlainPeg(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + PEG_SIZE, y + PEG_SIZE, color);
        graphics.outline(x, y, PEG_SIZE, PEG_SIZE, GameBoxTheme.cellBorder());
    }

    private void drawFramedPeg(GuiGraphicsExtractor graphics, int x, int y, int fillColor, int frameColor) {
        graphics.fill(x, y, x + PEG_SIZE, y + PEG_SIZE, frameColor);
        graphics.fill(x + PEG_FRAME_THICKNESS, y + PEG_FRAME_THICKNESS,
                x + PEG_SIZE - PEG_FRAME_THICKNESS, y + PEG_SIZE - PEG_FRAME_THICKNESS, fillColor);
    }

    private void drawPalette(GuiGraphicsExtractor graphics) {
        int colorCount = logic.getSettings().colorCount();
        for (int i = 0; i < colorCount; i++) {
            int swatchX = paletteRowX + i * (PALETTE_SWATCH_SIZE + PALETTE_GAP);
            graphics.fill(swatchX, paletteY, swatchX + PALETTE_SWATCH_SIZE, paletteY + PALETTE_SWATCH_SIZE, PALETTE_COLORS[i]);
            graphics.outline(swatchX, paletteY, PALETTE_SWATCH_SIZE, PALETTE_SWATCH_SIZE, GameBoxTheme.cellBorder());
        }
    }

    private void drawResultBanner(GuiGraphicsExtractor graphics) {
        String text;
        int color;
        if (logic.isWon()) {
            text = isNewBestAttempts
                    ? Component.translatable("gamebox.games.mastermind.new_best").getString()
                    : Component.translatable("gamebox.games.mastermind.won_detail", logic.getAttemptsUsed()).getString();
            color = RESULT_TEXT_COLOR_WIN;
        } else {
            text = Component.translatable("gamebox.games.mastermind.lost_detail").getString();
            color = RESULT_TEXT_COLOR_LOSS;
        }
        int textWidth = this.font.width(text);
        graphics.text(this.font, text, this.width / 2 - textWidth / 2, currentGuessY - 12, color, false);
    }
}