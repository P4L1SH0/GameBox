package com.gamebox.client.games.flappybird;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.client.records.RecordManager;
import com.gamebox.client.ui.GameBoxButton;
import com.gamebox.client.ui.HowToPlayScreen;
import com.gamebox.client.ui.HudBar;
import com.gamebox.client.ui.ItemIconRenderer;
import com.gamebox.client.ui.ServerLeaderboardPanel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FlappyBirdScreen extends Screen {

    private static final int TOP_MARGIN = 6;
    private static final int SIDE_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 10;
    private static final int SECTION_GAP = 8;
    private static final int BUTTON_ROW_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int LEADERBOARD_GAP = 8;

    private static final int SKY_COLOR_LIGHT = 0xFFB3E5FC;
    private static final int SKY_COLOR_DARK = 0xFF1B2838;
    private static final int PIPE_COLOR = 0xFF2E7D32;
    private static final int PIPE_CAP_COLOR = 0xFF1B5E20;
    private static final int PIPE_CAP_HEIGHT = 6;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int VICTORY_COLOR = 0xFFFFD86B;

    private final Screen parentScreen;
    private final ServerLeaderboardPanel leaderboardPanel = new ServerLeaderboardPanel();

    private FlappyBirdLogic logic;
    private float scale;
    private int worldOriginX;
    private int worldOriginY;
    private int worldPixelWidth;
    private int worldPixelHeight;
    private ItemStack birdItemStack;
    private boolean recordSaved;
    private boolean isNewBestScore;
    private int bestScoreAtStart;

    public FlappyBirdScreen(Screen parentScreen) {
        super(Component.translatable("gamebox.games.flappy_bird.name"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        this.logic = new FlappyBirdLogic();
        this.recordSaved = false;
        this.birdItemStack = new ItemStack(Items.BAT_SPAWN_EGG);
        refreshBestScore();

        leaderboardPanel.refresh(FlappyBirdGame.ID, "career");

        int leaderboardReserve = leaderboardPanel.isVisible() ? 2 * (ServerLeaderboardPanel.WIDTH + LEADERBOARD_GAP) : 0;
        int availableWidth = this.width - SIDE_MARGIN * 2 - leaderboardReserve;
        int reservedHeight = TOP_MARGIN + HudBar.reservedHeight() + SECTION_GAP + BUTTON_ROW_HEIGHT + BOTTOM_MARGIN;
        int availableHeight = this.height - reservedHeight;

        float scaleByWidth = availableWidth / FlappyBirdLogic.WORLD_WIDTH;
        float scaleByHeight = availableHeight / FlappyBirdLogic.WORLD_HEIGHT;
        this.scale = Math.max(0.5f, Math.min(scaleByWidth, scaleByHeight));

        this.worldPixelWidth = Math.round(FlappyBirdLogic.WORLD_WIDTH * scale);
        this.worldPixelHeight = Math.round(FlappyBirdLogic.WORLD_HEIGHT * scale);
        this.worldOriginX = this.width / 2 - worldPixelWidth / 2;
        this.worldOriginY = TOP_MARGIN + HudBar.reservedHeight();

        int buttonY = worldOriginY + worldPixelHeight + SECTION_GAP;
        int totalButtonsWidth = 90 + BUTTON_GAP + 90;
        int buttonStartX = Math.max(4, this.width / 2 - totalButtonsWidth / 2);

        this.addRenderableWidget(new GameBoxButton(buttonStartX, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.common.how_to_play"),
                () -> this.minecraft.gui.setScreen(new HowToPlayScreen(this, this.getTitle(), getInstructions()))));

        this.addRenderableWidget(new GameBoxButton(buttonStartX + 90 + BUTTON_GAP, buttonY, 90, BUTTON_ROW_HEIGHT,
                Component.translatable("gamebox.common.back"), this::onClose));
    }

    private Component getInstructions() {
        return Component.translatable("gamebox.games.flappy_bird.howto");
    }

    private void refreshBestScore() {
        this.bestScoreAtStart = RecordManager
                .getRecord(FlappyBirdGame.ID, "career", FlappyBirdRecord.class, new FlappyBirdRecord(0, 0))
                .getBestScore();
    }

    private void restart() {
        this.logic.restart();
        this.recordSaved = false;
        refreshBestScore();
        leaderboardPanel.refresh(FlappyBirdGame.ID, "career");
    }

    private void saveRecordIfNeeded() {
        if (recordSaved) {
            return;
        }
        recordSaved = true;

        FlappyBirdRecord current = RecordManager.getRecord(FlappyBirdGame.ID, "career", FlappyBirdRecord.class, new FlappyBirdRecord(0, 0));
        this.isNewBestScore = logic.getScore() > current.getBestScore();
        int newBestScore = Math.max(current.getBestScore(), logic.getScore());
        int newGamesPlayed = current.getGamesPlayed() + 1;
        RecordManager.putRecord(FlappyBirdGame.ID, "career", new FlappyBirdRecord(newBestScore, newGamesPlayed));

        GameBoxLeaderboardClient.submitScore(FlappyBirdGame.ID, "career", logic.getScore(), true);
        leaderboardPanel.refresh(FlappyBirdGame.ID, "career");
    }

    private void handleFlapOrRestart() {
        if (logic.isGameOver()) {
            restart();
            return;
        }
        logic.flap();
        GameBoxSounds.play(SoundEvents.NOTE_BLOCK_HAT, 1.3F);
    }

    @Override
    public void tick() {
        super.tick();
        if (logic.isGameOver()) {
            return;
        }

        int scoreBefore = logic.getScore();
        logic.tick();

        if (logic.getScore() > scoreBefore) {
            GameBoxSounds.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4F);
        }

        if (logic.isGameOver()) {
            GameBoxSounds.play(SoundEvents.ITEM_BREAK);
            saveRecordIfNeeded();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE) {
            handleFlapOrRestart();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int x = (int) event.x();
        int y = (int) event.y();
        if (x >= worldOriginX && x < worldOriginX + worldPixelWidth
                && y >= worldOriginY && y < worldOriginY + worldPixelHeight) {
            handleFlapOrRestart();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parentScreen);
    }

    private int worldToScreenX(float worldX) {
        return worldOriginX + Math.round(worldX * scale);
    }

    private int worldToScreenY(float worldY) {
        return worldOriginY + Math.round(worldY * scale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (GameBoxConfig.get().isShowScoreOverlay()) {
            String scoreText = Component.translatable("gamebox.games.flappy_bird.score", logic.getScore()).getString();
            String bestText = Component.translatable("gamebox.games.flappy_bird.best", bestScoreAtStart).getString();
            HudBar.draw(graphics, this.font, worldOriginX, TOP_MARGIN, worldPixelWidth, scoreText, bestText);
        }

        boolean dark = GameBoxConfig.get().isDarkTheme();
        int skyColor = dark ? SKY_COLOR_DARK : SKY_COLOR_LIGHT;
        graphics.fill(worldOriginX, worldOriginY, worldOriginX + worldPixelWidth, worldOriginY + worldPixelHeight, skyColor);

        graphics.enableScissor(worldOriginX, worldOriginY, worldOriginX + worldPixelWidth, worldOriginY + worldPixelHeight);

        for (FlappyBirdLogic.Pipe pipe : logic.getPipes()) {
            drawPipe(graphics, pipe);
        }

        drawBird(graphics);

        graphics.disableScissor();

        graphics.outline(worldOriginX, worldOriginY, worldPixelWidth, worldPixelHeight, 0xFF000000);

        if (leaderboardPanel.isVisible()) {
            int panelX = worldOriginX - ServerLeaderboardPanel.WIDTH - LEADERBOARD_GAP;
            leaderboardPanel.draw(graphics, this.font, panelX, worldOriginY);
        }

        if (logic.isGameOver()) {
            drawGameOverOverlay(graphics);
        }
    }

    private void drawPipe(GuiGraphicsExtractor graphics, FlappyBirdLogic.Pipe pipe) {
        int pipeScreenX = worldToScreenX(pipe.getX());
        int pipeScreenWidth = Math.round(FlappyBirdLogic.PIPE_WIDTH * scale);
        int gapTopScreenY = worldToScreenY(pipe.getGapTop());
        int gapBottomScreenY = worldToScreenY(pipe.getGapBottom());

        graphics.fill(pipeScreenX, worldOriginY, pipeScreenX + pipeScreenWidth, gapTopScreenY, PIPE_COLOR);
        graphics.fill(pipeScreenX, gapTopScreenY - PIPE_CAP_HEIGHT, pipeScreenX + pipeScreenWidth, gapTopScreenY, PIPE_CAP_COLOR);

        graphics.fill(pipeScreenX, gapBottomScreenY, pipeScreenX + pipeScreenWidth, worldOriginY + worldPixelHeight, PIPE_COLOR);
        graphics.fill(pipeScreenX, gapBottomScreenY, pipeScreenX + pipeScreenWidth, gapBottomScreenY + PIPE_CAP_HEIGHT, PIPE_CAP_COLOR);
    }

    private void drawBird(GuiGraphicsExtractor graphics) {
        int diameter = Math.max(6, Math.round(FlappyBirdLogic.BIRD_RADIUS * 2 * scale));
        int centerX = worldToScreenX(FlappyBirdLogic.BIRD_X);
        int centerY = worldToScreenY(logic.getBirdY());
        ItemIconRenderer.drawCentered(graphics, birdItemStack, centerX - diameter / 2, centerY - diameter / 2, diameter);
    }

    private void drawGameOverOverlay(GuiGraphicsExtractor graphics) {
        graphics.fill(worldOriginX, worldOriginY, worldOriginX + worldPixelWidth, worldOriginY + worldPixelHeight, OVERLAY_COLOR);

        String title = isNewBestScore
                ? Component.translatable("gamebox.games.flappy_bird.new_best").getString()
                : Component.translatable("gamebox.games.flappy_bird.game_over").getString();
        int titleWidth = this.font.width(title);
        graphics.text(this.font, title, worldOriginX + worldPixelWidth / 2 - titleWidth / 2,
                worldOriginY + worldPixelHeight / 2 - 10, isNewBestScore ? VICTORY_COLOR : OVERLAY_TEXT_COLOR, false);

        String hint = Component.translatable("gamebox.games.flappy_bird.restart_hint").getString();
        int hintWidth = this.font.width(hint);
        graphics.text(this.font, hint, worldOriginX + worldPixelWidth / 2 - hintWidth / 2,
                worldOriginY + worldPixelHeight / 2 + 4, OVERLAY_TEXT_COLOR, false);
    }
}