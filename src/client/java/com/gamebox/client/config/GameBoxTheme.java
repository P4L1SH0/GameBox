package com.gamebox.client.config;

/**
 * Central palette provider for GameBox's UI chrome (panels, buttons, board
 * backgrounds, generic text). Every screen asks this class for colors
 * instead of hardcoding them, so the light/dark theme setting applies
 * consistently everywhere without duplicating the light/dark logic.
 *
 * Game-specific accent colors (food/mine icons, Minesweeper number colors,
 * Snake body color, etc.) are NOT covered here on purpose - those are
 * meaningful, already-vivid colors that read fine on both themes, and
 * theming them too would blur each game's own visual identity.
 */
public final class GameBoxTheme {

    private GameBoxTheme() {
    }

    private static boolean dark() {
        return GameBoxConfig.get().isDarkTheme();
    }

    // Panel chrome (menus: main menu, difficulty select, records, settings)
    public static int panelBackgroundTop() {
        return dark() ? 0xF02A2D35 : 0xF0F4F5F7;
    }

    public static int panelBackgroundBottom() {
        return dark() ? 0xF01A1C21 : 0xF0DCE0E4;
    }

    public static int panelBorder() {
        return dark() ? 0xFF4A4F5C : 0xFFB8BFC7;
    }

    public static int titleColor() {
        return dark() ? 0xFFF0F1F3 : 0xFF2B2E33;
    }

    public static int subtitleColor() {
        return dark() ? 0xFFA9AFB9 : 0xFF6B7178;
    }

    public static int dividerColor() {
        return dark() ? 0xFF3E434F : 0xFFC7CCD2;
    }

    public static int lineColor() {
        return dark() ? 0xFFC3C8D0 : 0xFF585D64;
    }

    public static int versionColor() {
        return dark() ? 0xFF6E7480 : 0xFFA0A5AB;
    }

    public static int warningColor() {
        return dark() ? 0xFFE0895A : 0xFFB05A2E;
    }

    // Buttons
    public static int buttonBackground() {
        return dark() ? 0xFF454B58 : 0xFF3A3F47;
    }

    public static int buttonHoverBackground() {
        return dark() ? 0xFF565D6C : 0xFF4A505A;
    }

    public static int buttonDisabledBackground() {
        return dark() ? 0xFF33363D : 0xFFC7CBD1;
    }

    public static int buttonTextColor() {
        return 0xFFF4F4F4;
    }

    public static int buttonDisabledTextColor() {
        return dark() ? 0xFF6A6E76 : 0xFF888C92;
    }

    // Game boards (generic - single flat background, used by Snake, Minesweeper cells, Memory, Tic-Tac-Toe)
    public static int boardBackground() {
        return dark() ? 0xFF23262E : 0xFFE8EAED;
    }

    public static int boardBackgroundAlt() {
        return dark() ? 0xFF2B2F38 : 0xFFDCE0E4;
    }

    public static int cellBorder() {
        return dark() ? 0xFF454B58 : 0xFFB8BFC7;
    }

    public static int primaryTextColor() {
        return dark() ? 0xFFEDEEF0 : 0xFF2B2E33;
    }

    public static int selectedCellColor() {
        return dark() ? 0xFF3D5A8A : 0xFFCFE3FF;
    }

    // Sudoku's 3x3 box checkerboard needs more contrast than the generic
    // board colors above provide (those two are close together on purpose,
    // for a subtle single-panel look elsewhere) - these are dedicated,
    // higher-contrast pairs just for that pattern.
    public static int sudokuBoxBackgroundA() {
        return dark() ? 0xFF262A33 : 0xFFFFFFFF;
    }

    public static int sudokuBoxBackgroundB() {
        return dark() ? 0xFF33394A : 0xFFD8DEE6;
    }
}