package com.minewordle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class MineWordleMod implements ClientModInitializer {

    // /wordle runs synchronously inside the chat screen's key handler, before
    // the chat screen closes itself — opening WordleScreen immediately would
    // race with (and lose to) that close. Defer to the next client tick.
    private static volatile Boolean pendingOpen = null;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("wordle")
                .executes(ctx -> requestOpen(false))
                .then(ClientCommands.literal("practice")
                    .executes(ctx -> requestOpen(true)))
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingOpen == null) return;
            boolean practiceMode = pendingOpen;
            pendingOpen = null;

            if (!(client.screen instanceof WordleScreen)) {
                client.setScreen(new WordleScreen(practiceMode));
            }
        });
    }

    private static int requestOpen(boolean practiceMode) {
        pendingOpen = practiceMode;
        return 1;
    }
}
