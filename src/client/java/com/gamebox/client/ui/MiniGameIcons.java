package com.gamebox.client.ui;

import com.gamebox.client.games.flappybird.FlappyBirdGame;
import com.gamebox.client.games.game1010.Game1010Game;
import com.gamebox.client.games.game2048.Game2048Game;
import com.gamebox.client.games.lightsout.LightsOutGame;
import com.gamebox.client.games.mastermind.MastermindGame;
import com.gamebox.client.games.memory.MemoryGame;
import com.gamebox.client.games.minesweeper.MinesweeperGame;
import com.gamebox.client.games.simon.SimonGame;
import com.gamebox.client.games.snake.SnakeGame;
import com.gamebox.client.games.sudoku.SudokuGame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Central place mapping each mini-game's id to a representative vanilla
 * item icon, shared by the main menu buttons and the Records screen so
 * both stay visually consistent without duplicating the mapping.
 */
public final class MiniGameIcons {

    private MiniGameIcons() {
    }

    public static ItemStack forGameId(String gameId) {
        if (gameId.equals(SnakeGame.ID)) {
            return new ItemStack(Items.APPLE);
        }
        if (gameId.equals(LightsOutGame.ID)) {
            return new ItemStack(Items.REDSTONE_LAMP);
        }
        if (gameId.equals(MinesweeperGame.ID)) {
            return new ItemStack(Items.TNT);
        }
        if (gameId.equals(SudokuGame.ID)) {
            return new ItemStack(Items.PAPER);
        }
        if (gameId.equals(MemoryGame.ID)) {
            return new ItemStack(Items.DIAMOND);
        }
        if (gameId.equals(Game2048Game.ID)) {
            return new ItemStack(Items.GOLD_BLOCK);
        }
        if (gameId.equals(SimonGame.ID)) {
            return new ItemStack(Items.NOTE_BLOCK);
        }
        if (gameId.equals(MastermindGame.ID)) {
            return new ItemStack(Items.SPYGLASS);
        }
        if (gameId.equals(Game1010Game.ID)) {
            return new ItemStack(Items.JIGSAW);
        }
        if (gameId.equals(FlappyBirdGame.ID)) {
            return new ItemStack(Items.BAT_SPAWN_EGG);
        }
        return new ItemStack(Items.BOOK);
    }
}