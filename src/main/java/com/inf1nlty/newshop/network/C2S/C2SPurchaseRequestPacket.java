package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SPurchaseRequestPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "purchase_request");

    private final int itemID;
    private final int meta;
    private final int count;
    private final byte[] nbtData;

    public C2SPurchaseRequestPacket(PacketByteBuf buf)
    {
        this.itemID = buf.readInt();
        this.meta   = buf.readInt();
        this.count  = buf.readInt();
        boolean hasNbt = buf.readBoolean();
        if (hasNbt) {
            int len = buf.readUnsignedShort();
            this.nbtData = new byte[len];
            buf.readFully(this.nbtData);
        } else {
            this.nbtData = null;
        }
    }

    public C2SPurchaseRequestPacket(int itemID, int meta, int count, byte[] nbtData)
    {
        this.itemID  = itemID;
        this.meta    = meta;
        this.count   = count;
        this.nbtData = nbtData;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(itemID);
        buf.writeInt(meta);
        buf.writeInt(count);
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
        if (player instanceof ServerPlayer serverPlayer)
        {
            ShopS2C.ensureConfig(true);
            ShopS2C.buySystem(serverPlayer, itemID, meta, count, nbtData);
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}