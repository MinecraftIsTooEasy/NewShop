package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SSellRequestPacket implements Packet
{
    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "sell_request");

    private final int itemID;
    private final int count;
    private final int slotIndex;

    public C2SSellRequestPacket(PacketByteBuf buf)
    {
        this.itemID    = buf.readInt();
        this.count     = buf.readInt();
        this.slotIndex = buf.readInt();
    }

    public C2SSellRequestPacket(int itemID, int count, int slotIndex)
    {
        this.itemID    = itemID;
        this.count     = count;
        this.slotIndex = slotIndex;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(itemID);
        buf.writeInt(count);
        buf.writeInt(slotIndex);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            ShopS2C.ensureConfig(true);
            ShopS2C.sellSystem(serverPlayer, itemID, count, slotIndex);
        }
    }

    @Override
    public ResourceLocation getChannel() { return CHANNEL; }
}