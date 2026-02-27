package com.inf1nlty.newshop.mixin.client;

import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guards against a null space crashing isVisible on EMI sidebar panels. */
@Mixin(value = EmiScreenManager.SidebarPanel.class, remap = false)
public class SidebarPanelMixin
{
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void safeIsVisible(CallbackInfoReturnable<Boolean> cir)
    {
        EmiScreenManager.SidebarPanel self = (EmiScreenManager.SidebarPanel) (Object) this;
        if (self.space == null) cir.setReturnValue(false);
    }
}