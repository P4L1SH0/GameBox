package com.gamebox.client.ui;

import com.gamebox.client.config.GameBoxTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic "how to play" screen, reusable by any mini-game. Each game
 * provides its instructions as a single Component whose text uses "\n" to
 * separate lines - this screen splits on that and renders each line,
 * scrollable if it doesn't fit the available height.
 */
public class HowToPlayScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_PADDING = 16;
    private static final int LINE_HEIGHT = 12;
    private static final int TITLE_AREA_HEIGHT = 30;
    private static final int BUTTON_AREA_HEIGHT = 34;
    private static final int SCREEN_MARGIN = 10;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_TRACK_COLOR = 0x40000000;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFF8C939C;

    private final Screen parent;
    private final List<String> lines = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int fullContentHeight;
    private int maxScroll;
    private int scrollOffset;

    public HowToPlayScreen(Screen parent, Component gameTitle, Component instructions) {
        super(Component.translatable("gamebox.howto.title", gameTitle));
        this.parent = parent;
        for (String line : instructions.getString().split("\n")) {
            lines.add(line);
        }
    }

    @Override
    protected void init() {
        this.fullContentHeight = Math.max(LINE_HEIGHT, lines.size() * LINE_HEIGHT);

        int maxPanelHeight = this.height - SCREEN_MARGIN * 2;
        int desiredPanelHeight = TITLE_AREA_HEIGHT + fullContentHeight + BUTTON_AREA_HEIGHT;
        this.panelHeight = Math.min(desiredPanelHeight, maxPanelHeight);

        this.panelX = this.width / 2 - PANEL_WIDTH / 2;
        this.panelY = Math.max(SCREEN_MARGIN, this.height / 2 - panelHeight / 2);

        this.contentTop = panelY + TITLE_AREA_HEIGHT;
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
                GameBoxTheme.panelBackgroundTop(), GameBoxTheme.panelBackgroundBottom());
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, GameBoxTheme.panelBorder());

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String titleText = this.getTitle().getString();
        int titleWidth = this.font.width(titleText);
        graphics.text(this.font, titleText, this.width / 2 - titleWidth / 2, panelY + 12, GameBoxTheme.titleColor(), false);

        graphics.enableScissor(panelX, contentTop, panelX + PANEL_WIDTH, contentBottom);

        int y = contentTop - scrollOffset;
        for (String line : lines) {
            graphics.text(this.font, line, panelX + PANEL_PADDING, y, GameBoxTheme.lineColor(), false);
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