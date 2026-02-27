package com.inf1nlty.newshop.mixin.client;

import com.inf1nlty.newshop.client.gui.GuiEditPrice;
import net.minecraft.GuiContainerCreative;
import net.minecraft.ItemStack;
import net.minecraft.Minecraft;
import net.minecraft.Slot;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Alt + Left-Click on any item in the creative inventory opens {@link GuiEditPrice} for OPs to set system shop prices. */
@Mixin(GuiContainerCreative.class)
public abstract class CreativePriceMixin {

    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void shop$onMouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci)
    {
        if (button != 0) return;

        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (!altDown) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !mc.thePlayer.capabilities.isCreativeMode) return;

        GuiContainerCreative self = (GuiContainerCreative) (Object) this;

        Slot found = null;
        for (Object obj : self.inventorySlots.inventorySlots)
        {
            Slot slot = (Slot) obj;
            int sx = self.guiLeft + slot.xDisplayPosition;
            int sy = self.guiTop  + slot.yDisplayPosition;
            if (mouseX >= sx - 1 && mouseX < sx + 17 && mouseY >= sy - 1 && mouseY < sy + 17)
            {
                found = slot;
                break;
            }
        }

        if (found == null || !found.getHasStack()) return;

        ItemStack stack = found.getStack().copy();
        stack.stackSize = 1;

        mc.displayGuiScreen(new GuiEditPrice(self, stack));
        ci.cancel();
    }

}