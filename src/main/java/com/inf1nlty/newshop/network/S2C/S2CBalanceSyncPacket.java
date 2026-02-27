package com.inf1nlty.newshop.network.S2C;

import com.inf1nlty.newshop.client.state.ShopClientData;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.ResourceLocation;

/**
 * S2C: Sync balance only.
 */
public class S2CBalanceSyncPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "balance_sync");

    private final int balance;

    public S2CBalanceSyncPacket(PacketByteBuf buf) {
        this.balance = buf.readInt();
    }

    public S2CBalanceSyncPacket(int balance) {
        this.balance = balance;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(balance);
    }

    @Override
    public void apply(EntityPlayer player) {
        ShopClientData.balance = balance;
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}