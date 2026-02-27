package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;

public class C2SGlobalListPacket implements Packet
{
    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "global_list");

    private final int itemId;
    private final int meta;
    private final int amount;
    private final int priceTenths;

    public C2SGlobalListPacket(PacketByteBuf buf)
    {
        this.itemId      = buf.readInt();
        this.meta        = buf.readInt();
        this.amount      = buf.readInt();
        this.priceTenths = buf.readInt();
    }

    public C2SGlobalListPacket(int itemId, int meta, int amount, int priceTenths)
    {
        this.itemId      = itemId;
        this.meta        = meta;
        this.amount      = amount;
        this.priceTenths = priceTenths;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(itemId);
        buf.writeInt(meta);
        buf.writeInt(amount);
        buf.writeInt(priceTenths);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (player instanceof ServerPlayer serverPlayer)
            ShopS2C.listGlobal(serverPlayer, itemId, meta, amount, priceTenths);
    }

    @Override
    public ResourceLocation getChannel()
    {
        return CHANNEL;
    }
}