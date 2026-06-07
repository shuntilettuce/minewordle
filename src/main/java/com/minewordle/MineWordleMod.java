package com.minewordle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class MineWordleMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBinding key = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.minewordle.open", GLFW.GLFW_KEY_UNKNOWN, "MineWordle"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.wasPressed()) {
                if (client.currentScreen == null) client.openScreen(new WordleScreen(false));
            }
        });
    }
}
