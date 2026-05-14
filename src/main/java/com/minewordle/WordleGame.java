package com.minewordle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class WordleGame {

    public enum TileState {
        EMPTY, CORRECT, PRESENT, ABSENT
    }

    public enum GameState {
        LOADING, PLAYING, WON, LOST, ERROR
    }

    private static final List<String> WORD_LIST;
    private static final Set<String>  VALID_WORDS;
    static {
        WORD_LIST   = loadWordList();
        VALID_WORDS = new HashSet<>(WORD_LIST);
    }

    private static List<String> loadWordList() {
        List<String> words = new ArrayList<>();
        try (InputStream in = WordleGame.class.getResourceAsStream("/assets/minewordle/wordlist.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.length() == 5) words.add(line);
            }
        } catch (Exception e) {}
        return words;
    }

    private String   solution    = null;
    private final String[]      guesses   = new String[6];
    private final TileState[][] evaluated = new TileState[6][5];
    private int       guessIndex   = 0;
    private String    currentInput = "";
    private GameState gameState    = GameState.LOADING;
    private final TileState[] keyStates   = new TileState[26];
    private boolean   practiceMode = false;

    public void setSolution(String word) {
        this.solution  = word.toUpperCase();
        this.gameState = GameState.PLAYING;
    }

    public void setError() {
        this.gameState = GameState.ERROR;
    }

    public void startPractice() {
        if (WORD_LIST.isEmpty()) { setError(); return; }
        String word = WORD_LIST.get(new Random().nextInt(WORD_LIST.size()));
        this.practiceMode = true;
        setSolution(word.toUpperCase());
    }

    public void restoreFromSave(String solution, GameState state, int guessIndex,
                                String currentInput, String[] savedGuesses,
                                TileState[][] savedEval, TileState[] savedKeys) {
        this.solution     = solution;
        this.gameState    = state;
        this.guessIndex   = guessIndex;
        this.currentInput = currentInput;
        System.arraycopy(savedGuesses, 0, this.guesses, 0, 6);
        for (int r = 0; r < 6; r++)
            if (savedEval[r] != null)
                System.arraycopy(savedEval[r], 0, this.evaluated[r], 0, 5);
        System.arraycopy(savedKeys, 0, this.keyStates, 0, 26);
    }

    public void addLetter(char c) {
        if (gameState != GameState.PLAYING) return;
        if (currentInput.length() < 5) currentInput += Character.toUpperCase(c);
    }

    public void removeLetter() {
        if (gameState != GameState.PLAYING) return;
        if (!currentInput.isEmpty())
            currentInput = currentInput.substring(0, currentInput.length() - 1);
    }

    public String submitGuess() {
        if (gameState != GameState.PLAYING)  return "NOT_PLAYING";
        if (currentInput.length() != 5)      return "NOT_ENOUGH_LETTERS";
        if (!VALID_WORDS.isEmpty() && !VALID_WORDS.contains(currentInput.toLowerCase()))
            return "NOT_A_WORD";

        String guess = currentInput.toUpperCase();
        guesses[guessIndex]   = guess;
        evaluated[guessIndex] = evaluate(guess);

        for (int i = 0; i < 5; i++) {
            int ki = guess.charAt(i) - 'A';
            TileState ts = evaluated[guessIndex][i];
            if (keyStates[ki] == null || rank(ts) > rank(keyStates[ki]))
                keyStates[ki] = ts;
        }

        guessIndex++;
        currentInput = "";

        if (guess.equals(solution)) { gameState = GameState.WON;  return "WON";  }
        if (guessIndex >= 6)        { gameState = GameState.LOST; return "LOST"; }
        return "OK";
    }

    private TileState[] evaluate(String guess) {
        TileState[] result    = new TileState[5];
        int[]       remaining = new int[26];
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == solution.charAt(i)) result[i] = TileState.CORRECT;
            else remaining[solution.charAt(i) - 'A']++;
        }
        for (int i = 0; i < 5; i++) {
            if (result[i] != null) continue;
            char c = guess.charAt(i);
            if (remaining[c - 'A'] > 0) { result[i] = TileState.PRESENT; remaining[c - 'A']--; }
            else                          result[i] = TileState.ABSENT;
        }
        return result;
    }

    private int rank(TileState s) {
        return switch (s) {
            case CORRECT -> 3; case PRESENT -> 2; case ABSENT -> 1; default -> 0;
        };
    }

    public boolean      isPracticeMode()  { return practiceMode; }
    public GameState    getGameState()    { return gameState; }
    public String       getCurrentInput() { return currentInput; }
    public String[]     getGuesses()      { return guesses; }
    public TileState[][] getEvaluated()   { return evaluated; }
    public TileState[]  getKeyStates()    { return keyStates; }
    public int          getGuessIndex()   { return guessIndex; }
    public String       getSolution()     { return solution; }
}
