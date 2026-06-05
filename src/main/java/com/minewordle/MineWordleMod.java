package com.minewordle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

public class MineWordleMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("wordle")
                .executes(ctx -> openScreen(ctx.getSource().getClient(), false))
                .then(ClientCommands.literal("practice")
                    .executes(ctx -> openScreen(ctx.getSource().getClient(), true)))
            )
        );
    }

    private static int openScreen(Minecraft client, boolean practiceMode) {
        client.execute(() -> {
            if (client.screen == null) {
                client.setScreen(new WordleScreen(practiceMode));
            }
        });
        return 1;
    }
}
