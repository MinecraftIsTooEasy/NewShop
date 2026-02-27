package com.inf1nlty.newshop.network;

import com.inf1nlty.newshop.network.C2S.*;
import com.inf1nlty.newshop.network.S2C.*;
import moddedmite.rustedironcore.network.PacketReader;

/** Registers all Shop packets with RustedIronCore's PacketReader. */
public final class ShopPacketHandler {

    private ShopPacketHandler() {}

    public static void init()
    {
        PacketReader.registerServerPacketReader(C2SPurchaseRequestPacket.CHANNEL, C2SPurchaseRequestPacket::new);
        PacketReader.registerServerPacketReader(C2SSellRequestPacket.CHANNEL,     C2SSellRequestPacket::new);
        PacketReader.registerServerPacketReader(C2SOpenShopPacket.CHANNEL,        C2SOpenShopPacket::new);
        PacketReader.registerServerPacketReader(C2SGlobalOpenPacket.CHANNEL,      C2SGlobalOpenPacket::new);
        PacketReader.registerServerPacketReader(C2SGlobalBuyPacket.CHANNEL,       C2SGlobalBuyPacket::new);
        PacketReader.registerServerPacketReader(C2SListFromSlotPacket.CHANNEL,    C2SListFromSlotPacket::new);
        PacketReader.registerServerPacketReader(C2SGlobalListPacket.CHANNEL,      C2SGlobalListPacket::new);
        PacketReader.registerServerPacketReader(C2SGlobalUnlistPacket.CHANNEL,    C2SGlobalUnlistPacket::new);
        PacketReader.registerServerPacketReader(C2SMailboxOpenPacket.CHANNEL,     C2SMailboxOpenPacket::new);
        PacketReader.registerServerPacketReader(C2SSellToBuyOrderPacket.CHANNEL,  C2SSellToBuyOrderPacket::new);
        PacketReader.registerServerPacketReader(C2SSetPricePacket.CHANNEL,        C2SSetPricePacket::new);

        PacketReader.registerClientPacketReader(S2CBalanceSyncPacket.CHANNEL,     S2CBalanceSyncPacket::new);
        PacketReader.registerClientPacketReader(S2CSystemShopOpenPacket.CHANNEL,  S2CSystemShopOpenPacket::new);
        PacketReader.registerClientPacketReader(S2CInventorySyncPacket.CHANNEL,   S2CInventorySyncPacket::new);
        PacketReader.registerClientPacketReader(S2CGlobalSnapshotPacket.CHANNEL,  S2CGlobalSnapshotPacket::new);
        PacketReader.registerClientPacketReader(S2CMailboxOpenPacket.CHANNEL,     S2CMailboxOpenPacket::new);
    }
}