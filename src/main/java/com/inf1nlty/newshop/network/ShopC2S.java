package com.inf1nlty.newshop.network;

import com.inf1nlty.newshop.network.C2S.*;
import moddedmite.rustedironcore.network.Network;

public final class ShopC2S {

    private ShopC2S() {}

    public static void sendPurchaseRequest(int itemID, int meta, int count, byte[] nbtData)
    {
        Network.sendToServer(new C2SPurchaseRequestPacket(itemID, meta, count, nbtData));
    }

    public static void sendSellRequest(int itemID, int count, int slotIndex)
    {
        Network.sendToServer(new C2SSellRequestPacket(itemID, count, slotIndex));
    }

    public static void sendOpenRequest()
    {
        Network.sendToServer(new C2SOpenShopPacket());
    }

    public static void sendGlobalOpenRequest()
    {
        Network.sendToServer(new C2SGlobalOpenPacket());
    }

    public static void sendGlobalBuy(int listingId, int count)
    {
        Network.sendToServer(new C2SGlobalBuyPacket(listingId, count));
    }

    public static void sendGlobalList(int itemId, int meta, int amount, int priceTenths)
    {
        Network.sendToServer(new C2SGlobalListPacket(itemId, meta, amount, priceTenths));
    }

    public static void sendGlobalListFromSlot(int itemId, int meta, int amount, int slotIndex, int priceTenths, boolean creative, byte[] nbtData)
    {
        Network.sendToServer(new C2SListFromSlotPacket(itemId, meta, amount, slotIndex, priceTenths, -1, false, creative, nbtData));
    }

    public static void sendGlobalListFromContainerSlot(int itemId, int meta, int amount, int containerSlotNumber, int priceTenths, int windowId, boolean creative)
    {
        Network.sendToServer(new C2SListFromSlotPacket(itemId, meta, amount, containerSlotNumber, priceTenths, windowId, true, creative));
    }

    public static void sendGlobalUnlist(int listingId)
    {
        Network.sendToServer(new C2SGlobalUnlistPacket(listingId));
    }

    public static void sendMailboxOpen()
    {
        Network.sendToServer(new C2SMailboxOpenPacket());
    }

    public static void sendSellToBuyOrder(int listingId, int count, int slotIndex)
    {
        Network.sendToServer(new C2SSellToBuyOrderPacket(listingId, count, slotIndex));
    }

    public static void sendSetPrice(int itemID, int meta, int buyTenths, int sellTenths, byte[] nbtData)
    {
        Network.sendToServer(new C2SSetPricePacket(itemID, meta, buyTenths, sellTenths, nbtData));
    }
}