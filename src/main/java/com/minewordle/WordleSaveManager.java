package com.minewordle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;

public class WordleSaveManager {

    private static final Gson   GSON      = new Gson();
    private static final String FILE_NAME = "minewordle_save.json";
    private static final WordleGame.TileState[] TS_VALS = WordleGame.TileState.values();

    private static Path savePath() {
        return FabricLoader.getInstance().getGameDir().resolve(FILE_NAME);
    }

    public static void save(WordleGame game) {
        if (game.isPracticeMode()) return;
        WordleGame.GameState state = game.getGameState();
        if (state == WordleGame.GameState.LOADING || state == WordleGame.GameState.ERROR) return;

        try (Writer w = new FileWriter(savePath().toFile())) {
            JsonObject json = new JsonObject();
            json.addProperty("date",         today());
            json.addProperty("solution",     game.getSolution() != null ? game.getSolution() : "");
            json.addProperty("state",        state.name());
            json.addProperty("guessIndex",   game.getGuessIndex());
            json.addProperty("currentInput", game.getCurrentInput());

            JsonArray guessArr = new JsonArray();
            String[]  guesses  = game.getGuesses();
            for (int i = 0; i < game.getGuessIndex(); i++)
                guessArr.add(guesses[i] != null ? guesses[i] : "");
            json.add("guesses", guessArr);

            JsonArray              evalArr = new JsonArray();
            WordleGame.TileState[][] eval  = game.getEvaluated();
            for (int r = 0; r < game.getGuessIndex(); r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < 5; c++) sb.append(eval[r][c] != null ? eval[r][c].ordinal() : 0);
                evalArr.add(sb.toString());
            }
            json.add("evaluated", evalArr);

            JsonArray ksArr = new JsonArray();
            for (WordleGame.TileState ts : game.getKeyStates())
                ksArr.add(ts != null ? ts.ordinal() : -1);
            json.add("keyStates", ksArr);

            GSON.toJson(json, w);
        } catch (Exception ignored) {}
    }

    public static boolean load(WordleGame game) {
        Path path = savePath();
        if (!path.toFile().exists()) return false;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject json = GSON.fromJson(r, JsonObject.class);
            if (!today().equals(json.get("date").getAsString())) return false;

            String solution = json.get("solution").getAsString();
            if (solution.isEmpty()) return false;

            WordleGame.GameState state = WordleGame.GameState.valueOf(json.get("state").getAsString());
            int    guessIndex   = json.get("guessIndex").getAsInt();
            String currentInput = json.get("currentInput").getAsString();

            String[] guesses = new String[6];
            JsonArray guessArr = json.getAsJsonArray("guesses");
            for (int i = 0; i < guessIndex && i < guessArr.size(); i++) {
                String g = guessArr.get(i).getAsString();
                guesses[i] = g.isEmpty() ? null : g;
            }

            WordleGame.TileState[][] evaluated = new WordleGame.TileState[6][5];
            JsonArray evalArr = json.getAsJsonArray("evaluated");
            for (int row = 0; row < guessIndex && row < evalArr.size(); row++) {
                String s = evalArr.get(row).getAsString();
                for (int col = 0; col < 5; col++)
                    evaluated[row][col] = TS_VALS[Character.getNumericValue(s.charAt(col))];
            }

            WordleGame.TileState[] keyStates = new WordleGame.TileState[26];
            JsonArray ksArr = json.getAsJsonArray("keyStates");
            for (int i = 0; i < 26 && i < ksArr.size(); i++) {
                int ord = ksArr.get(i).getAsInt();
                keyStates[i] = ord < 0 ? null : TS_VALS[ord];
            }

            game.restoreFromSave(solution, state, guessIndex, currentInput, guesses, evaluated, keyStates);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static String today() {
        return LocalDate.now(ZoneId.systemDefault()).toString();
    }
}
