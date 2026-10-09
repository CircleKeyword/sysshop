package com.sysshop.core;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SysShopMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() { }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sysshop")
                .then(Commands.literal("open").executes(context -> { ShopClientState.openBrowse(); return 1; }))
                .then(Commands.literal("create").executes(context -> { ShopClientState.openCreate(); return 1; }))
                .then(Commands.literal("manager").executes(context -> { ShopClientState.openManager(); return 1; }))
                .then(Commands.literal("shop").executes(context -> { ShopClientState.openBrowse(); return 1; })));
    }
}
