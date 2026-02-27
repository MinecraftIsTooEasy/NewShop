package com.inf1nlty.newshop;

import com.inf1nlty.newshop.network.ShopPacketHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.ModResourceManager;

public class ShopMod implements ModInitializer {

    public void onInitialize() {

        ModResourceManager.addResourcePackDomain("newshop");

        ShopConfig.getInstance().load();
        ConfigManager.getInstance().registerConfig(ShopConfig.getInstance());

        ShopEvents.register();

        ShopPacketHandler.init();
    }
}