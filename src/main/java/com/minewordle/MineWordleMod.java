package com.minewordle;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("minewordle")
public class MineWordleMod {

    public MineWordleMod(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.register(this);
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
