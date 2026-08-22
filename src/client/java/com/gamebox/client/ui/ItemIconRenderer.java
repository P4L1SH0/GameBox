package com.gamebox.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Small helper for drawing a vanilla item icon (native 16x16 size) scaled
 * and centered inside an arbitrary square area. Used by any mini-game that
 * reuses Minecraft's own item textures instead of drawing custom shapes.
 */
public final class ItemIconRenderer {

    private static final int NATIVE_ITEM_SIZE = 16;

    private ItemIconRenderer() {
    }

    /**
     * Draws {@code stack}'s icon centered inside the square area starting at
     * (areaX, areaY) with side length {@code areaSize}, scaled up or down
     * from its native 16x16 size as needed.
     */
    public static void drawCentered(GuiGraphicsExtractor graphics, ItemStack stack, int areaX, int areaY, int areaSize) {
        float scale = areaSize / (float) NATIVE_ITEM_SIZE;
        float offset = (areaSize - NATIVE_ITEM_SIZE * scale) / 2.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(areaX + offset, areaY + offset);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }
}