package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SGlobalUnlistPacket implements Packet
{
    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "global_unlist");

    private final int listingId;

    public C2SGlobalUnlistPacket(PacketByteBuf buf)   { this.listingId = buf.readInt(); }
    public C2SGlobalUnlistPacket(int listingId)        { this.listingId = listingId; }

    @Override
    public void write(PacketByteBuf buf) { buf.writeInt(listingId); }

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
            ShopS2C.unlistGlobal(serverPlayer, listingId);
    }

    @Override
    public ResourceLocation getChannel() { return CHANNEL; }
}