package com.inf1nlty.newshop.mixin.world;

import com.inf1nlty.newshop.util.MailboxManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(method = "stopServer", at = @At("RETURN"))
    private void shop$onServerShutdown(CallbackInfo ci)
    {
        List<ServerPlayer> players = new ArrayList<>();

        for (Object obj : ((MinecraftServer) (Object) this).getConfigurationManager().playerEntityList)

            if (obj instanceof ServerPlayer serverPlayer) players.add(serverPlayer);
        MailboxManager.flushAllMailboxesToNBT(players);
    }
}