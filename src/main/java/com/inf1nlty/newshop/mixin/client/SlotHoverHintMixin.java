package com.inf1nlty.newshop.mixin.client;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.*;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Appends Alt-action hint lines to tooltips when Alt is held.
 * Covers both container slots and EMI sidebar items.
 */
@Mixin(GuiContainer.class)
public abstract class SlotHoverHintMixin extends GuiScreen
{
    @Shadow public Slot theSlot;

    @Shadow protected abstract void func_102021_a(List<String> lines, int x, int y);

    @Inject(method = "drawScreen(IIF)V", at = @At("TAIL"))
    private void shop$drawHints(int mouseX, int mouseY, float partial, CallbackInfo ci)
    {
        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (!altDown) return;

        boolean isOp = mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode;

        if (isOp)
        {
            EmiStackInteraction emiHovered = EmiScreenManager.getHoveredStack(mouseX, mouseY, false);
            if (emiHovered != null && emiHovered.getStack() != null && !emiHovered.getStack().isEmpty())
            {
                List<EmiStack> stacks = emiHovered.getStack().getEmiStacks();
                if (stacks != null && !stacks.isEmpty() && !stacks.get(0).isEmpty()
                        && stacks.get(0).getItemStack() != null)
                {
                    List<String> hints = new ArrayList<>();

                    hints.add("§7" + I18n.getString("shop.hint.altleft.editprice"));
                    hints.add("§7" + I18n.getString("shop.hint.altright.list"));
                    func_102021_a(hints, mouseX, mouseY);
                    return;
                }
            }
        }

        // Container slot hover
        if (theSlot == null || !theSlot.getHasStack()) return;

        List<String> hints = new ArrayList<>();

        if (isOp)
            hints.add("§7" + I18n.getString("shop.hint.altleft.editprice"));

        hints.add("§7" + I18n.getString("shop.hint.altright.list"));

        func_102021_a(hints, mouseX, mouseY);
    }
}