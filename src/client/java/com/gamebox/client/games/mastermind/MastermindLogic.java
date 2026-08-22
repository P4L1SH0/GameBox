package com.gamebox.client.games.mastermind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Mastermind. Contains no Minecraft classes or
 * rendering code on purpose, so it can be understood, modified and tested
 * in complete isolation from the game engine.
 */
public class MastermindLogic {

    public enum PegStatus {
        EXACT, COLOR_MATCH, NONE
    }

    public record GuessResult(int[] guess, int exactMatches, int colorMatches, PegStatus[] positionStatus) {
    }

    private final MastermindDifficultySettings settings;
    private final Random random;

    private int[] secret;
    private final List<GuessResult> guesses = new ArrayList<>();
    private boolean won;

    public MastermindLogic(MastermindDifficultySettings settings) {
        this(settings, new Random().nextLong());
    }

    public MastermindLogic(MastermindDifficultySettings settings, long seed) {
        this.settings = settings;
        this.random = new Random(seed);
        restart();
    }

    public void restart() {
        this.secret = generateSecret();
        this.guesses.clear();
        this.won = false;
    }

    private int[] generateSecret() {
        int length = settings.codeLength();
        int colorCount = settings.colorCount();
        int[] code = new int[length];

        if (settings.allowRepeats()) {
            for (int i = 0; i < length; i++) {
                code[i] = random.nextInt(colorCount);
            }
        } else {
            List<Integer> colors = new ArrayList<>();
            for (int c = 0; c < colorCount; c++) {
                colors.add(c);
            }
            Collections.shuffle(colors, random);
            for (int i = 0; i < length; i++) {
                code[i] = colors.get(i);
            }
        }
        return code;
    }

    public GuessResult submitGuess(int[] guess) {
        if (isFinished() || guess.length != settings.codeLength()) {
            return null;
        }

        int length = secret.length;
        boolean[] secretConsumed = new boolean[length];
        boolean[] guessConsumed = new boolean[length];
        PegStatus[] positionStatus = new PegStatus[length];

        int exactMatches = 0;
        for (int i = 0; i < length; i++) {
            if (guess[i] == secret[i]) {
                exactMatches++;
                secretConsumed[i] = true;
                guessConsumed[i] = true;
                positionStatus[i] = PegStatus.EXACT;
            }
        }

        int colorMatches = 0;
        for (int i = 0; i < length; i++) {
            if (guessConsumed[i]) {
                continue;
            }
            boolean found = false;
            for (int j = 0; j < length; j++) {
                if (!secretConsumed[j] && guess[i] == secret[j]) {
                    colorMatches++;
                    secretConsumed[j] = true;
                    positionStatus[i] = PegStatus.COLOR_MATCH;
                    found = true;
                    break;
                }
            }
            if (!found) {
                positionStatus[i] = PegStatus.NONE;
            }
        }

        GuessResult result = new GuessResult(guess.clone(), exactMatches, colorMatches, positionStatus);
        guesses.add(result);

        if (exactMatches == settings.codeLength()) {
            won = true;
        }

        return result;
    }

    // --- Read-only getters for the Minecraft-side screen ---

    public List<GuessResult> getGuesses() {
        return List.copyOf(guesses);
    }

    public int getAttemptsUsed() {
        return guesses.size();
    }

    public int getAttemptsRemaining() {
        return settings.maxAttempts() - guesses.size();
    }

    public boolean isWon() {
        return won;
    }

    public boolean isOutOfAttempts() {
        return !won && guesses.size() >= settings.maxAttempts();
    }

    public boolean isFinished() {
        return won || isOutOfAttempts();
    }

    public int[] getSecret() {
        return secret.clone();
    }

    public MastermindDifficultySettings getSettings() {
        return settings;
    }
}