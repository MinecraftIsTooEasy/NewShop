package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SSellToBuyOrderPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "sell_to_buy_order");

    private final int listingId;
    private final int count;
    private final int slotIndex;

    public C2SSellToBuyOrderPacket(PacketByteBuf buf)
    {
        this.listingId = buf.readInt();
        this.count     = buf.readInt();
        this.slotIndex = buf.readInt();
    }

    public C2SSellToBuyOrderPacket(int listingId, int count, int slotIndex)
    {
        this.listingId = listingId;
        this.count     = count;
        this.slotIndex = slotIndex;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(listingId);
        buf.writeInt(count);
        buf.writeInt(slotIndex);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
            ShopS2C.sellToBuyOrder(serverPlayer, listingId, count, slotIndex);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}