package com.gamebox.client.ui;

import com.gamebox.GameBox;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MainMenuScreen extends Screen {

    private static final int FULL_BUTTON_WIDTH = 220;
    private static final int COLUMN_GAP = 10;
    private static final int COLUMN_BUTTON_WIDTH = (FULL_BUTTON_WIDTH - COLUMN_GAP) / 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final int PANEL_PADDING = 20;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int dividerY;

    public MainMenuScreen() {
        super(Component.translatable("gamebox.menu.title"));
    }

    @Override
    protected void init() {
        List<MiniGame> games = List.copyOf(MiniGameRegistry.getAll());
        int gameRows = (games.size() + 1) / 2;
        int utilityRows = 2; // [Records, Settings] and [Statistics, Exit]

        this.panelWidth = FULL_BUTTON_WIDTH + PANEL_PADDING * 2;
        this.panelHeight = 46 + (gameRows * BUTTON_SPACING) + 16 + (utilityRows * BUTTON_SPACING) + PANEL_PADDING;
        this.panelX = this.width / 2 - this.panelWidth / 2;
        this.panelY = this.height / 2 - this.panelHeight / 2;

        int centeredX = this.width / 2 - FULL_BUTTON_WIDTH / 2;
        int leftColumnX = centeredX;
        int rightColumnX = centeredX + COLUMN_BUTTON_WIDTH + COLUMN_GAP;
        int y = this.panelY + 46;

        for (int i = 0; i < games.size(); i += 2) {
            MiniGame leftGame = games.get(i);
            boolean hasRightGame = i + 1 < games.size();

            if (!hasRightGame) {
                this.addRenderableWidget(new GameBoxButton(centeredX, y, FULL_BUTTON_WIDTH, BUTTON_HEIGHT,
                        leftGame.getMetadata().displayName(),
                        () -> this.minecraft.gui.setScreen(leftGame.createScreen(this)),
                        MiniGameIcons.forGameId(leftGame.getMetadata().id())));
            } else {
                MiniGame rightGame = games.get(i + 1);
                this.addRenderableWidget(new GameBoxButton(leftColumnX, y, COLUMN_BUTTON_WIDTH, BUTTON_HEIGHT,
                        leftGame.getMetadata().displayName(),
                        () -> this.minecraft.gui.setScreen(leftGame.createScreen(this)),
                        MiniGameIcons.forGameId(leftGame.getMetadata().id())));
                this.addRenderableWidget(new GameBoxButton(rightColumnX, y, COLUMN_BUTTON_WIDTH, BUTTON_HEIGHT,
                        rightGame.getMetadata().displayName(),
                        () -> this.minecraft.gui.setScreen(rightGame.createScreen(this)),
                        MiniGameIcons.forGameId(rightGame.getMetadata().id())));
            }
            y += BUTTON_SPACING;
        }

        this.dividerY = y + 4;
        y += 12;

        this.addRenderableWidget(new GameBoxButton(leftColumnX, y, COLUMN_BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.menu.records"),
                () -> this.minecraft.gui.setScreen(new RecordsScreen(this))));
        this.addRenderableWidget(new GameBoxButton(rightColumnX, y, COLUMN_BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.menu.settings"),
                () -> this.minecraft.gui.setScreen(new SettingsScreen(this))));
        y += BUTTON_SPACING;

        this.addRenderableWidget(new GameBoxButton(leftColumnX, y, COLUMN_BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.menu.statistics"),
                () -> this.minecraft.gui.setScreen(new StatisticsScreen(this))));
        this.addRenderableWidget(new GameBoxButton(rightColumnX, y, COLUMN_BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gamebox.menu.exit"), this::onClose));
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

        String subtitleText = Component.translatable("gamebox.menu.subtitle").getString();
        int subtitleWidth = this.font.width(subtitleText);
        graphics.text(this.font, subtitleText, this.width / 2 - subtitleWidth / 2, panelY + 26, GameBoxTheme.subtitleColor(), false);

        graphics.horizontalLine(panelX + PANEL_PADDING, panelX + panelWidth - PANEL_PADDING, dividerY, GameBoxTheme.dividerColor());

        String version = FabricLoader.getInstance()
                .getModContainer(GameBox.MOD_ID)
                .map(container -> "v" + container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
        int versionWidth = this.font.width(version);
        graphics.text(this.font, version, panelX + panelWidth - versionWidth - 6, panelY + panelHeight - 12, GameBoxTheme.versionColor(), false);
    }
}