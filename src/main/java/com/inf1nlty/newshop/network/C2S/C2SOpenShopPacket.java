package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SOpenShopPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "open_shop");

    public C2SOpenShopPacket(PacketByteBuf buf) {}
    public C2SOpenShopPacket() {}

    @Override public void write(PacketByteBuf buf) {}

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            ShopS2C.ensureConfig(false);
            ShopS2C.openSystemShop(serverPlayer);
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}