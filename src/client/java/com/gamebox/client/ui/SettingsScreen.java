package com.gamebox.client.ui;

import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final int PANEL_PADDING = 20;

    private final Screen parent;
    private final GameBoxConfig config = GameBoxConfig.get();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private GameBoxButton themeButton;
    private GameBoxButton scoreOverlayButton;
    private GameBoxButton soundButton;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("gamebox.menu.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int totalButtons = 5; // theme, score overlay, sound, controls, back
        this.panelWidth = BUTTON_WIDTH + PANEL_PADDING * 2;
        this.panelHeight = 40 + (totalButtons * BUTTON_SPACING) + PANEL_PADDING;
        this.panelX = this.width / 2 - panelWidth / 2;
        this.panelY = this.height / 2 - panelHeight / 2;

        int buttonX = this.width / 2 - BUTTON_WIDTH / 2;
        int y = panelY + 36;

        this.themeButton = new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                buildThemeLabel(), this::toggleTheme);
        this.addRenderableWidget(this.themeButton);
        y += BUTTON_SPACING;

        this.scoreOverlayButton = new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                buildScoreOverlayLabel(), this::toggleScoreOverlay);
        this.addRenderableWidget(this.scoreOverlayButton);
        y += BUTTON_SPACING;

        this.soundButton = new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                buildSoundLabel(), this::toggleSound);
        this.addRenderableWidget(this.soundButton);
        y += BUTTON_SPACING;

        this.addRenderableWidget(new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.settings.open_controls"),
                () -> this.minecraft.gui.setScreen(new ControlsScreen(this, Minecraft.getInstance().options))));
        y += BUTTON_SPACING + 8;

        this.addRenderableWidget(new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.common.back"), this::onClose));
    }

    private void toggleTheme() {
        config.setDarkTheme(!config.isDarkTheme());
        config.save();
        themeButton.setMessage(buildThemeLabel());
    }

    private void toggleScoreOverlay() {
        config.setShowScoreOverlay(!config.isShowScoreOverlay());
        config.save();
        scoreOverlayButton.setMessage(buildScoreOverlayLabel());
    }

    private void toggleSound() {
        config.setSoundEnabled(!config.isSoundEnabled());
        config.save();
        soundButton.setMessage(buildSoundLabel());
    }

    private Component buildThemeLabel() {
        Component theme = config.isDarkTheme()
                ? Component.translatable("gamebox.settings.theme_dark")
                : Component.translatable("gamebox.settings.theme_light");
        return Component.translatable("gamebox.settings.theme", theme);
    }

    private Component buildScoreOverlayLabel() {
        Component state = config.isShowScoreOverlay()
                ? Component.translatable("gamebox.settings.on")
                : Component.translatable("gamebox.settings.off");
        return Component.translatable("gamebox.settings.show_score", state);
    }

    private Component buildSoundLabel() {
        Component state = config.isSoundEnabled()
                ? Component.translatable("gamebox.settings.on")
                : Component.translatable("gamebox.settings.off");
        return Component.translatable("gamebox.settings.sound", state);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                GameBoxTheme.panelBackgroundTop(), GameBoxTheme.panelBackgroundBottom());
        graphics.outline(panelX, panelY, panelWidth, panelHeight, GameBoxTheme.panelBorder());

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String titleText = this.getTitle().getString();
        int titleWidth = this.font.width(titleText);
        graphics.text(this.font, titleText, this.width / 2 - titleWidth / 2, panelY + 12, GameBoxTheme.titleColor(), false);
    }
}