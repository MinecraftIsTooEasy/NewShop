package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.GoodsConfig;
import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;
import net.minecraft.ServerPlayer;
import net.minecraft.server.MinecraftServer;

/** Admin sets buy/sell price for a system shop item. OP-only. */
public class C2SSetPricePacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "set_price");

    private final int itemID;
    private final int meta;
    private final int buyTenths;
    private final int sellTenths;

    public C2SSetPricePacket(PacketByteBuf buf)
    {
        this.itemID     = buf.readInt();
        this.meta       = buf.readInt();
        this.buyTenths  = buf.readInt();
        this.sellTenths = buf.readInt();
    }

    public C2SSetPricePacket(int itemID, int meta, int buyTenths, int sellTenths)
    {
        this.itemID     = itemID;
        this.meta       = meta;
        this.buyTenths  = buyTenths;
        this.sellTenths = sellTenths;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(itemID);
        buf.writeInt(meta);
        buf.writeInt(buyTenths);
        buf.writeInt(sellTenths);
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!MinecraftServer.getServer().getConfigurationManager().isPlayerOpped(serverPlayer.username)) return;
        if (buyTenths == 0 && sellTenths == 0) return;

        GoodsConfig.savePrice(itemID, meta, buyTenths, sellTenths);
        ShopS2C.ensureConfig(true);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}