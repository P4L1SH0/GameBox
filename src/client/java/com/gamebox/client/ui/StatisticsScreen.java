package com.gamebox.client.ui;

import com.gamebox.client.core.MiniGame;
import com.gamebox.client.core.MiniGameRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate statistics across every registered mini-game: total games
 * played, the most-played ("favorite") game, and a per-game breakdown.
 *
 * All numbers come from MiniGame.getTotalGamesPlayed(), which each game
 * implements from its own already-stored local records - no new data is
 * tracked for this screen.
 */
public class StatisticsScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_PADDING = 16;
    private static final int ICON_SIZE = 14;
    private static final int ICON_TEXT_GAP = 6;
    private static final int LINE_HEIGHT = 12;
    private static final int SUMMARY_LINE_HEIGHT = 14;
    private static final int SUMMARY_LINES = 2;
    private static final int SUMMARY_AREA_HEIGHT = SUMMARY_LINES * SUMMARY_LINE_HEIGHT + 10;
    private static final int TITLE_AREA_HEIGHT = 30;
    private static final int BUTTON_AREA_HEIGHT = 34;
    private static final int SCREEN_MARGIN = 10;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_TRACK_COLOR = 0x40000000;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFF8C939C;

    private final Screen parent;
    private final List<GameEntry> entries = new ArrayList<>();

    private int totalGamesPlayed;
    private String favoriteGameName;
    private int favoriteGameCount;

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int summaryTop;
    private int contentTop;
    private int contentBottom;
    private int fullContentHeight;
    private int maxScroll;
    private int scrollOffset;

    private record GameEntry(String gameName, net.minecraft.world.item.ItemStack icon, int gamesPlayed) {
    }

    public StatisticsScreen(Screen parent) {
        super(Component.translatable("gamebox.statistics.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        entries.clear();
        totalGamesPlayed = 0;
        favoriteGameName = null;
        favoriteGameCount = 0;

        for (MiniGame game : MiniGameRegistry.getAll()) {
            int played = game.getTotalGamesPlayed();
            totalGamesPlayed += played;
            entries.add(new GameEntry(
                    game.getMetadata().displayName().getString(),
                    MiniGameIcons.forGameId(game.getMetadata().id()),
                    played
            ));
            if (played > favoriteGameCount) {
                favoriteGameCount = played;
                favoriteGameName = game.getMetadata().displayName().getString();
            }
        }

        this.fullContentHeight = Math.max(LINE_HEIGHT, entries.size() * LINE_HEIGHT);

        int maxPanelHeight = this.height - SCREEN_MARGIN * 2;
        int desiredPanelHeight = TITLE_AREA_HEIGHT + SUMMARY_AREA_HEIGHT + fullContentHeight + BUTTON_AREA_HEIGHT;
        this.panelHeight = Math.min(desiredPanelHeight, maxPanelHeight);

        this.panelX = this.width / 2 - PANEL_WIDTH / 2;
        this.panelY = Math.max(SCREEN_MARGIN, this.height / 2 - panelHeight / 2);

        this.summaryTop = panelY + TITLE_AREA_HEIGHT;
        this.contentTop = summaryTop + SUMMARY_AREA_HEIGHT;
        this.contentBottom = panelY + panelHeight - BUTTON_AREA_HEIGHT;

        int viewportHeight = contentBottom - contentTop;
        this.maxScroll = Math.max(0, fullContentHeight - viewportHeight);
        this.scrollOffset = 0;

        this.addRenderableWidget(new GameBoxButton(
                this.width / 2 - 50, panelY + panelHeight - 26, 100, 20,
                Component.translatable("gamebox.common.back"), this::onClose
        ));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int scrollStep = LINE_HEIGHT * 3;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * scrollStep));
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight,
                com.gamebox.client.config.GameBoxTheme.panelBackgroundTop(), com.gamebox.client.config.GameBoxTheme.panelBackgroundBottom());
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, com.gamebox.client.config.GameBoxTheme.panelBorder());

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String titleText = this.getTitle().getString();
        int titleWidth = this.font.width(titleText);
        graphics.text(this.font, titleText, this.width / 2 - titleWidth / 2, panelY + 12,
                com.gamebox.client.config.GameBoxTheme.titleColor(), false);

        String totalLine = Component.translatable("gamebox.statistics.total_games", totalGamesPlayed).getString();
        graphics.text(this.font, totalLine, panelX + PANEL_PADDING, summaryTop, com.gamebox.client.config.GameBoxTheme.titleColor(), false);

        String favoriteLine = (favoriteGameName == null)
                ? Component.translatable("gamebox.statistics.favorite_game_empty").getString()
                : Component.translatable("gamebox.statistics.favorite_game", favoriteGameName, favoriteGameCount).getString();
        graphics.text(this.font, favoriteLine, panelX + PANEL_PADDING, summaryTop + SUMMARY_LINE_HEIGHT,
                com.gamebox.client.config.GameBoxTheme.lineColor(), false);

        graphics.horizontalLine(panelX + PANEL_PADDING, panelX + PANEL_WIDTH - PANEL_PADDING,
                contentTop - 4, com.gamebox.client.config.GameBoxTheme.dividerColor());

        graphics.enableScissor(panelX, contentTop, panelX + PANEL_WIDTH, contentBottom);

        int y = contentTop - scrollOffset;
        int textX = panelX + PANEL_PADDING + ICON_SIZE + ICON_TEXT_GAP;

        for (GameEntry entry : entries) {
            ItemIconRenderer.drawCentered(graphics, entry.icon(), panelX + PANEL_PADDING, y - 1, ICON_SIZE);
            String line = Component.translatable("gamebox.statistics.per_game_line", entry.gameName(), entry.gamesPlayed()).getString();
            graphics.text(this.font, line, textX, y, com.gamebox.client.config.GameBoxTheme.lineColor(), false);
            y += LINE_HEIGHT;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(graphics);
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        int trackHeight = contentBottom - contentTop;
        int trackX = panelX + PANEL_WIDTH - PANEL_PADDING / 2 - SCROLLBAR_WIDTH;

        graphics.fill(trackX, contentTop, trackX + SCROLLBAR_WIDTH, contentBottom, SCROLLBAR_TRACK_COLOR);

        int thumbHeight = Math.max(10, (int) ((float) trackHeight / fullContentHeight * trackHeight));
        int scrollableTrack = trackHeight - thumbHeight;
        int thumbY = contentTop + (maxScroll == 0 ? 0 : (int) ((float) scrollOffset / maxScroll * scrollableTrack));

        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
    }
}