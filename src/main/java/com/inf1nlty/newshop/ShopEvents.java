package com.inf1nlty.newshop;

import com.inf1nlty.newshop.event.ShopCommandEvents;
import com.inf1nlty.newshop.registry.ShopPropertyRegistry;
import moddedmite.rustedironcore.api.event.Handlers;
import moddedmite.rustedironcore.api.event.listener.IWorldLoadListener;
import net.minecraft.WorldClient;

public class ShopEvents {

    public static void register() {

        Handlers.Command.register(new ShopCommandEvents());

        Handlers.WorldLoad.register(new IWorldLoadListener() {
            @Override
            public void onWorldLoad(WorldClient world) {
                ShopPropertyRegistry.run();
            }
        });

    }
}