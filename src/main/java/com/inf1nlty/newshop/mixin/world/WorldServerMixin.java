package com.inf1nlty.newshop.mixin.world;

import com.inf1nlty.newshop.global.GlobalShopData;
import com.inf1nlty.newshop.registry.ShopPropertyRegistry;
import com.inf1nlty.newshop.util.MailboxManager;
import com.inf1nlty.newshop.util.MoneyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.ISaveHandler;
import net.minecraft.SaveHandler;
import net.minecraft.WorldSettings;
import net.minecraft.Profiler;
import net.minecraft.ILogAgent;
import net.minecraft.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(WorldServer.class)
public class WorldServerMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onWorldServerConstructed(MinecraftServer server, ISaveHandler saveHandler, String worldName, int dimension, WorldSettings settings, Profiler profiler, ILogAgent logAgent, CallbackInfo ci) {

        GlobalShopData.clearStatic();
        MoneyManager.clearStatic();
        MailboxManager.clearStatic();

        File worldDir = ((SaveHandler)saveHandler).getWorldDirectory();
        File shopDir = new File(worldDir, "shop");

        MoneyManager.init(shopDir);
        MoneyManager.loadBalancesFromFile();

        GlobalShopData.init(shopDir);
        GlobalShopData.load();

        MailboxManager.init(shopDir);

        if (dimension == 0) {
            ShopPropertyRegistry.run();
        }
    }
}