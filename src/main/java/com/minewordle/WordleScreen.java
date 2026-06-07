package com.minewordle;

import com.minewordle.WordleGame.GameState;
import com.minewordle.WordleGame.TileState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class WordleScreen extends Screen {

    private static final int C_CORRECT=0xFF538D4E,C_PRESENT=0xFFB59F3B,C_ABSENT=0xFF3A3A3C;
    private static final int C_TILE_EMPTY=0xFF121213,C_BORDER=0xFF3A3A3C,C_BORDER_ACTIVE=0xFF878A8C;
    private static final int C_KEY_DEFAULT=0xFF818384,C_WHITE=0xFFFFFFFF,C_GREEN=0xFF538D4E;
    private static final int C_RED=0xFFFF5555,C_PANEL_BG=0xD0121213,C_PRACTICE=0xFF88FF88;

    private static final String[][] KB_ROWS = {
        {"Q","W","E","R","T","Y","U","I","O","P"},
        {"A","S","D","F","G","H","J","K","L"},
        {"ENT","Z","X","C","V","B","N","M","DEL"}
    };

    private int TILE_SIZE,TILE_GAP,GRID_TOP,KEY_W,KEY_W_WIDE,KEY_H,KEY_GAP;
    private final WordleGame game=new WordleGame();
    private final boolean practiceMode;
    private String flashMsg=""; private int flashTimer=0;
    private int copyBtnX,copyBtnY,copyBtnW,copyBtnH,chatBtnX,chatBtnY,chatBtnW,chatBtnH;

    public WordleScreen(boolean practiceMode) {
        super(new LiteralText("MineWordle"));
        this.practiceMode=practiceMode;
        if (practiceMode) { game.startPractice(); }
        else if (!WordleSaveManager.load(game)) {
            WordleFetcher.fetchTodaysSolution().thenAccept(word ->
                MinecraftClient.getInstance().execute(() -> {
                    if (word!=null) game.setSolution(word); else game.setError();
                }));
        }
    }

    @Override public void removed() { if (!practiceMode) WordleSaveManager.save(game); super.removed(); }

    @Override
    protected void init() {
        super.init(); TILE_GAP=4; KEY_GAP=4; GRID_TOP=34; TILE_SIZE=12;
        for (int s=46;s>=12;s--) {
            int kh=s*36/46,kw=s*26/46;
            int totalH=GRID_TOP+6*(s+TILE_GAP)-TILE_GAP+14+3*(kh+KEY_GAP)-KEY_GAP+10;
            if (totalH<=this.height && 10*kw+9*KEY_GAP<=this.width-20) { TILE_SIZE=s; break; }
        }
        KEY_H=TILE_SIZE*36/46; KEY_W=TILE_SIZE*26/46; KEY_W_WIDE=TILE_SIZE*40/46;
    }

    private int gridLeft() { return (this.width-(5*TILE_SIZE+4*TILE_GAP))/2; }
    private int keyboardTop() { return GRID_TOP+6*(TILE_SIZE+TILE_GAP)-TILE_GAP+14; }
    private int panelWidth() { return Math.max(10*KEY_W+9*KEY_GAP+16,5*TILE_SIZE+4*TILE_GAP+20); }

    private int tileColor(TileState ts) {
        if (ts==null) return C_KEY_DEFAULT;
        return switch(ts){case CORRECT->C_CORRECT;case PRESENT->C_PRESENT;case ABSENT->C_ABSENT;default->C_TILE_EMPTY;};
    }
    private static int darken(int a) {
        int al=(a>>24)&0xFF,r=Math.max(0,((a>>16)&0xFF)-28),g=Math.max(0,((a>>8)&0xFF)-28),b=Math.max(0,(a&0xFF)-28);
        return (al<<24)|(r<<16)|(g<<8)|b;
    }
    private void playClick(float p){MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK,p));}
    private void playWinSound(int g){
        var s=g<=2?SoundEvents.UI_TOAST_CHALLENGE_COMPLETE:g<=4?SoundEvents.ENTITY_PLAYER_LEVELUP:SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(s,1.0f));
    }
    private void copyShare(){MinecraftClient.getInstance().keyboard.setClipboard(buildShareText());flash("Copied!");}
    private void sendToChat(){
        MinecraftClient c=MinecraftClient.getInstance();
        if(c.player==null){flash("Not in a world!");return;}
        c.player.sendChatMessage(buildShareText().split("\n")[0]); flash("Sent to chat!");
    }
    private String buildShareText(){
        int g=game.getGuessIndex(); boolean won=game.getGameState()==GameState.WON;
        String res=won?g+"/6":"X/6"; StringBuilder sb=new StringBuilder();
        if(practiceMode){sb.append("MineWordle Practice ").append(res).append("\n\n");}
        else{long d=ChronoUnit.DAYS.between(LocalDate.of(2021,6,20),LocalDate.now(ZoneId.systemDefault()))+1;sb.append("Wordle ").append(d).append(" ").append(res).append("\n\n");}
        TileState[][] eval=game.getEvaluated();
        for(int row=0;row<g;row++){for(int col=0;col<5;col++){sb.append(switch(eval[row][col]){case CORRECT->"🟩";case PRESENT->"🟨";default->"⬜";});}if(row<g-1)sb.append("\n");}
        return sb.toString();
    }

    @Override
    public void render(MatrixStack matrices,int mx,int my,float delta){
        fill(matrices,0,0,this.width,this.height,0xC0000000);
        int pw=panelWidth(),pl=(this.width-pw)/2,pt=GRID_TOP-24,pb=keyboardTop()+3*(KEY_H+KEY_GAP)-KEY_GAP+10;
        fill(matrices,pl,pt,pl+pw,pb,C_PANEL_BG);
        drawCenteredText(matrices,textRenderer,new LiteralText("MINEWORDLE"),this.width/2,pt+4,C_WHITE);
        fill(matrices,this.width/2-80,pt+14,this.width/2+80,pt+15,C_BORDER);
        if(practiceMode) drawCenteredText(matrices,textRenderer,new LiteralText("- PRACTICE -"),this.width/2,pt+17,C_PRACTICE);
        renderLoadingStatus(matrices);renderGrid(matrices);renderKeyboard(matrices);renderEndOverlay(matrices);renderFlash(matrices);
        super.render(matrices,mx,my,delta);
    }
    private void renderLoadingStatus(MatrixStack matrices){
        if(game.getGameState()==GameState.ERROR)
            drawCenteredText(matrices,textRenderer,new LiteralText("Failed to load — check your connection."),this.width/2,GRID_TOP-4,C_RED);
    }
    private void renderEndOverlay(MatrixStack matrices){
        GameState state=game.getGameState();
        if(state!=GameState.WON&&state!=GameState.LOST)return;
        boolean won=(state==GameState.WON); int g=game.getGuessIndex();
        String line1=won?switch(g){case 1->"Genius!";case 2->"Magnificent!";case 3->"Impressive!";case 4->"Splendid!";case 5->"Great!";default->"Phew!";}:"Game over!";
        String line2="The word was: "+game.getSolution(),copyLabel="[ COPY ]",chatLabel="[ CHAT ]";
        int color1=won?C_GREEN:C_RED,w1=textRenderer.getWidth(line1),w2=textRenderer.getWidth(line2);
        int boxW=Math.max(Math.max(w1,w2),textRenderer.getWidth(copyLabel)+20)+28,boxH=68;
        int boxX=this.width/2-boxW/2,boxY=this.height/2-boxH/2;
        fill(matrices,boxX,boxY,boxX+boxW,boxY+boxH,0xF0101010);
        drawRect(matrices,boxX,boxY,boxW,boxH,1,0xFF888888);
        drawCenteredText(matrices,textRenderer,new LiteralText(line1),this.width/2,boxY+7,color1);
        drawCenteredText(matrices,textRenderer,new LiteralText(line2),this.width/2,boxY+20,C_WHITE);
        int bp=12,bw=boxW-bp*2,bh=13;
        copyBtnW=bw;copyBtnH=bh;copyBtnX=boxX+bp;copyBtnY=boxY+33;
        fill(matrices,copyBtnX,copyBtnY,copyBtnX+bw,copyBtnY+bh,0xFF333333);
        drawRect(matrices,copyBtnX,copyBtnY,bw,bh,1,0xFF666666);
        drawCenteredText(matrices,textRenderer,new LiteralText(copyLabel),this.width/2,copyBtnY+3,C_WHITE);
        chatBtnW=bw;chatBtnH=bh;chatBtnX=boxX+bp;chatBtnY=copyBtnY+bh+3;
        fill(matrices,chatBtnX,chatBtnY,chatBtnX+bw,chatBtnY+bh,0xFF333333);
        drawRect(matrices,chatBtnX,chatBtnY,bw,bh,1,0xFF666666);
        drawCenteredText(matrices,textRenderer,new LiteralText(chatLabel),this.width/2,chatBtnY+3,C_WHITE);
    }
    private void renderGrid(MatrixStack matrices){
        int left=gridLeft(); String[] gs=game.getGuesses(); TileState[][] eval=game.getEvaluated();
        String cur=game.getCurrentInput(); int gi=game.getGuessIndex(); boolean playing=game.getGameState()==GameState.PLAYING;
        for(int row=0;row<6;row++){for(int col=0;col<5;col++){
            int x=left+col*(TILE_SIZE+TILE_GAP),y=GRID_TOP+row*(TILE_SIZE+TILE_GAP);
            String letter=""; int bg=C_TILE_EMPTY,border=C_BORDER;
            if(row<gi){TileState ts=eval[row][col];bg=tileColor(ts);border=bg;if(gs[row]!=null&&col<gs[row].length())letter=String.valueOf(gs[row].charAt(col));}
            else if(row==gi&&playing&&col<cur.length()){letter=String.valueOf(cur.charAt(col));border=C_BORDER_ACTIVE;}
            fill(matrices,x,y,x+TILE_SIZE,y+TILE_SIZE,bg);
            drawRect(matrices,x,y,TILE_SIZE,TILE_SIZE,2,border);
            if(!letter.isEmpty())drawCentered(matrices,letter,x+TILE_SIZE/2,y+TILE_SIZE/2,C_WHITE);
        }}
    }
    private void renderKeyboard(MatrixStack matrices){
        TileState[] ks=game.getKeyStates(); int top=keyboardTop();
        for(int row=0;row<KB_ROWS.length;row++){
            String[] keys=KB_ROWS[row]; int rowW=0;
            for(String k:keys)rowW+=(isWide(k)?KEY_W_WIDE:KEY_W)+KEY_GAP; rowW-=KEY_GAP;
            int x=(this.width-rowW)/2,y=top+row*(KEY_H+KEY_GAP);
            for(String key:keys){
                int kw=isWide(key)?KEY_W_WIDE:KEY_W,bg=C_KEY_DEFAULT;
                if(key.length()==1){TileState ts=ks[key.charAt(0)-'A'];if(ts!=null)bg=tileColor(ts);}
                fill(matrices,x,y,x+kw,y+KEY_H,bg); fill(matrices,x,y+KEY_H-3,x+kw,y+KEY_H,darken(bg));
                int lw=textRenderer.getWidth(key);
                textRenderer.draw(matrices,key,x+kw/2-lw/2,y+KEY_H/2-4,C_WHITE);
                x+=kw+KEY_GAP;
            }
        }
    }
    private void renderFlash(MatrixStack matrices){
        if(flashTimer<=0)return; flashTimer--;
        int fw=textRenderer.getWidth(flashMsg),fx=this.width/2-fw/2,fy=GRID_TOP-14;
        fill(matrices,fx-6,fy-2,fx+fw+6,fy+12,C_WHITE);
        textRenderer.draw(matrices,flashMsg,fx,fy+1,0xFF000000);
    }
    private boolean isWide(String k){return k.equals("ENT")||k.equals("DEL");}
    private void drawRect(MatrixStack matrices,int x,int y,int w,int h,int t,int c){
        fill(matrices,x,y,x+w,y+t,c);fill(matrices,x,y+h-t,x+w,y+h,c);
        fill(matrices,x,y,x+t,y+h,c);fill(matrices,x+w-t,y,x+w,y+h,c);
    }
    private void drawCentered(MatrixStack matrices,String text,int cx,int cy,int color){
        textRenderer.draw(matrices,text,cx-textRenderer.getWidth(text)/2,cy-4,color);
    }
    @Override public boolean keyPressed(int kc,int sc,int mod){
        if(kc==GLFW.GLFW_KEY_ESCAPE){this.onClose();return true;}
        if(game.getGameState()!=GameState.PLAYING)return super.keyPressed(kc,sc,mod);
        if(kc==GLFW.GLFW_KEY_BACKSPACE){game.removeLetter();playClick(0.9f);return true;}
        if(kc==GLFW.GLFW_KEY_ENTER){handleSubmit();return true;}
        return super.keyPressed(kc,sc,mod);
    }
    @Override public boolean charTyped(char chr,int mod){
        if(game.getGameState()!=GameState.PLAYING)return false;
        if(Character.isLetter(chr)){game.addLetter(chr);playClick(0.9f+(float)Math.random()*0.2f);return true;}
        return false;
    }
    private void handleSubmit(){
        String r=game.submitGuess();
        switch(r){
            case "NOT_ENOUGH_LETTERS"->flash("Not enough letters!");
            case "NOT_A_WORD"->flash("Not a word!");
            case "WON"->playWinSound(game.getGuessIndex());
            case "LOST"->playClick(0.6f);
            default->playClick(1.1f);
        }
    }
    private void flash(String msg){this.flashMsg=msg;this.flashTimer=80;}
    @Override public boolean mouseClicked(double mx,double my,int btn){
        GameState state=game.getGameState();
        if((state==GameState.WON||state==GameState.LOST)&&btn==0){
            if(mx>=copyBtnX&&mx<=copyBtnX+copyBtnW&&my>=copyBtnY&&my<=copyBtnY+copyBtnH){copyShare();return true;}
            if(mx>=chatBtnX&&mx<=chatBtnX+chatBtnW&&my>=chatBtnY&&my<=chatBtnY+chatBtnH){sendToChat();return true;}
        }
        return super.mouseClicked(mx,my,btn);
    }
    public boolean shouldPause(){return false;}
}
