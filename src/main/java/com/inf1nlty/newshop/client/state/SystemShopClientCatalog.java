package com.inf1nlty.newshop.client.state;

import net.minecraft.CompressedStreamTools;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/** Holds the server-provided system shop entries on the client. */
public final class SystemShopClientCatalog {

    public static class Entry
    {
        public int itemID;
        public int meta;
        public int buyTenths;
        public int sellTenths;
        /** Compressed NBT of the item stack (may be null for items without subtype NBT). */
        public byte[] nbtCompressed;

        public ItemStack toStack()
        {
            Item item = (itemID >= 0 && itemID < Item.itemsList.length) ? Item.itemsList[itemID] : null;
            if (item == null) return null;
            ItemStack stack = new ItemStack(item, 1, meta);
            // Restore variant NBT (needed for MITE NBT-subtype items to display correctly)
            if (nbtCompressed != null && nbtCompressed.length > 0) {
                try {
                    stack.stackTagCompound = CompressedStreamTools.readCompressed(
                            new ByteArrayInputStream(nbtCompressed));
                } catch (Exception ignored) {}
            }
            if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
            stack.stackTagCompound.setInteger("ShopBuyPrice", buyTenths);
            stack.stackTagCompound.setInteger("ShopSellPrice", sellTenths);
            return stack;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private SystemShopClientCatalog() {}

    public static void set(List<Entry> fresh)
    {
        ENTRIES.clear();
        ENTRIES.addAll(fresh);
    }

    public static List<Entry> get() {
        return ENTRIES;
    }
}