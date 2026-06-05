package com.minewordle;

import com.minewordle.WordleGame.GameState;
import com.minewordle.WordleGame.TileState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.PositionedSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
        super(Component.literal("MineWordle"));
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
        return switch (ts) {
            case CORRECT -> C_CORRECT;
            case PRESENT -> C_PRESENT;
            case ABSENT  -> C_ABSENT;
            default      -> C_TILE_EMPTY;
        };
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
            PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, pitch)
        );
    }

    private void playWinSound(int guesses) {
        if (guesses <= 2) {
            Minecraft.getInstance().getSoundManager().play(
                PositionedSoundInstance.ui(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
        } else if (guesses <= 4) {
            Minecraft.getInstance().getSoundManager().play(
                PositionedSoundInstance.ui(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f));
        } else {
            Minecraft.getInstance().getSoundManager().play(
                PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        }
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
        client.player.connection.sendChat(header);
        flash("Sent to chat!");
    }

    private String buildShareText() {
        int     guesses = game.getGuessIndex();
        boolean won     = game.getGameState() == GameState.WON;
        String  result  = won ? guesses + "/6" : "X/6";

        StringBuilder sb = new StringBuilder();
        sb.append("MineWordle ").append(dayNumber()).append(" ").append(result).append("\n");

        TileState[][] eval = game.getEvaluated();
        for (int row = 0; row < guesses; row++) {
            for (int col = 0; col < 5; col++) {
                TileState ts = (eval[row] != null) ? eval[row][col] : null;
                sb.append(ts == TileState.CORRECT ? "🟩"
                         : ts == TileState.PRESENT ? "🟨" : "⬛");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private int dayNumber() {
        LocalDate epoch = LocalDate.of(2025, 1, 1);
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        return (int) ChronoUnit.DAYS.between(epoch, today) + 1;
    }

    // ── 描画 ──────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        super.extractRenderState(graphics, mx, my, delta);
        graphics.fill(0, 0, this.width, this.height, 0xC0000000);

        int pw        = panelWidth();
        int panelLeft = (this.width - pw) / 2;
        int panelTop  = GRID_TOP - 24;
        int panelBot  = keyboardTop() + 3 * (KEY_H + KEY_GAP) - KEY_GAP + 10;
        graphics.fill(panelLeft, panelTop, panelLeft + pw, panelBot, C_PANEL_BG);

        drawCenteredText(graphics, "MINEWORDLE", this.width / 2, panelTop + 4, C_WHITE, true);
        graphics.fill(this.width / 2 - 80, panelTop + 14, this.width / 2 + 80, panelTop + 15, C_BORDER);

        if (practiceMode) {
            drawCenteredText(graphics, "- PRACTICE -", this.width / 2, panelTop + 17, C_PRACTICE, true);
        }

        renderLoadingStatus(graphics);
        renderGrid(graphics);
        renderKeyboard(graphics);
        renderEndOverlay(graphics);
        renderFlash(graphics);
    }

    private void renderLoadingStatus(GuiGraphicsExtractor graphics) {
        if (game.getGameState() == GameState.ERROR) {
            drawCenteredText(graphics, "Failed to load - check your connection.",
                this.width / 2, GRID_TOP - 4, C_RED, true);
        }
    }

    private void renderEndOverlay(GuiGraphicsExtractor graphics) {
        GameState state = game.getGameState();
        if (state != GameState.WON && state != GameState.LOST) return;

        boolean won     = (state == GameState.WON);
        int     guesses = game.getGuessIndex();
        String line1 = won ? switch (guesses) {
            case 1  -> "Genius!";
            case 2  -> "Magnificent!";
            case 3  -> "Impressive!";
            case 4  -> "Splendid!";
            case 5  -> "Great!";
            default -> "Phew!";
        } : "Game over!";
        String line2     = "The word was: " + game.getSolution();
        String copyLabel = "[ COPY ]";
        String chatLabel = "[ CHAT ]";
        int    color1    = won ? C_GREEN : C_RED;

        int w1   = this.font.width(line1);
        int w2   = this.font.width(line2);
        int wBtn = this.font.width(copyLabel) + 20;
        int boxW = Math.max(Math.max(w1, w2), wBtn) + 28;
        int boxH = 68;
        int boxX = this.width  / 2 - boxW / 2;
        int boxY = this.height / 2 - boxH / 2;

        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF0101010);
        drawRect(graphics, boxX, boxY, boxW, boxH, 1, 0xFF888888);
        drawCenteredText(graphics, line1, this.width / 2, boxY + 7,  color1, true);
        drawCenteredText(graphics, line2, this.width / 2, boxY + 20, C_WHITE, true);

        int btnPad = 12;
        int btnW   = boxW - btnPad * 2;
        int btnH   = 13;

        copyBtnW = btnW; copyBtnH = btnH;
        copyBtnX = boxX + btnPad;
        copyBtnY = boxY + 33;
        graphics.fill(copyBtnX, copyBtnY, copyBtnX + copyBtnW, copyBtnY + copyBtnH, 0xFF333333);
        drawRect(graphics, copyBtnX, copyBtnY, copyBtnW, copyBtnH, 1, 0xFF666666);
        drawCenteredText(graphics, copyLabel, this.width / 2, copyBtnY + 3, C_WHITE, true);

        chatBtnW = btnW; chatBtnH = btnH;
        chatBtnX = boxX + btnPad;
        chatBtnY = copyBtnY + btnH + 3;
        graphics.fill(chatBtnX, chatBtnY, chatBtnX + chatBtnW, chatBtnY + chatBtnH, 0xFF333333);
        drawRect(graphics, chatBtnX, chatBtnY, chatBtnW, chatBtnH, 1, 0xFF666666);
        drawCenteredText(graphics, chatLabel, this.width / 2, chatBtnY + 3, C_WHITE, true);
    }

    private void renderGrid(GuiGraphicsExtractor graphics) {
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

                graphics.fill(x, y, x + TILE_SIZE, y + TILE_SIZE, bg);
                drawRect(graphics, x, y, TILE_SIZE, TILE_SIZE, 2, border);
                if (!letter.isEmpty())
                    drawCentered(graphics, letter, x + TILE_SIZE / 2, y + TILE_SIZE / 2, C_WHITE);
            }
        }
    }

    private void renderKeyboard(GuiGraphicsExtractor graphics) {
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
                graphics.fill(x, y, x + kw, y + KEY_H, bg);
                graphics.fill(x, y + KEY_H - 3, x + kw, y + KEY_H, darken(bg));
                int lw = this.font.width(key);
                graphics.text(this.font, key, x + kw / 2 - lw / 2, y + KEY_H / 2 - 4, C_WHITE, false);
                x += kw + KEY_GAP;
            }
        }
    }

    private void renderFlash(GuiGraphicsExtractor graphics) {
        if (flashTimer <= 0) return;
        flashTimer--;
        int fw = this.font.width(flashMsg);
        int fx = this.width / 2 - fw / 2;
        int fy = GRID_TOP - 14;
        graphics.fill(fx - 6, fy - 2, fx + fw + 6, fy + 12, C_WHITE);
        graphics.text(this.font, flashMsg, fx, fy + 1, 0xFF000000, false);
    }

    private boolean isWide(String key) {
        return key.equals("ENT") || key.equals("DEL");
    }

    // ── 描画ユーティリティ ────────────────────────────────────────────────────

    private void drawRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int t, int color) {
        g.fill(x,         y,         x + w,     y + t,     color);
        g.fill(x,         y + h - t, x + w,     y + h,     color);
        g.fill(x,         y,         x + t,     y + h,     color);
        g.fill(x + w - t, y,         x + w,     y + h,     color);
    }

    private void drawCentered(GuiGraphicsExtractor g, String text, int cx, int cy, int color) {
        int tw = this.font.width(text);
        g.text(this.font, text, cx - tw / 2, cy - 4, color, false);
    }

    private void drawCenteredText(GuiGraphicsExtractor g, String text, int cx, int y, int color, boolean shadow) {
        int tw = this.font.width(text);
        g.text(this.font, text, cx - tw / 2, y, color, shadow);
    }

    // ── 入力 ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { this.onClose(); return true; }
        if (game.getGameState() != GameState.PLAYING) return super.keyPressed(event);
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) { game.removeLetter(); playClick(0.9f); return true; }
        if (event.key() == GLFW.GLFW_KEY_ENTER)     { handleSubmit(); return true; }
        return super.keyPressed(event);
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
            case "NOT_ENOUGH_LETTERS" -> flash("Not enough letters!");
            case "NOT_A_WORD"         -> flash("Not a word!");
            case "WON"                -> playWinSound(game.getGuessIndex());
            case "LOST"               -> playClick(0.6f);
            default                   -> playClick(1.1f);
        }
    }

    private void flash(String msg) {
        this.flashMsg   = msg;
        this.flashTimer = 80;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        GameState state = game.getGameState();
        if ((state == GameState.WON || state == GameState.LOST) && event.button() == 0) {
            if (event.x() >= copyBtnX && event.x() <= copyBtnX + copyBtnW
                    && event.y() >= copyBtnY && event.y() <= copyBtnY + copyBtnH) {
                copyShare();
                return true;
            }
            if (event.x() >= chatBtnX && event.x() <= chatBtnX + chatBtnW
                    && event.y() >= chatBtnY && event.y() <= chatBtnY + chatBtnH) {
                sendToChat();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
