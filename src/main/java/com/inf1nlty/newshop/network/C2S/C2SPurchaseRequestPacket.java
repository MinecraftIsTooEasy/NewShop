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

    public C2SPurchaseRequestPacket(PacketByteBuf buf)
    {
        this.itemID = buf.readInt();
        this.meta   = buf.readInt();
        this.count  = buf.readInt();
    }

    public C2SPurchaseRequestPacket(int itemID, int meta, int count)
    {
        this.itemID = itemID;
        this.meta   = meta;
        this.count  = count;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(itemID);
        buf.writeInt(meta);
        buf.writeInt(count);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            ShopS2C.ensureConfig(true);
            ShopS2C.buySystem(serverPlayer, itemID, meta, count);
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}