package com.minewordle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public class MineWordleMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("wordle")
                .executes(ctx -> openScreen(ctx.getSource().getClient(), false))
                .then(ClientCommandManager.literal("practice")
                    .executes(ctx -> openScreen(ctx.getSource().getClient(), true)))
            )
        );
    }

    private static int openScreen(MinecraftClient client, boolean practiceMode) {
        client.execute(() -> {
            if (client.currentScreen == null) {
                client.setScreen(new WordleScreen(practiceMode));
            }
        });
        return 1;
    }
}
