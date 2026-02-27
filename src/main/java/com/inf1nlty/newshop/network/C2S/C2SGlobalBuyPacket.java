package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SGlobalBuyPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "global_buy");

    private final int listingId;
    private final int count;

    public C2SGlobalBuyPacket(PacketByteBuf buf)
    {
        this.listingId = buf.readInt();
        this.count     = buf.readInt();
    }

    public C2SGlobalBuyPacket(int listingId, int count)
    {
        this.listingId = listingId;
        this.count     = count;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(listingId);
        buf.writeInt(count);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
            ShopS2C.buyGlobal(serverPlayer, listingId, count);
    }

    @Override
    public ResourceLocation getChannel() { return CHANNEL;
    }
}