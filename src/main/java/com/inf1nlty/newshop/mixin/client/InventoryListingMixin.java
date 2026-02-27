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
        boolean creative = mc.thePlayer.capabilities.isCreativeMode;

        if (creative) {
            // In creative mode: use the hovered item as a template regardless of which
            // inventory it came from (creative tabs, hotbar, etc.).
            // slotIndex=-1 tells the server to skip inventory deduction entirely.
            stack.stackSize = 1;
            mc.displayGuiScreen(new GuiCreateListing(self, stack, -1, -1, false, true));
        } else if (found.inventory == mc.thePlayer.inventory) {
            int slotIdx = found.slotIndex;
            ItemStack[] mainInv = mc.thePlayer.inventory.mainInventory;
            if (slotIdx < 0 || slotIdx >= mainInv.length || mainInv[slotIdx] == null) return;
            mc.displayGuiScreen(new GuiCreateListing(self, stack, slotIdx, -1, false, false));
        } else {
            mc.displayGuiScreen(new GuiCreateListing(self, stack, found.slotNumber, self.inventorySlots.windowId, true, false));
        }

        ci.cancel();
    }
}