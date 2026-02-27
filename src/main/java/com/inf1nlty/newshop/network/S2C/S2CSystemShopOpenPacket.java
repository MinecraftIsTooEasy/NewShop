package com.inf1nlty.newshop.network.S2C;

import com.inf1nlty.newshop.client.gui.GuiShop;
import com.inf1nlty.newshop.client.state.ShopClientData;
import com.inf1nlty.newshop.client.state.SystemShopClientCatalog;
import com.inf1nlty.newshop.inventory.ContainerShopPlayer;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.Minecraft;
import net.minecraft.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: Open system shop GUI with catalog data.
 */
public class S2CSystemShopOpenPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "system_shop_open");

    private final int windowId;
    private final int balance;
    private final List<SystemShopClientCatalog.Entry> entries;

    public S2CSystemShopOpenPacket(PacketByteBuf buf) {
        this.windowId = buf.readInt();
        this.balance = buf.readInt();
        int count = buf.readInt();
        this.entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SystemShopClientCatalog.Entry e = new SystemShopClientCatalog.Entry();
            e.itemID = buf.readInt();
            e.meta = buf.readInt();
            e.buyTenths = buf.readInt();
            e.sellTenths = buf.readInt();
            entries.add(e);
        }
    }

    public S2CSystemShopOpenPacket(int windowId, int balance, List<SystemShopClientCatalog.Entry> entries) {
        this.windowId = windowId;
        this.balance = balance;
        this.entries = entries;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(windowId);
        buf.writeInt(balance);
        buf.writeInt(entries.size());
        for (SystemShopClientCatalog.Entry e : entries) {
            buf.writeInt(e.itemID);
            buf.writeInt(e.meta);
            buf.writeInt(e.buyTenths);
            buf.writeInt(e.sellTenths);
        }
    }

    @Override
    public void apply(EntityPlayer player) {
        ShopClientData.balance = balance;
        SystemShopClientCatalog.set(entries);
        Minecraft mc = Minecraft.getMinecraft();
        ContainerShopPlayer c = new ContainerShopPlayer(mc.thePlayer.inventory);
        c.windowId = windowId;
        mc.thePlayer.openContainer = c;
        mc.displayGuiScreen(new GuiShop(mc.thePlayer, c));
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}