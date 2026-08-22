package com.gamebox.client.ui;

import com.gamebox.client.config.GameBoxConfig;
import com.gamebox.client.config.GameBoxTheme;
import com.gamebox.client.network.GameBoxClientNetworking;
import com.gamebox.client.network.GameBoxLeaderboardClient;
import com.gamebox.network.ClientLeaderboardEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A small, reusable "Top 5" server leaderboard panel that a game screen can
 * draw to one side of its board. Only ever shows anything when connected to
 * a real remote server that supports GameBox (see GameBoxClientNetworking) -
 * otherwise isVisible() is false and the game screen should skip drawing it
 * and reserving space for it entirely.
 *
 * By design this only refreshes when explicitly told to (refresh(...)),
 * not continuously - the intended usage is once when a game screen opens,
 * and once more right after a score is submitted at the end of a game.
 *
 * Each entry row shows a small face icon, "rank. name" left-aligned, and
 * the score right-aligned, like a small table. The face icon only shows
 * for players CURRENTLY connected to the same server (matched by UUID
 * against the live player list, the same data the vanilla tab list uses) -
 * a player who has logged off shows a plain placeholder square instead,
 * since there's no safe way to fetch their skin otherwise. Faces are drawn
 * via PlayerFaceExtractor, the same vanilla utility the tab list uses, so
 * texture loading is handled correctly instead of manually cropping the
 * skin texture ourselves. The name truncates with an ellipsis if it
 * doesn't fit - the rank and score are never cut. The top 3 ranks get a
 * medal-colored accent (gold/silver/bronze).
 */
public class ServerLeaderboardPanel {

    public static final int WIDTH = 130;
    private static final int PADDING = 6;
    private static final int TITLE_HEIGHT = 14;
    private static final int LINE_HEIGHT = 11;
    private static final int MAX_ENTRIES = 5;
    private static final int TITLE_UNDERLINE_GAP = 2;
    private static final int FACE_SIZE = 8;
    private static final int FACE_GAP = 4;

    private static final int GOLD_COLOR = 0xFFFFD700;
    private static final int SILVER_COLOR = 0xFFC7CCD2;
    private static final int BRONZE_COLOR = 0xFFCD7F32;
    private static final int FACE_PLACEHOLDER_COLOR_LIGHT = 0xFFC7CCD2;
    private static final int FACE_PLACEHOLDER_COLOR_DARK = 0xFF454B58;

    private List<ClientLeaderboardEntry> entries = List.of();
    private boolean loaded;
    private int requestGeneration;

    public boolean isVisible() {
        return GameBoxClientNetworking.isServerAvailable();
    }

    /**
     * Requests a fresh copy of the top scores for (gameId, difficultyKey).
     * Safe to call even when isVisible() is false - simply does nothing.
     */
    public void refresh(String gameId, String difficultyKey) {
        if (!isVisible()) {
            return;
        }
        this.loaded = false;
        this.entries = List.of();
        int generation = ++requestGeneration;
        GameBoxLeaderboardClient.requestLeaderboard(gameId, difficultyKey, result -> {
            if (generation == requestGeneration) {
                this.entries = result;
                this.loaded = true;
            }
        });
    }

    public int getHeight() {
        return TITLE_HEIGHT + TITLE_UNDERLINE_GAP + MAX_ENTRIES * LINE_HEIGHT + PADDING * 2;
    }

