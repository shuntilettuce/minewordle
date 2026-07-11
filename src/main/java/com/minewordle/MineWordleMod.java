package com.minewordle;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;

@Mod("minewordle")
public class MineWordleMod {

    // /wordle runs synchronously inside ChatScreen's key handler, before the
    // chat screen closes itself — opening WordleScreen immediately would race
    // with (and lose to) that close. Defer to the next client tick instead.
    private static volatile Boolean pendingOpen = null;

    public MineWordleMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("wordle")
                .executes(ctx -> requestOpen(false))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("practice")
                    .executes(ctx -> requestOpen(true)))
        );
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pendingOpen == null) return;
        boolean practiceMode = pendingOpen;
        pendingOpen = null;

        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof WordleScreen)) {
            client.setScreen(new WordleScreen(practiceMode));
        }
    }

    private static int requestOpen(boolean practiceMode) {
        pendingOpen = practiceMode;
        return 1;
    }
}
