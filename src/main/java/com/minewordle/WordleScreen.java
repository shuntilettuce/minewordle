package com.minewordle;

import com.minewordle.WordleGame.GameState;
import com.minewordle.WordleGame.TileState;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class WordleScreen extends Screen {

    // Colors (ARGB)
    private static final int C_CORRECT       = 0xFF538D4E;
    private static final int C_PRESENT       = 0xFFB59F3B;
    private static final int C_ABSENT        = 0xFF3A3A3C;
    private static final int C_TILE_EMPTY    = 0xFF121213;
    private static final int C_BORDER        = 0xFF3A3A3C;
    private static final int C_BORDER_ACTIVE = 0xFF878A8C;
    private static final int C_KEY_DEFAULT   = 0xFF818384;
    private static final int C_WHITE         = 0xFFFFFFFF;
    private static final int C_GREEN         = 0xFF538D4E;
    private static final int C_RED           = 0xFFFF5555;
    private static final int C_GRAY          = 0xFFAAAAAA;
    private static final int C_PANEL_BG      = 0xD0121213;
    private static final int C_PRACTICE      = 0xFF88FF88;

    private static final String[][] KB_ROWS = {
        {"Q","W","E","R","T","Y","U","I","O","P"},
        {"A","S","D","F","G","H","J","K","L"},
        {"ENT","Z","X","C","V","B","N","M","DEL"}
    };

    private int TILE_SIZE;
    private int TILE_GAP;
    private int GRID_TOP;
    private int KEY_W;
    private int KEY_W_WIDE;
    private int KEY_H;
    private int KEY_GAP;

    private final WordleGame game         = new WordleGame();
    private final boolean    practiceMode;
    private String flashMsg   = "";
    private int    flashTimer = 0;

    // ボタンのヒットエリア（renderEndOverlay で毎フレーム更新）
    private int copyBtnX, copyBtnY, copyBtnW, copyBtnH;
    private int chatBtnX, chatBtnY, chatBtnW, chatBtnH;

    public WordleScreen(boolean practiceMode) {
        super(new StringTextComponent("MineWordle"));
        this.practiceMode = practiceMode;

        if (practiceMode) {
            game.startPractice();
        } else if (!WordleSaveManager.load(game)) {
            WordleFetcher.fetchTodaysSolution().thenAccept(word ->
                Minecraft.getInstance().execute(() -> {
                    if (word != null) game.setSolution(word);
                    else              game.setError();
                })
            );
        }
    }

    @Override
    public void removed() {
        if (!practiceMode) WordleSaveManager.save(game);
        super.removed();
    }

    @Override
    protected void init() {
        super.init();
        TILE_GAP = 4;
        KEY_GAP  = 4;
        GRID_TOP = 34;

        TILE_SIZE = 12;
        for (int s = 46; s >= 12; s--) {
            int kh    = s * 36 / 46;
            int kw    = s * 26 / 46;
            int totalH = GRID_TOP + 6 * (s + TILE_GAP) - TILE_GAP + 14
                       + 3 * (kh + KEY_GAP) - KEY_GAP + 10;
            int row1W  = 10 * kw + 9 * KEY_GAP;
            if (totalH <= this.height && row1W <= this.width - 20) {
                TILE_SIZE = s;
                break;
            }
        }

        KEY_H      = TILE_SIZE * 36 / 46;
        KEY_W      = TILE_SIZE * 26 / 46;
        KEY_W_WIDE = TILE_SIZE * 40 / 46;
    }

    // ── ヘルパー ──────────────────────────────────────────────────────────────

    private int gridLeft() {
        return (this.width - (5 * TILE_SIZE + 4 * TILE_GAP)) / 2;
    }

    private int keyboardTop() {
        return GRID_TOP + 6 * (TILE_SIZE + TILE_GAP) - TILE_GAP + 14;
    }

    private int panelWidth() {
        int kbRow1 = 10 * KEY_W + 9 * KEY_GAP + 16;
        int gridW  = 5 * TILE_SIZE + 4 * TILE_GAP + 20;
        return Math.max(kbRow1, gridW);
    }

    private int tileColor(TileState ts) {
        if (ts == null) return C_KEY_DEFAULT;
        switch (ts) {
            case CORRECT: return C_CORRECT;
            case PRESENT: return C_PRESENT;
            case ABSENT:  return C_ABSENT;
            default:      return C_TILE_EMPTY;
        }
    }

    private static int darken(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.max(0, ((argb >> 16) & 0xFF) - 28);
        int g = Math.max(0, ((argb >>  8) & 0xFF) - 28);
        int b = Math.max(0,  (argb        & 0xFF) - 28);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void playClick(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, pitch)
        );
    }

    private void playWinSound(int guesses) {
        net.minecraft.util.SoundEvent sound = guesses <= 2
            ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
            : guesses <= 4
                ? SoundEvents.PLAYER_LEVELUP
                : SoundEvents.EXPERIENCE_ORB_PICKUP;
        Minecraft.getInstance().getSoundManager().play(
            SimpleSound.forUI(sound, 1.0f)
        );
    }

    // ── シェア機能 ────────────────────────────────────────────────────────────

    private void copyShare() {
        Minecraft.getInstance().keyboardHandler.setClipboard(buildShareText());
        flash("Copied!");
    }

    private void sendToChat() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) { flash("Not in a world!"); return; }
        String header = buildShareText().split("\n")[0];
        client.player.chat(header);
        flash("Sent to chat!");
    }

    private String buildShareText() {
        int     guesses = game.getGuessIndex();
        boolean won     = game.getGameState() == GameState.WON;
        String  result  = won ? guesses + "/6" : "X/6";

        StringBuilder sb = new StringBuilder();
        if (practiceMode) {
            sb.append("MineWordle Practice ").append(result).append("\n\n");
        } else {
            long dayNum = ChronoUnit.DAYS.between(
                    LocalDate.of(2021, 6, 20),
                    LocalDate.now(ZoneId.systemDefault())) + 1;
            sb.append("Wordle ").append(dayNum).append(" ").append(result).append("\n\n");
        }

        TileState[][] eval = game.getEvaluated();
        for (int row = 0; row < guesses; row++) {
            for (int col = 0; col < 5; col++) {
                switch (eval[row][col]) {
                    case CORRECT: sb.append("🟩"); break;
                    case PRESENT: sb.append("🟨"); break;
                    default:      sb.append("⬜"); break;
                }
            }
            if (row < guesses - 1) sb.append("\n");
        }
        return sb.toString();
    }

    // ── 描画 ──────────────────────────────────────────────────────────────────

    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        this.renderBackground(ms);

        int pw        = panelWidth();
        int panelLeft = (this.width - pw) / 2;
        int panelTop  = GRID_TOP - 24;
        int panelBot  = keyboardTop() + 3 * (KEY_H + KEY_GAP) - KEY_GAP + 10;
        fill(ms, panelLeft, panelTop, panelLeft + pw, panelBot, C_PANEL_BG);

        drawCenteredString(ms, font, "MINEWORDLE", this.width / 2, panelTop + 4, C_WHITE);
        fill(ms, this.width / 2 - 80, panelTop + 14, this.width / 2 + 80, panelTop + 15, C_BORDER);

        if (practiceMode) {
            drawCenteredString(ms, font, "- PRACTICE -", this.width / 2, panelTop + 17, C_PRACTICE);
        }

        renderLoadingStatus(ms);
        renderGrid(ms);
        renderKeyboard(ms);
        renderEndOverlay(ms);
        renderFlash(ms);

        super.render(ms, mx, my, delta);
    }

    private void renderLoadingStatus(MatrixStack ms) {
        if (game.getGameState() == GameState.ERROR) {
            drawCenteredString(ms, font,
                "Failed to load — check your connection.",
                this.width / 2, GRID_TOP - 4, C_RED);
        }
    }

    private void renderEndOverlay(MatrixStack ms) {
        GameState state = game.getGameState();
        if (state != GameState.WON && state != GameState.LOST) return;

        boolean won     = (state == GameState.WON);
        int     guesses = game.getGuessIndex();
        String line1;
        if (won) {
            switch (guesses) {
                case 1:  line1 = "Genius!"; break;
                case 2:  line1 = "Magnificent!"; break;
                case 3:  line1 = "Impressive!"; break;
                case 4:  line1 = "Splendid!"; break;
                case 5:  line1 = "Great!"; break;
                default: line1 = "Phew!"; break;
            }
        } else {
            line1 = "Game over!";
        }
        String line2     = "The word was: " + game.getSolution();
        String copyLabel = "[ COPY ]";
        String chatLabel = "[ CHAT ]";
        int    color1    = won ? C_GREEN : C_RED;

        int w1   = font.width(line1);
        int w2   = font.width(line2);
        int wBtn = font.width(copyLabel) + 20;
        int boxW = Math.max(Math.max(w1, w2), wBtn) + 28;
        int boxH = 68;
        int boxX = this.width  / 2 - boxW / 2;
        int boxY = this.height / 2 - boxH / 2;

        fill(ms, boxX, boxY, boxX + boxW, boxY + boxH, 0xF0101010);
        drawRect(ms, boxX, boxY, boxW, boxH, 1, 0xFF888888);
        drawCenteredString(ms, font, line1, this.width / 2, boxY + 7,  color1);
        drawCenteredString(ms, font, line2, this.width / 2, boxY + 20, C_WHITE);

        int btnPad = 12;
        int btnW   = boxW - btnPad * 2;
        int btnH   = 13;

        copyBtnW = btnW; copyBtnH = btnH;
        copyBtnX = boxX + btnPad;
        copyBtnY = boxY + 33;
        fill(ms, copyBtnX, copyBtnY, copyBtnX + copyBtnW, copyBtnY + copyBtnH, 0xFF333333);
        drawRect(ms, copyBtnX, copyBtnY, copyBtnW, copyBtnH, 1, 0xFF666666);
        drawCenteredString(ms, font, copyLabel, this.width / 2, copyBtnY + 3, C_WHITE);

        chatBtnW = btnW; chatBtnH = btnH;
        chatBtnX = boxX + btnPad;
        chatBtnY = copyBtnY + btnH + 3;
        fill(ms, chatBtnX, chatBtnY, chatBtnX + chatBtnW, chatBtnY + chatBtnH, 0xFF333333);
        drawRect(ms, chatBtnX, chatBtnY, chatBtnW, chatBtnH, 1, 0xFF666666);
        drawCenteredString(ms, font, chatLabel, this.width / 2, chatBtnY + 3, C_WHITE);
    }

    private void renderGrid(MatrixStack ms) {
        int left       = gridLeft();
        String[] guesses  = game.getGuesses();
        TileState[][] eval = game.getEvaluated();
        String current = game.getCurrentInput();
        int guessIdx   = game.getGuessIndex();
        boolean playing = game.getGameState() == GameState.PLAYING;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                int x = left + col * (TILE_SIZE + TILE_GAP);
                int y = GRID_TOP + row * (TILE_SIZE + TILE_GAP);

                String letter = "";
                int bg     = C_TILE_EMPTY;
                int border = C_BORDER;

                if (row < guessIdx) {
                    TileState ts = eval[row][col];
                    bg     = tileColor(ts);
                    border = bg;
                    if (guesses[row] != null && col < guesses[row].length())
                        letter = String.valueOf(guesses[row].charAt(col));
                } else if (row == guessIdx && playing) {
                    if (col < current.length()) {
                        letter = String.valueOf(current.charAt(col));
                        border = C_BORDER_ACTIVE;
                    }
                }

                fill(ms, x, y, x + TILE_SIZE, y + TILE_SIZE, bg);
                drawRect(ms, x, y, TILE_SIZE, TILE_SIZE, 2, border);
                if (!letter.isEmpty())
                    drawCentered(ms, letter, x + TILE_SIZE / 2, y + TILE_SIZE / 2, C_WHITE);
            }
        }
    }

    private void renderKeyboard(MatrixStack ms) {
        TileState[] ks  = game.getKeyStates();
        int         top = keyboardTop();

        for (int row = 0; row < KB_ROWS.length; row++) {
            String[] keys = KB_ROWS[row];
            int rowW = 0;
            for (String k : keys) rowW += (isWide(k) ? KEY_W_WIDE : KEY_W) + KEY_GAP;
            rowW -= KEY_GAP;

            int x = (this.width - rowW) / 2;
            int y = top + row * (KEY_H + KEY_GAP);

            for (String key : keys) {
                int kw = isWide(key) ? KEY_W_WIDE : KEY_W;
                int bg = C_KEY_DEFAULT;
                if (key.length() == 1) {
                    TileState ts = ks[key.charAt(0) - 'A'];
                    if (ts != null) bg = tileColor(ts);
                }
                fill(ms, x, y, x + kw, y + KEY_H, bg);
                fill(ms, x, y + KEY_H - 3, x + kw, y + KEY_H, darken(bg));
                int lw = font.width(key);
                font.draw(ms, key, x + kw / 2 - lw / 2, y + KEY_H / 2 - 4, C_WHITE);
                x += kw + KEY_GAP;
            }
        }
    }

    private void renderFlash(MatrixStack ms) {
        if (flashTimer <= 0) return;
        flashTimer--;
        int fw = font.width(flashMsg);
        int fx = this.width / 2 - fw / 2;
        int fy = GRID_TOP - 14;
        fill(ms, fx - 6, fy - 2, fx + fw + 6, fy + 12, C_WHITE);
        font.draw(ms, flashMsg, fx, fy + 1, 0xFF000000);
    }

    private boolean isWide(String key) {
        return key.equals("ENT") || key.equals("DEL");
    }

    // ── 描画ユーティリティ ────────────────────────────────────────────────────

    private void drawRect(MatrixStack ms, int x, int y, int w, int h, int t, int color) {
        fill(ms, x,         y,         x + w,     y + t,     color);
        fill(ms, x,         y + h - t, x + w,     y + h,     color);
        fill(ms, x,         y,         x + t,     y + h,     color);
        fill(ms, x + w - t, y,         x + w,     y + h,     color);
    }

    private void drawCentered(MatrixStack ms, String text, int cx, int cy, int color) {
        int tw = font.width(text);
        font.draw(ms, text, cx - tw / 2, cy - 4, color);
    }

    // ── 入力 ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { this.onClose(); return true; }
        if (game.getGameState() != GameState.PLAYING) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { game.removeLetter(); playClick(0.9f); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER)     { handleSubmit(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (game.getGameState() != GameState.PLAYING) return false;
        if (Character.isLetter(chr)) {
            game.addLetter(chr);
            playClick(0.9f + (float) Math.random() * 0.2f);
            return true;
        }
        return false;
    }

    private void handleSubmit() {
        String result = game.submitGuess();
        switch (result) {
            case "NOT_ENOUGH_LETTERS": flash("Not enough letters!"); break;
            case "NOT_A_WORD":         flash("Not a word!"); break;
            case "WON":                playWinSound(game.getGuessIndex()); break;
            case "LOST":               playClick(0.6f); break;
            default:                   playClick(1.1f); break;
        }
    }

    private void flash(String msg) {
        this.flashMsg   = msg;
        this.flashTimer = 80;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        GameState state = game.getGameState();
        if ((state == GameState.WON || state == GameState.LOST) && button == 0) {
            if (mouseX >= copyBtnX && mouseX <= copyBtnX + copyBtnW
                    && mouseY >= copyBtnY && mouseY <= copyBtnY + copyBtnH) {
                copyShare();
                return true;
            }
            if (mouseX >= chatBtnX && mouseX <= chatBtnX + chatBtnW
                    && mouseY >= chatBtnY && mouseY <= chatBtnY + chatBtnH) {
                sendToChat();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
