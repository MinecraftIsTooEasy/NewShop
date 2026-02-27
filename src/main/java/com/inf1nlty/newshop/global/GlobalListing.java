package com.inf1nlty.newshop.global;

import net.minecraft.NBTTagCompound;

import java.util.UUID;

/** A single global shop listing (sell or buy order). */
public class GlobalListing {

    public int           listingId;
    public UUID          ownerUUID;
    public String        ownerName;
    public int           itemId;
    public int           meta;
    public int           amount;
    public int           priceTenths;
    public NBTTagCompound nbt;
    public boolean       isBuyOrder;

    public GlobalListing copyShallow() {
        GlobalListing copy   = new GlobalListing();
        copy.listingId   = listingId;
        copy.ownerUUID   = ownerUUID;
        copy.ownerName   = ownerName;
        copy.itemId      = itemId;
        copy.meta        = meta;
        copy.amount      = amount;
        copy.priceTenths = priceTenths;
        copy.nbt         = nbt == null ? null : (NBTTagCompound) nbt.copy();
        copy.isBuyOrder  = isBuyOrder;
        return copy;
    }
}