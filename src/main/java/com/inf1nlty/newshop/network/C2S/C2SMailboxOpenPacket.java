package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SMailboxOpenPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "mailbox_open");

    public C2SMailboxOpenPacket(PacketByteBuf buf) {}
    public C2SMailboxOpenPacket() {}

    @Override public void write(PacketByteBuf buf) {}

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
            ShopS2C.openMailbox(serverPlayer);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}