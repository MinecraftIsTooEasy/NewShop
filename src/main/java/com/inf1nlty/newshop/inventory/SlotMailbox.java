package com.inf1nlty.newshop.inventory;

import net.minecraft.*;

public class SlotMailbox extends Slot {
    public SlotMailbox(IInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }
}