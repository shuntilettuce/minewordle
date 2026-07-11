package com.minewordle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MineWordleMod implements ClientModInitializer {

    // /wordle runs synchronously inside the chat screen's key handler, before
    // the chat screen closes itself — opening WordleScreen immediately would
    // race with (and lose to) that close. Defer to the next client tick.
    private static volatile Boolean pendingOpen = null;

    @Override
    public void onInitializeClient() {
        ClientCommandManager.DISPATCHER.register(
            ClientCommandManager.literal("wordle")
                .executes(ctx -> requestOpen(false))
                .then(ClientCommandManager.literal("practice")
                    .executes(ctx -> requestOpen(true)))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingOpen == null) return;
            boolean practiceMode = pendingOpen;
            pendingOpen = null;

            if (!(client.currentScreen instanceof WordleScreen)) {
                client.openScreen(new WordleScreen(practiceMode));
            }
        });
    }

    private static int requestOpen(boolean practiceMode) {
        pendingOpen = practiceMode;
        return 1;
    }
}
