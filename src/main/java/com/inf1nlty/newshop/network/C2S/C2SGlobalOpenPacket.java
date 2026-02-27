package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SGlobalOpenPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "global_open");

    public C2SGlobalOpenPacket(PacketByteBuf buf) {}
    public C2SGlobalOpenPacket() {}

    @Override public void write(PacketByteBuf buf) {}

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
            ShopS2C.openGlobalShop(serverPlayer);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}