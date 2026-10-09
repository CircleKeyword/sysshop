package com.sysshop.core;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SysShopMod.MOD_ID)
public final class SysShopMod {
    public static final String MOD_ID = "sysshop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SysShopMod() {
        ShopNetwork.init();
        MinecraftForge.EVENT_BUS.register(this);
    }
}
