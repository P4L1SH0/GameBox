package com.gamebox.client.games.snake;

public record GridPosition(int x, int y) {

    public GridPosition translate(int dx, int dy) {
        return new GridPosition(x + dx, y + dy);
    }
}