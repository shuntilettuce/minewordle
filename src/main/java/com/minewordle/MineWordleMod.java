package com.minewordle;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod("minewordle")
public class MineWordleMod {

    public MineWordleMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("wordle")
                .executes(ctx -> openScreen(false))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("practice")
                    .executes(ctx -> openScreen(true)))
        );
    }

    private static int openScreen(boolean practiceMode) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.screen == null) {
                client.setScreen(new WordleScreen(practiceMode));
            }
        });
        return 1;
    }
}
