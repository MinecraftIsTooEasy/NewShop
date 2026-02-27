package com.inf1nlty.newshop.registry;

import com.inf1nlty.newshop.ShopConfig;
import com.inf1nlty.newshop.GoodsConfig;
import com.inf1nlty.newshop.api.ShopPlugin;
import com.inf1nlty.newshop.api.ShopPluginLoader;
import net.xiaoyu233.fml.FishModLoader;

/** Triggers plugin registration and config reload on each world load. */
public class ShopPropertyRegistry {

    /** Called on every Overworld load. */
    public static void run()
    {
        ShopPluginLoader.clearAll();
        ShopPluginLoader.register(new InternalPlugin());

        if (ShopConfig.SKYBLOCK_MODE.getBooleanValue())
            ShopPluginLoader.register(new SkyblockPlugin());

        FishModLoader.getEntrypointContainers("newshop", ShopPlugin.class)
                .forEach(container -> ShopPluginLoader.register(container.getEntrypoint()));

        GoodsConfig.reload();
    }

    public static void reset()
    {
        ShopPluginLoader.clearAll();
    }
}