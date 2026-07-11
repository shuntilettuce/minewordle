package com.minewordle;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("minewordle")
public class MineWordleMod {

    // /wordle runs synchronously inside ChatScreen's key handler, before the
    // chat screen closes itself — opening WordleScreen immediately would race
    // with (and lose to) that close. Defer to the next client tick instead.
    private static volatile Boolean pendingOpen = null;

    public MineWordleMod(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.register(this);
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
    public void onClientTick(TickEvent.ClientTickEvent.Post event) {
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
