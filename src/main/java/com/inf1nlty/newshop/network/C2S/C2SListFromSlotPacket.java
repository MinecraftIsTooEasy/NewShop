package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

/**
 * Lists a global sell order from a specific inventory or container slot.
 * fromContainer=false uses player.inventory.mainInventory[slotIndex];
 * fromContainer=true uses player.openContainer.getSlot(slotIndex), validated by windowId.
 */
public class C2SListFromSlotPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "global_list_from_slot");

    private final int     itemId;
    private final int     meta;
    private final int     amount;
    private final int     slotIndex;
    private final int     priceTenths;
    private final int     windowId;
    private final boolean fromContainer;

    public C2SListFromSlotPacket(PacketByteBuf buf)
    {
        this.itemId        = buf.readInt();
        this.meta          = buf.readInt();
        this.amount        = buf.readInt();
        this.slotIndex     = buf.readInt();
        this.priceTenths   = buf.readInt();
        this.windowId      = buf.readInt();
        this.fromContainer = buf.readBoolean();
    }

    public C2SListFromSlotPacket(int itemId, int meta, int amount, int slotIndex, int priceTenths)
    {
        this(itemId, meta, amount, slotIndex, priceTenths, -1, false);
    }

    public C2SListFromSlotPacket(int itemId, int meta, int amount, int slotIndex, int priceTenths, int windowId, boolean fromContainer)
    {
        this.itemId        = itemId;
        this.meta          = meta;
        this.amount        = amount;
        this.slotIndex     = slotIndex;
        this.priceTenths   = priceTenths;
        this.windowId      = windowId;
        this.fromContainer = fromContainer;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(itemId);
        buf.writeInt(meta);
        buf.writeInt(amount);
        buf.writeInt(slotIndex);
        buf.writeInt(priceTenths);
        buf.writeInt(windowId);
        buf.writeBoolean(fromContainer);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (fromContainer)
            ShopS2C.listGlobalFromContainerSlot(serverPlayer, itemId, meta, amount, priceTenths, windowId, slotIndex);
        else
            ShopS2C.listGlobalFromSlot(serverPlayer, itemId, meta, amount, priceTenths, slotIndex);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}