package com.gamebox.client.ui;

import com.gamebox.client.audio.GameBoxSounds;
import com.gamebox.client.config.GameBoxTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * A flat, custom-colored button used instead of the vanilla Button widget,
 * so its color stays fully under our control regardless of Minecraft's
 * current UI theme, and remains clearly visible against GameBox's panel.
 * Colors come from GameBoxTheme so it automatically follows the light/dark
 * theme setting.
 */
public class GameBoxButton extends AbstractWidget {

    private static final int ICON_PADDING = 4;
    private static final int ICON_TEXT_GAP = 4;
    private static final int MAX_ICON_SIZE = 14;

    private final Runnable onPress;
    private final ItemStack icon;

    public GameBoxButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        this(x, y, width, height, message, onPress, null);
    }

    public GameBoxButton(int x, int y, int width, int height, Component message, Runnable onPress, ItemStack icon) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.icon = icon;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int backgroundColor = !this.active
                ? GameBoxTheme.buttonDisabledBackground()
                : (this.isHovered() ? GameBoxTheme.buttonHoverBackground() : GameBoxTheme.buttonBackground());
        int textColor = this.active ? GameBoxTheme.buttonTextColor() : GameBoxTheme.buttonDisabledTextColor();

        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, backgroundColor);

        String text = this.getMessage().getString();

        if (icon != null) {
            int iconSize = Math.min(this.height - 6, MAX_ICON_SIZE);
            int iconY = getY() + (this.height - iconSize) / 2;
            ItemIconRenderer.drawCentered(graphics, icon, getX() + ICON_PADDING, iconY, iconSize);

            int textX = getX() + ICON_PADDING + iconSize + ICON_TEXT_GAP;
            int availableTextWidth = getX() + this.width - textX - 4;
            String displayText = truncateToFit(text, availableTextWidth);

            graphics.text(Minecraft.getInstance().font, displayText,
                    textX, getY() + (this.height - 8) / 2, textColor, false);
        } else {
            int textWidth = Minecraft.getInstance().font.width(text);
            graphics.text(Minecraft.getInstance().font, text,
                    getX() + this.width / 2 - textWidth / 2,
                    getY() + (this.height - 8) / 2,
                    textColor, false);
        }
    }

    private String truncateToFit(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        StringBuilder truncated = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(truncated.toString() + c) + ellipsisWidth > maxWidth) {
                break;
            }
            truncated.append(c);
        }
        return truncated + ellipsis;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.active && this.visible && this.isMouseOver(event.x(), event.y())) {
            GameBoxSounds.play(SoundEvents.UI_BUTTON_CLICK);
            if (this.onPress != null) {
                this.onPress.run();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // Accessibility narration can be expanded later; skipped for now for simplicity.
    }
}