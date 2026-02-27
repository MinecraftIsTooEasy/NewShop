package com.inf1nlty.newshop;

import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;

public class ShopListing {

    public int itemID;
    public int damage;
    public String displayName;
    public int buyPriceTenths;
    public int sellPriceTenths;
    public ItemStack itemStack;

    public ItemStack getShopStack() {
        ItemStack stack = new ItemStack(itemStack.getItem(), 1, damage);
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger("ShopBuyPrice", buyPriceTenths);
        stack.stackTagCompound.setInteger("ShopSellPrice", sellPriceTenths);
        stack.stackTagCompound.setString("ShopDisplay", displayName);
        return stack;
    }

    public static String formatTenths(int t) {
        int w = t / 10;
        int f = Math.abs(t % 10);
        return w + "." + f;
    }
}