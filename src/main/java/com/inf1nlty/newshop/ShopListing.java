package com.inf1nlty.newshop;

import net.minecraft.CompressedStreamTools;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ShopListing {

    public int itemID;
    public int damage;
    public String displayName;
    public int buyPriceTenths;
    public int sellPriceTenths;
    public ItemStack itemStack;
    public NBTTagCompound nbt;

    public ItemStack getShopStack() {
        ItemStack stack = new ItemStack(itemStack.getItem(), 1, damage);
        if (nbt != null) stack.stackTagCompound = (NBTTagCompound) nbt.copy();
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger("ShopBuyPrice", buyPriceTenths);
        stack.stackTagCompound.setInteger("ShopSellPrice", sellPriceTenths);
        stack.stackTagCompound.setString("ShopDisplay", displayName);
        return stack;
    }

    /** Returns a stable string key that uniquely identifies this listing (itemID:meta[:base64nbt]). */
    public String nbtKey() {
        if (nbt == null) return null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(nbt, bos);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
        } catch (Exception e) { return null; }
    }

    public static String formatTenths(int t) {
        int w = t / 10;
        int f = Math.abs(t % 10);
        return w + "." + f;
    }

    /** Strips shop-internal price tags from an NBT compound, returning null if nothing remains. */
    public static NBTTagCompound stripShopTags(NBTTagCompound tag) {
        if (tag == null) return null;
        NBTTagCompound copy = (NBTTagCompound) tag.copy();
        copy.removeTag("ShopBuyPrice");
        copy.removeTag("ShopSellPrice");
        copy.removeTag("ShopDisplay");
        copy.removeTag("ShopBaby");
        return copy.hasNoTags() ? null : copy;
    }
}