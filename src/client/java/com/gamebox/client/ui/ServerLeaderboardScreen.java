package com.gamebox.client.ui;

import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.core.Difficulty;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.network.ClientLeaderboardEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Generic server leaderboard screen, reusable by any mini-game that passes
 * a gameId to DifficultySelectScreen. Shows the top entries for whichever
 * difficulty tab is selected, requesting fresh data from the server each
 * time the tab changes.
 *
 * Shows only the top MAX_ENTRIES_SHOWN results for simplicity - the server
 * itself keeps up to 100 per difficulty, but a short top list is enough
 * for a first version of this feature.
 */
public class ServerLeaderboardScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_PADDING = 16;
    private static final int LINE_HEIGHT = 12;
    private static final int TAB_BUTTON_WIDTH = 58;
    private static final int TAB_BUTTON_HEIGHT = 16;
    private static final int TAB_GAP = 4;
    private static final int TITLE_AREA_HEIGHT = 26;
    private static final int TAB_AREA_HEIGHT = 22;
    private static final int MAX_ENTRIES_SHOWN = 10;

    private final Screen parent;
    private final String gameId;

    private Difficulty selectedDifficulty = Difficulty.NORMAL;
    private List<ClientLeaderboardEntry> entries = List.of();
    private boolean loaded;
    private int requestGeneration;

    private int panelX;
    private int panelY;
    private int panelHeight;

    public ServerLeaderboardScreen(Screen parent, String gameId, Component gameTitle) {
        super(Component.translatable("gamebox.leaderboard.title", gameTitle));
        this.parent = parent;
        this.gameId = gameId;
    }

    @Override
    protected void init() {
        this.panelHeight = TITLE_AREA_HEIGHT + TAB_AREA_HEIGHT + MAX_ENTRIES_SHOWN * LINE_HEIGHT + PANEL_PADDING + 30;
        this.panelX = this.width / 2 - PANEL_WIDTH / 2;
        this.panelY = this.height / 2 - panelHeight / 2;

        Difficulty[] difficulties = Difficulty.values();
        int tabsWidth = difficulties.length * TAB_BUTTON_WIDTH + (difficulties.length - 1) * TAB_GAP;
        int tabX = this.width / 2 - tabsWidth / 2;
        int tabY = panelY + TITLE_AREA_HEIGHT;

        for (Difficulty difficulty : difficulties) {
            Difficulty capturedDifficulty = difficulty;
            this.addRenderableWidget(new GameBoxButton(tabX, tabY, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT,
                    difficulty.getDisplayName(), () -> selectDifficulty(capturedDifficulty)));
            tabX += TAB_BUTTON_WIDTH + TAB_GAP;
        }

        this.addRenderableWidget(new GameBoxButton(
                this.width / 2 - 50, panelY + panelHeight - 26, 100, 20,
                Component.translatable("gamebox.common.back"), this::onClose));

        requestLeaderboard();
    }

    private void selectDifficulty(Difficulty difficulty) {
        if (difficulty == selectedDifficulty) {
            return;
        }
        this.selectedDifficulty = difficulty;
        requestLeaderboard();
    }

    private void requestLeaderboard() {
        this.loaded = false;
        this.entries = List.of();
        int generation = ++requestGeneration;
        GameBoxLeaderboardClient.requestLeaderboard(gameId, selectedDifficulty.name(), result -> {
            // Ignore stale responses for a difficulty the player has since switched away from.
            if (generation == requestGeneration) {
                this.entries = result;
                this.loaded = true;
            }
        });
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight,
                GameBoxTheme.panelBackgroundTop(), GameBoxTheme.panelBackgroundBottom());
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, GameBoxTheme.panelBorder());

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String titleText = this.getTitle().getString();
        int titleWidth = this.font.width(titleText);
        graphics.text(this.font, titleText, this.width / 2 - titleWidth / 2, panelY + 10, GameBoxTheme.titleColor(), false);

        int y = panelY + TITLE_AREA_HEIGHT + TAB_AREA_HEIGHT + 4;
        int textX = panelX + PANEL_PADDING;

        if (!loaded) {
            String loadingText = Component.translatable("gamebox.leaderboard.loading").getString();
            graphics.text(this.font, loadingText, textX, y, GameBoxTheme.lineColor(), false);
            return;
        }

        if (entries.isEmpty()) {
            String emptyText = Component.translatable("gamebox.leaderboard.empty").getString();
            graphics.text(this.font, emptyText, textX, y, GameBoxTheme.lineColor(), false);
            return;
        }

        int rank = 1;
        for (ClientLeaderboardEntry entry : entries) {
            if (rank > MAX_ENTRIES_SHOWN) {
                break;
            }
            String line = Component.translatable("gamebox.leaderboard.entry", rank, entry.getPlayerName(), entry.getValue()).getString();
            graphics.text(this.font, line, textX, y, GameBoxTheme.lineColor(), false);
            y += LINE_HEIGHT;
            rank++;
        }
    }
}