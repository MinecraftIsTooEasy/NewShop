package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.*;

import java.io.ByteArrayInputStream;

/**
 * Lists a global sell order from a specific inventory or container slot.
 * fromContainer=false uses player.inventory.mainInventory[slotIndex];
 * fromContainer=true uses player.openContainer.getSlot(slotIndex), validated by windowId.
 * When creative=true and slotIndex=-1, nbtData carries the full item NBT template.
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
    private final boolean creative;
    /** Compressed item NBT for creative template listings (slotIndex == -1). May be null. */
    private final byte[]  nbtData;

    public C2SListFromSlotPacket(PacketByteBuf buf)
    {
        this.itemId        = buf.readInt();
        this.meta          = buf.readInt();
        this.amount        = buf.readInt();
        this.slotIndex     = buf.readInt();
        this.priceTenths   = buf.readInt();
        this.windowId      = buf.readInt();
        this.fromContainer = buf.readBoolean();
        this.creative      = buf.readBoolean();
        boolean hasNbt     = buf.readBoolean();
        if (hasNbt) {
            int len = buf.readUnsignedShort();
            this.nbtData = new byte[len];
            buf.readFully(this.nbtData);
        } else {
            this.nbtData = null;
        }
    }

    public C2SListFromSlotPacket(int itemId, int meta, int amount, int slotIndex, int priceTenths)
    {
        this(itemId, meta, amount, slotIndex, priceTenths, -1, false, false, null);
    }

    public C2SListFromSlotPacket(int itemId, int meta, int amount, int slotIndex, int priceTenths, int windowId, boolean fromContainer, boolean creative)
    {
        this(itemId, meta, amount, slotIndex, priceTenths, windowId, fromContainer, creative, null);
    }

    public C2SListFromSlotPacket(int itemId, int meta, int amount, int slotIndex, int priceTenths, int windowId, boolean fromContainer, boolean creative, byte[] nbtData)
    {
        this.itemId        = itemId;
        this.meta          = meta;
        this.amount        = amount;
        this.slotIndex     = slotIndex;
        this.priceTenths   = priceTenths;
        this.windowId      = windowId;
        this.fromContainer = fromContainer;
        this.creative      = creative;
        this.nbtData       = nbtData;
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
        buf.writeBoolean(creative);
        if (nbtData != null && nbtData.length > 0) {
            buf.writeBoolean(true);
            buf.writeShort(nbtData.length);
            buf.write(nbtData, 0, nbtData.length);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Decompress template NBT if present
        NBTTagCompound templateNbt = null;
        if (nbtData != null && nbtData.length > 0) {
            try { templateNbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(nbtData)); }
            catch (Exception ignored) {}
        }

        if (fromContainer)
            ShopS2C.listGlobalFromContainerSlot(serverPlayer, itemId, meta, amount, priceTenths, windowId, slotIndex, creative);
        else
            ShopS2C.listGlobalFromSlot(serverPlayer, itemId, meta, amount, priceTenths, slotIndex, creative, templateNbt);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}