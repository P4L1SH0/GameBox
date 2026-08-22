package com.gamebox.client.games.minesweeper;

/**
 * A simple (x, y) board coordinate, local to Minesweeper. Each mini-game
 * keeps its own small coordinate type rather than sharing one across games,
 * so each game's logic module stays fully independent of the others.
 */
public record GridPosition(int x, int y) {
}