package com.sysshop.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SysShopMod.MOD_ID)
public final class SysShopMod {
    public static final String MOD_ID = "sysshop";
    public static final Logger LOGGER = LogManager.getLogger();

    public SysShopMod() {
        ShopNetwork.init();
        MinecraftForge.EVENT_BUS.register(this);
    }
}
