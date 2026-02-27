package com.inf1nlty.newshop.mixin.client;

import com.inf1nlty.newshop.client.input.ShopKeyHandler;
import net.minecraft.EntityClientPlayerMP;
import net.minecraft.GuiScreen;
import net.minecraft.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Polls custom shop hotkeys each client tick. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow public EntityClientPlayerMP thePlayer;
    @Shadow public GuiScreen currentScreen;

    @Inject(method = "runTick", at = @At("TAIL"))
    private void shop$handleHotkeys(CallbackInfo ci)
    {
        ShopKeyHandler.onClientTick();
    }
}