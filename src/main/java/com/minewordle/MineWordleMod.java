package com.minewordle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.client.MinecraftClient;

public class MineWordleMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandManager.DISPATCHER.register(
            ClientCommandManager.literal("wordle")
                .executes(ctx -> openScreen(ctx.getSource().getClient(), false))
                .then(ClientCommandManager.literal("practice")
                    .executes(ctx -> openScreen(ctx.getSource().getClient(), true)))
        );
    }

    private static int openScreen(MinecraftClient client, boolean practiceMode) {
        client.execute(() -> {
            if (client.currentScreen == null) {
                client.openScreen(new WordleScreen(practiceMode));
            }
        });
        return 1;
    }
}
