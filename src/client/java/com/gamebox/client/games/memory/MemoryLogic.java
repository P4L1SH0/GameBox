package com.gamebox.client.games.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Memory (a.k.a. Concentration / Pairs). Contains no
 * Minecraft classes or rendering code on purpose, so it can be understood,
 * modified and tested in complete isolation from the game engine.
 *
 * Cards are identified by symbolId (0..pairCount-1), with each symbolId
 * appearing on exactly two cards. The screen layer decides how to actually
 * draw each symbolId (icon, color, letter, etc.) - this class only tracks
 * which symbol is where and which cards are flipped/matched.
 */
public class MemoryLogic {

    private final MemoryDifficultySettings settings;

    private int[] cardSymbols;
    private boolean[] matched;
    private final List<Integer> currentlyFlipped = new ArrayList<>();

    private int moveCount;
    private int matchedPairs;

    public MemoryLogic(MemoryDifficultySettings settings) {
        this(settings, new Random().nextLong());
    }

    public MemoryLogic(MemoryDifficultySettings settings, long seed) {
        this.settings = settings;
        restart(seed);
    }

    public void restart() {
        restart(new Random().nextLong());
    }

    private void restart(long seed) {
        int cardCount = settings.pairCount() * 2;
        List<Integer> symbols = new ArrayList<>(cardCount);
        for (int symbolId = 0; symbolId < settings.pairCount(); symbolId++) {
            symbols.add(symbolId);
            symbols.add(symbolId);
        }
        Collections.shuffle(symbols, new Random(seed));

        this.cardSymbols = new int[cardCount];
        for (int i = 0; i < cardCount; i++) {
            this.cardSymbols[i] = symbols.get(i);
        }

        this.matched = new boolean[cardCount];
        this.currentlyFlipped.clear();
        this.moveCount = 0;
        this.matchedPairs = 0;
    }

    /**
     * Flips the card at {@code index}. If two mismatched cards were already
     * flipped, they are turned back face-down first. Does nothing if the
     * index is already matched, already flipped, or the game is won.
     *
     * @return true if this flip completed a pair-check (i.e. this was the
     * second card flipped), so the caller can react (sound, etc.) to a
     * match or mismatch just having been decided.
     */
    public boolean flip(int index) {
        if (isWon() || matched[index] || currentlyFlipped.contains(index)) {
            return false;
        }

        if (currentlyFlipped.size() == 2) {
            currentlyFlipped.clear();
        }

        currentlyFlipped.add(index);

        if (currentlyFlipped.size() < 2) {
            return false;
        }

        moveCount++;
        int firstIndex = currentlyFlipped.get(0);
        int secondIndex = currentlyFlipped.get(1);

        if (cardSymbols[firstIndex] == cardSymbols[secondIndex]) {
            matched[firstIndex] = true;
            matched[secondIndex] = true;
            matchedPairs++;
            currentlyFlipped.clear();
        }
        return true;
    }

    /**
     * @return true if the last pair-check (the second flip in a pair)
     * resulted in a match. Only meaningful right after {@link #flip(int)}
     * returns true.
     */
    public boolean wasLastPairAMatch() {
        return currentlyFlipped.isEmpty();
    }

    public boolean isFaceUp(int index) {
        return matched[index] || currentlyFlipped.contains(index);
    }

    public boolean isMatched(int index) {
        return matched[index];
    }

    public int getSymbolId(int index) {
        return cardSymbols[index];
    }

    public int getCardCount() {
        return cardSymbols.length;
    }

    public int getPairCount() {
        return settings.pairCount();
    }

    public int getMatchedPairs() {
        return matchedPairs;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public boolean isWon() {
        return matchedPairs == settings.pairCount();
    }

    public MemoryDifficultySettings getSettings() {
        return settings;
    }
}