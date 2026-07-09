package com.minewordle;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ClientRegistry;
import org.lwjgl.glfw.GLFW;

@Mod("minewordle")
public class MineWordleMod {

    private static final KeyMapping OPEN_KEY =
        new KeyMapping("key.minewordle.open", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");
    private static final KeyMapping OPEN_PRACTICE_KEY =
        new KeyMapping("key.minewordle.open_practice", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc");

    public MineWordleMod(IEventBus modBus) {
        modBus.addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(OPEN_KEY);
        ClientRegistry.registerKeyBinding(OPEN_PRACTICE_KEY);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        while (OPEN_KEY.consumeClick())          openScreen(false);
        while (OPEN_PRACTICE_KEY.consumeClick()) openScreen(true);
    }

    private static void openScreen(boolean practiceMode) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.screen == null) {
                client.setScreen(new WordleScreen(practiceMode));
            }
        });
    }
}
