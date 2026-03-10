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

/**
 * Shortcut keys for listing items in the global marketplace from any inventory GUI:
 *   Left-ALT  + Left-Click  → post a sell order  (items deducted; creative/OP: no deduction)
 *   Left-ALT  + Right-Click → post a buy order   (no items deducted; price paid from balance)
 *   Right-ALT + Left-Click  → creative/OP only: open the system shop price editor
 */
@Mixin(GuiContainer.class)
public abstract class InventoryListingMixin
{
    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci)
    {
        boolean lAlt = Keyboard.isKeyDown(Keyboard.KEY_LMENU);
        boolean rAlt = Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (!lAlt && !rAlt) return;

        // button 0 = left-click, button 1 = right-click
        boolean isLeftClick  = (button == 0);
        boolean isRightClick = (button == 1);

        // Right-ALT + Left-Click  → system shop price editor (creative only)
        // Left-ALT  + Left-Click  → sell order
        // Left-ALT  + Right-Click → buy order
        boolean wantSystemShop = rAlt && isLeftClick;
        boolean wantSellOrder  = lAlt && isLeftClick;
        boolean wantBuyOrder   = lAlt && isRightClick;

        if (!wantSystemShop && !wantSellOrder && !wantBuyOrder) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        GuiContainer self = (GuiContainer) (Object) this;

        // Find the slot under the cursor
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

        if (wantSystemShop)
        {
            // Right-ALT + Left-Click: open system shop price editor (creative/OP only)
            if (!creative) return;
            stack.stackSize = 1;
            mc.displayGuiScreen(new com.inf1nlty.newshop.client.gui.GuiEditPrice(self, stack));
            ci.cancel();
            return;
        }

        if (wantBuyOrder)
        {
            // Left-ALT + Right-Click: post a buy order — no item deduction required
            stack.stackSize = 1;
            mc.displayGuiScreen(new GuiCreateListing(self, stack, -1, -1, false, false, true));
            ci.cancel();
            return;
        }

        // Left-ALT + Left-Click: post a sell order
        if (creative)
        {
            // Creative mode: use hovered item as a template; server skips inventory deduction
            stack.stackSize = 1;
            mc.displayGuiScreen(new GuiCreateListing(self, stack, -1, -1, false, true, false));
        }
        else if (found.inventory == mc.thePlayer.inventory)
        {
            int slotIdx = found.slotIndex;
            ItemStack[] mainInv = mc.thePlayer.inventory.mainInventory;
            if (slotIdx < 0 || slotIdx >= mainInv.length || mainInv[slotIdx] == null) return;
            mc.displayGuiScreen(new GuiCreateListing(self, stack, slotIdx, -1, false, false, false));
        }
        else
        {
            mc.displayGuiScreen(new GuiCreateListing(self, stack, found.slotNumber, self.inventorySlots.windowId, true, false, false));
        }
        ci.cancel();
    }
}