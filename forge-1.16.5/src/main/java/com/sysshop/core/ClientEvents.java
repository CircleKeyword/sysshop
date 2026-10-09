package com.sysshop.core;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SysShopMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() { }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String message = event.getMessage().trim();
        if (message.equals("/sysshop open")) {
            event.setCanceled(true);
            ShopClientState.openBrowse();
        } else if (message.equals("/sysshop create")) {
            event.setCanceled(true);
            ShopClientState.openCreate();
        } else if (message.equals("/sysshop manager")) {
            event.setCanceled(true);
            ShopClientState.openManager();
        } else if (message.equals("/sysshop shop")) {
            event.setCanceled(true);
            ShopClientState.openBrowse();
        }
    }
}
