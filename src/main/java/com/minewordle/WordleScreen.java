package com.minewordle;

import com.minewordle.WordleGame.GameState;
import com.minewordle.WordleGame.TileState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class WordleScreen extends Screen {

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

    private int copyBtnX, copyBtnY, copyBtnW, copyBtnH;
    private int chatBtnX, chatBtnY, chatBtnW, chatBtnH;

    public WordleScreen(boolean practiceMode) {
        super(new LiteralText("MineWordle"));
        this.practiceMode = practiceMode;

        if (practiceMode) {
            game.startPractice();
        } else if (!WordleSaveManager.load(game)) {
            WordleFetcher.fetchTodaysSolution().thenAccept(word ->
                MinecraftClient.getInstance().execute(() -> {
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
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, pitch)
        );
    }

    private void playWinSound(int guesses) {
        var sound = guesses <= 2
            ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
            : guesses <= 4
                ? SoundEvents.ENTITY_PLAYER_LEVELUP
                : SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(sound, 1.0f)
        );
    }

    private void copyShare() {
        MinecraftClient.getInstance().keyboard.setClipboard(buildShareText());
        flash("Copied!");
    }

    private void sendToChat() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) { flash("Not in a world!"); return; }
        String header = buildShareText().split("\n")[0];
        client.player.sendChatMessage(header);
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
                sb.append(switch (eval[row][col]) {
                    case CORRECT -> "🟩";
                    case PRESENT -> "🟨";
                    default      -> "⬜";
                });
            }
            if (row < guesses - 1) sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public void render(MatrixStack matrices, int mx, int my, float delta) {
        fill(matrices, 0, 0, this.width, this.height, 0xC0000000);

        int pw        = panelWidth();
        int panelLeft = (this.width - pw) / 2;
        int panelTop  = GRID_TOP - 24;
        int panelBot  = keyboardTop() + 3 * (KEY_H + KEY_GAP) - KEY_GAP + 10;
        fill(matrices, panelLeft, panelTop, panelLeft + pw, panelBot, C_PANEL_BG);

        drawCenteredTextWithShadow(matrices, textRenderer, new LiteralText("MINEWORDLE"), this.width / 2, panelTop + 4, C_WHITE);
        fill(matrices, this.width / 2 - 80, panelTop + 14, this.width / 2 + 80, panelTop + 15, C_BORDER);

        if (practiceMode) {
            drawCenteredTextWithShadow(matrices, textRenderer,
                new LiteralText("- PRACTICE -"), this.width / 2, panelTop + 17, C_PRACTICE);
        }

        renderLoadingStatus(matrices);
        renderGrid(matrices);
        renderKeyboard(matrices);
        renderEndOverlay(matrices);
        renderFlash(matrices);

        super.render(matrices, mx, my, delta);
    }

    private void renderLoadingStatus(MatrixStack matrices) {
        if (game.getGameState() == GameState.ERROR) {
            drawCenteredTextWithShadow(matrices, textRenderer,
                new LiteralText("Failed to load — check your connection."),
                this.width / 2, GRID_TOP - 4, C_RED);
        }
    }

    private void renderEndOverlay(MatrixStack matrices) {
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

        int w1   = textRenderer.getWidth(line1);
        int w2   = textRenderer.getWidth(line2);
        int wBtn = textRenderer.getWidth(copyLabel) + 20;
        int boxW = Math.max(Math.max(w1, w2), wBtn) + 28;
        int boxH = 68;
        int boxX = this.width  / 2 - boxW / 2;
        int boxY = this.height / 2 - boxH / 2;

        fill(matrices, boxX, boxY, boxX + boxW, boxY + boxH, 0xF0101010);
        drawRect(matrices, boxX, boxY, boxW, boxH, 1, 0xFF888888);
        drawCenteredTextWithShadow(matrices, textRenderer, new LiteralText(line1), this.width / 2, boxY + 7,  color1);
        drawCenteredTextWithShadow(matrices, textRenderer, new LiteralText(line2), this.width / 2, boxY + 20, C_WHITE);

        int btnPad = 12;
        int btnW   = boxW - btnPad * 2;
        int btnH   = 13;

        copyBtnW = btnW; copyBtnH = btnH;
        copyBtnX = boxX + btnPad;
        copyBtnY = boxY + 33;
        fill(matrices, copyBtnX, copyBtnY, copyBtnX + copyBtnW, copyBtnY + copyBtnH, 0xFF333333);
        drawRect(matrices, copyBtnX, copyBtnY, copyBtnW, copyBtnH, 1, 0xFF666666);
        drawCenteredTextWithShadow(matrices, textRenderer, new LiteralText(copyLabel),
                this.width / 2, copyBtnY + 3, C_WHITE);

        chatBtnW = btnW; chatBtnH = btnH;
        chatBtnX = boxX + btnPad;
        chatBtnY = copyBtnY + btnH + 3;
        fill(matrices, chatBtnX, chatBtnY, chatBtnX + chatBtnW, chatBtnY + chatBtnH, 0xFF333333);
        drawRect(matrices, chatBtnX, chatBtnY, chatBtnW, chatBtnH, 1, 0xFF666666);
        drawCenteredTextWithShadow(matrices, textRenderer, new LiteralText(chatLabel),
                this.width / 2, chatBtnY + 3, C_WHITE);
    }

    private void renderGrid(MatrixStack matrices) {
        int left          = gridLeft();
        String[] guesses  = game.getGuesses();
        TileState[][] eval = game.getEvaluated();
        String current    = game.getCurrentInput();
        int guessIdx      = game.getGuessIndex();
        boolean playing   = game.getGameState() == GameState.PLAYING;

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

                fill(matrices, x, y, x + TILE_SIZE, y + TILE_SIZE, bg);
                drawRect(matrices, x, y, TILE_SIZE, TILE_SIZE, 2, border);
                if (!letter.isEmpty())
                    drawCentered(matrices, letter, x + TILE_SIZE / 2, y + TILE_SIZE / 2, C_WHITE);
            }
        }
    }

    private void renderKeyboard(MatrixStack matrices) {
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
                fill(matrices, x, y, x + kw, y + KEY_H, bg);
                fill(matrices, x, y + KEY_H - 3, x + kw, y + KEY_H, darken(bg));
                int lw = textRenderer.getWidth(key);
                textRenderer.draw(matrices, key, x + kw / 2 - lw / 2, y + KEY_H / 2 - 4, C_WHITE);
                x += kw + KEY_GAP;
            }
        }
    }

    private void renderFlash(MatrixStack matrices) {
        if (flashTimer <= 0) return;
        flashTimer--;
        int fw = textRenderer.getWidth(flashMsg);
        int fx = this.width / 2 - fw / 2;
        int fy = GRID_TOP - 14;
        fill(matrices, fx - 6, fy - 2, fx + fw + 6, fy + 12, C_WHITE);
        textRenderer.draw(matrices, flashMsg, fx, fy + 1, 0xFF000000);
    }

    private boolean isWide(String key) {
        return key.equals("ENT") || key.equals("DEL");
    }

    private void drawRect(MatrixStack matrices, int x, int y, int w, int h, int t, int color) {
        fill(matrices, x,         y,         x + w,     y + t,     color);
        fill(matrices, x,         y + h - t, x + w,     y + h,     color);
        fill(matrices, x,         y,         x + t,     y + h,     color);
        fill(matrices, x + w - t, y,         x + w,     y + h,     color);
    }

    private void drawCentered(MatrixStack matrices, String text, int cx, int cy, int color) {
        int tw = textRenderer.getWidth(text);
        textRenderer.draw(matrices, text, cx - tw / 2, cy - 4, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { this.close(); return true; }
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
    public boolean shouldPause() { return false; }
}
