package com.gamebox.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A small contained bar for showing score/time-style HUD text above a game
 * board, used by every mini-game screen instead of loose floating text.
 * Reserves a fixed height regardless of whether left/right text is present,
 * so board layout stays stable across screens.
 */
public final class HudBar {

    private static final int BACKGROUND_COLOR = 0xA0000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BAR_HEIGHT = 16;
    private static final int GAP_BELOW = 4;

    private HudBar() {
    }

    public static int height() {
        return BAR_HEIGHT;
    }

    public static int reservedHeight() {
        return BAR_HEIGHT + GAP_BELOW;
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, String leftText, String rightText) {
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, BACKGROUND_COLOR);
        if (leftText != null && !leftText.isEmpty()) {
            graphics.text(font, leftText, x + 6, y + 4, TEXT_COLOR, false);
        }
        if (rightText != null && !rightText.isEmpty()) {
            int rightWidth = font.width(rightText);
            graphics.text(font, rightText, x + width - rightWidth - 6, y + 4, TEXT_COLOR, false);
        }
    }
}