package com.inf1nlty.newshop.network.C2S;

import com.inf1nlty.newshop.GoodsConfig;
import com.inf1nlty.newshop.ShopListing;
import com.inf1nlty.newshop.network.ShopS2C;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayInputStream;

/** Admin sets buy/sell price for a system shop item. OP-only. */
public class C2SSetPricePacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "set_price");

    private final int itemID;
    private final int meta;
    private final int buyTenths;
    private final int sellTenths;
    private final byte[] nbtData;

    public C2SSetPricePacket(PacketByteBuf buf)
    {
        this.itemID     = buf.readInt();
        this.meta       = buf.readInt();
        this.buyTenths  = buf.readInt();
        this.sellTenths = buf.readInt();
        boolean hasNbt  = buf.readBoolean();
        if (hasNbt) {
            int len = buf.readUnsignedShort();
            this.nbtData = new byte[len];
            buf.readFully(this.nbtData);
        } else {
            this.nbtData = null;
        }
    }

    public C2SSetPricePacket(int itemID, int meta, int buyTenths, int sellTenths, byte[] nbtData)
    {
        this.itemID   = itemID;
        this.meta     = meta;
        this.buyTenths  = buyTenths;
        this.sellTenths = sellTenths;
        this.nbtData    = nbtData;
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        buf.writeInt(itemID);
        buf.writeInt(meta);
        buf.writeInt(buyTenths);
        buf.writeInt(sellTenths);
        if (nbtData != null && nbtData.length > 0) {
            buf.writeBoolean(true);
            buf.writeShort(nbtData.length);
            buf.write(nbtData, 0, nbtData.length);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public void apply(EntityPlayer player)
    {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!MinecraftServer.getServer().getConfigurationManager().isPlayerOpped(serverPlayer.username)) return;
        if (buyTenths == 0 && sellTenths == 0) return;

        NBTTagCompound nbt = null;
        if (nbtData != null && nbtData.length > 0) {
            try { nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(nbtData)); }
            catch (Exception ignored) {}
        }
        GoodsConfig.savePrice(itemID, meta, buyTenths, sellTenths, nbt);
        ShopS2C.ensureConfig(true);
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}