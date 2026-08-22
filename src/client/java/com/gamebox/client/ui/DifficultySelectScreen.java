package com.gamebox.client.ui;

import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.core.Difficulty;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BiFunction;

/**
 * Generic difficulty-selection screen, reusable by any mini-game.
 *
 * The gameId parameter is currently unused by this screen itself (the
 * separate "Server Leaderboard" button/screen was replaced by the always-
 * visible Top 5 panel drawn directly on each game's own screen), but the
 * overloaded constructor is kept so games that already pass a gameId don't
 * need to change their call site if this screen needs it again later.
 */
public class DifficultySelectScreen extends Screen {

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final int PANEL_PADDING = 20;

    private final Screen parent;
    private final Component gameTitle;
    private final Component instructions;
    private final BiFunction<Screen, Difficulty, Screen> screenFactory;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public DifficultySelectScreen(Screen parent, Component gameTitle, Component instructions,
                                  BiFunction<Screen, Difficulty, Screen> screenFactory) {
        this(parent, gameTitle, instructions, null, screenFactory);
    }

    public DifficultySelectScreen(Screen parent, Component gameTitle, Component instructions, String gameId,
                                  BiFunction<Screen, Difficulty, Screen> screenFactory) {
        super(Component.translatable("gamebox.difficulty.select_title", gameTitle));
        this.parent = parent;
        this.gameTitle = gameTitle;
        this.instructions = instructions;
        this.screenFactory = screenFactory;
    }

    @Override
    protected void init() {
        Difficulty[] difficulties = Difficulty.values();
        int totalButtons = difficulties.length + 2;

        this.panelWidth = BUTTON_WIDTH + PANEL_PADDING * 2;
        this.panelHeight = 40 + (totalButtons * BUTTON_SPACING) + PANEL_PADDING;
        this.panelX = this.width / 2 - panelWidth / 2;
        this.panelY = this.height / 2 - panelHeight / 2;

        int buttonX = this.width / 2 - BUTTON_WIDTH / 2;
        int y = panelY + 36;

        for (Difficulty difficulty : difficulties) {
            this.addRenderableWidget(new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    difficulty.getDisplayName(),
                    () -> this.minecraft.gui.setScreen(screenFactory.apply(this, difficulty))));
            y += BUTTON_SPACING;
        }

        y += 8;
        this.addRenderableWidget(new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.common.how_to_play"),
                () -> this.minecraft.gui.setScreen(new HowToPlayScreen(this, gameTitle, instructions))));
        y += BUTTON_SPACING;

        this.addRenderableWidget(new GameBoxButton(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.common.back"), this::onClose));
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