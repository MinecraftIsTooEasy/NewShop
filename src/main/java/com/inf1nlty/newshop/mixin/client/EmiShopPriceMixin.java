package com.inf1nlty.newshop.mixin.client;

import com.inf1nlty.newshop.client.gui.GuiEditPrice;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.ItemStack;
import net.minecraft.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Alt + Left-Click on an EMI sidebar item opens {@link GuiEditPrice} for OPs to set system shop prices. */
@Pseudo
@Mixin(EmiScreenManager.class)
public class EmiShopPriceMixin
{
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void shop$onMouseClicked(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir)
    {
        if (button != 0) return;

        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (!altDown) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !mc.thePlayer.capabilities.isCreativeMode) return;

        EmiStackInteraction hovered = EmiScreenManager.getHoveredStack((int) mx, (int) my, false);
        if (hovered == null || hovered.getStack() == null || hovered.getStack().isEmpty()) return;

        List<EmiStack> stacks = hovered.getStack().getEmiStacks();
        if (stacks == null || stacks.isEmpty()) return;

        EmiStack emiStack = stacks.get(0);
        if (emiStack.isEmpty()) return;

        ItemStack itemStack = emiStack.getItemStack();
        if (itemStack == null) return;

        ItemStack copy = itemStack.copy();
        copy.stackSize = 1;
        mc.displayGuiScreen(new GuiEditPrice(mc.currentScreen, copy));
        cir.setReturnValue(true);
    }
}