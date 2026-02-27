package com.inf1nlty.newshop.mixin.client;

import com.inf1nlty.newshop.client.gui.GuiCreateListing;
import net.minecraft.GuiContainer;
import net.minecraft.ItemStack;
import net.minecraft.Minecraft;
import net.minecraft.Slot;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Alt + Right-Click on any inventory slot opens the global listing creation GUI. */
@Mixin(GuiContainer.class)
public abstract class InventoryListingMixin
{
    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci)
    {
        if (button != 1) return;

        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (!altDown) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        GuiContainer self = (GuiContainer) (Object) this;

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

        if (found.inventory == mc.thePlayer.inventory)
        {
            ItemStack[] mainInv = mc.thePlayer.inventory.mainInventory;
            ItemStack   actual  = found.getStack();
            int slotIdx = -1;
            for (int i = 0; i < mainInv.length; i++)
            {
                if (mainInv[i] == actual) { slotIdx = i; break; }
            }
            if (slotIdx < 0) return;
            mc.displayGuiScreen(new GuiCreateListing(self, stack, slotIdx));
        }
        else
        {
            mc.displayGuiScreen(new GuiCreateListing(self, stack, found.slotNumber, self.inventorySlots.windowId, true));
        }

        ci.cancel();
    }
}