    public void draw(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        if (!isVisible()) {
            return;
        }

        int height = getHeight();
        graphics.fillGradient(x, y, x + WIDTH, y + height,
                GameBoxTheme.panelBackgroundTop(), GameBoxTheme.panelBackgroundBottom());
        graphics.outline(x, y, WIDTH, height, GameBoxTheme.panelBorder());

        int contentLeft = x + PADDING;
        int contentRight = x + WIDTH - PADDING;
        int contentWidth = contentRight - contentLeft;

        String title = Component.translatable("gamebox.leaderboard.panel_title").getString();
        int titleWidth = font.width(title);
        graphics.text(font, title, x + WIDTH / 2 - titleWidth / 2, y + PADDING, GameBoxTheme.titleColor(), false);

        int underlineY = y + PADDING + TITLE_HEIGHT;
        graphics.horizontalLine(contentLeft, contentRight, underlineY, GameBoxTheme.dividerColor());

        int lineY = underlineY + TITLE_UNDERLINE_GAP + 2;

        if (!loaded) {
            drawWrapped(graphics, font, Component.translatable("gamebox.leaderboard.loading").getString(),
                    contentLeft, lineY, contentWidth);
            return;
        }

        if (entries.isEmpty()) {
            drawWrapped(graphics, font, Component.translatable("gamebox.leaderboard.empty").getString(),
                    contentLeft, lineY, contentWidth);
            return;
        }

        int rank = 1;
        for (ClientLeaderboardEntry entry : entries) {
            if (rank > MAX_ENTRIES) {
                break;
            }
            drawEntryRow(graphics, font, rank, entry, contentLeft, contentRight, lineY);
            lineY += LINE_HEIGHT;
            rank++;
        }
    }

    private void drawEntryRow(GuiGraphicsExtractor graphics, Font font, int rank, ClientLeaderboardEntry entry,
                              int contentLeft, int contentRight, int lineY) {
        int rankColor = medalColorFor(rank);

        int faceY = lineY - 1;
        PlayerSkin skin = findOnlineSkin(entry.getPlayerId());
        if (skin != null) {
            PlayerFaceExtractor.extractRenderState(graphics, skin, contentLeft, faceY, FACE_SIZE);
        } else {
            boolean dark = GameBoxConfig.get().isDarkTheme();
            int placeholderColor = dark ? FACE_PLACEHOLDER_COLOR_DARK : FACE_PLACEHOLDER_COLOR_LIGHT;
            graphics.fill(contentLeft, faceY, contentLeft + FACE_SIZE, faceY + FACE_SIZE, placeholderColor);
        }

        int textLeft = contentLeft + FACE_SIZE + FACE_GAP;

        String scoreText = String.valueOf(entry.getValue());
        int scoreWidth = font.width(scoreText);
        int scoreX = contentRight - scoreWidth;

        String prefix = rank + ". ";
        int prefixWidth = font.width(prefix);
        int nameGap = 4;
        int availableForName = scoreX - nameGap - (textLeft + prefixWidth);

        String name = truncateToFit(font, entry.getPlayerName(), Math.max(0, availableForName));

        graphics.text(font, prefix, textLeft, lineY, rankColor, false);
        graphics.text(font, name, textLeft + prefixWidth, lineY, GameBoxTheme.lineColor(), false);
        graphics.text(font, scoreText, scoreX, lineY, GameBoxTheme.titleColor(), false);
    }

    /**
     * Looks up the PlayerSkin for a leaderboard entry's UUID among players
     * currently connected to the same server (the same source the vanilla
     * tab list uses) - returns null if that player isn't online right now,
     * the UUID is malformed, or we're not in a multiplayer session at all.
     */
    private PlayerSkin findOnlineSkin(String playerIdString) {
        if (playerIdString == null || playerIdString.isEmpty()) {
            return null;
        }
        UUID playerId;
        try {
            playerId = UUID.fromString(playerIdString);
        } catch (IllegalArgumentException e) {
            return null;
        }

        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return null;
        }
        var info = connection.getPlayerInfo(playerId);
        return info == null ? null : info.getSkin();
    }

    private int medalColorFor(int rank) {
        return switch (rank) {
            case 1 -> GOLD_COLOR;
            case 2 -> SILVER_COLOR;
            case 3 -> BRONZE_COLOR;
            default -> GameBoxTheme.lineColor();
        };
    }

    private String truncateToFit(Font font, String text, int maxWidth) {
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

    private void drawWrapped(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int maxWidth) {
        List<String> lines = wrapText(font, text, maxWidth);
        int lineY = y;
        for (String line : lines) {
            graphics.text(font, line, x, lineY, GameBoxTheme.lineColor(), false);
            lineY += LINE_HEIGHT;
        }
    }

    private List<String> wrapText(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(candidate) <= maxWidth || currentLine.isEmpty()) {
                currentLine = new StringBuilder(candidate);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }
}