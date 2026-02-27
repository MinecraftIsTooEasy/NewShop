package com.inf1nlty.newshop.network.S2C;

import com.inf1nlty.newshop.client.gui.GuiGlobalShop;
import com.inf1nlty.newshop.client.gui.GuiGlobalShop.GlobalListingClient;
import com.inf1nlty.newshop.client.state.ShopClientData;
import com.inf1nlty.newshop.inventory.ContainerShopPlayer;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.Minecraft;
import net.minecraft.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: Global shop snapshot (listings + balance).
 */
public class S2CGlobalSnapshotPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "global_snapshot");

    private final boolean isOpenRequest;
    private final int windowId;
    private final int balance;
    private final List<GlobalListingClient> listings;

    public S2CGlobalSnapshotPacket(PacketByteBuf buf) {
        this.isOpenRequest = buf.readBoolean();
        this.windowId = buf.readInt();
        this.balance = buf.readInt();
        int cnt = buf.readInt();
        this.listings = new ArrayList<>(cnt);
        for (int i = 0; i < cnt; i++) {
            GlobalListingClient listingClient = new GlobalListingClient();
            listingClient.listingId = buf.readInt();
            listingClient.itemId = buf.readInt();
            listingClient.meta = buf.readInt();
            listingClient.amount = buf.readInt();
            listingClient.priceTenths = buf.readInt();
            listingClient.owner = buf.readUTF();
            listingClient.isBuyOrder = buf.readBoolean();
            boolean hasNbt = buf.readBoolean();
            if (hasNbt) {
                int len = buf.readUnsignedShort();
                byte[] data = new byte[len];
                buf.readFully(data);
                listingClient.nbtCompressed = data;
            }
            listings.add(listingClient);
        }
    }

    public S2CGlobalSnapshotPacket(boolean isOpenRequest, int windowId, int balance, List<GlobalListingClient> listings) {
        this.isOpenRequest = isOpenRequest;
        this.windowId = windowId;
        this.balance = balance;
        this.listings = listings;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(isOpenRequest);
        buf.writeInt(windowId);
        buf.writeInt(balance);
        buf.writeInt(listings.size());
        for (GlobalListingClient gl : listings) {
            buf.writeInt(gl.listingId);
            buf.writeInt(gl.itemId);
            buf.writeInt(gl.meta);
            buf.writeInt(gl.amount);
            buf.writeInt(gl.priceTenths);
            buf.writeUTF(gl.owner);
            buf.writeBoolean(gl.isBuyOrder);
            if (gl.nbtCompressed != null) {
                buf.writeBoolean(true);
                buf.writeShort(gl.nbtCompressed.length);
                buf.write(gl.nbtCompressed, 0, gl.nbtCompressed.length);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public void apply(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        ShopClientData.balance = balance;
        GuiGlobalShop.setSnapshot(listings);
        if (mc.currentScreen instanceof GuiGlobalShop shopGui) {
            shopGui.refreshListings();
        } else if (isOpenRequest) {
            ContainerShopPlayer container = new ContainerShopPlayer(mc.thePlayer.inventory);
            container.windowId = windowId;
            mc.thePlayer.openContainer = container;
            mc.displayGuiScreen(new GuiGlobalShop(mc.thePlayer, container));
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}