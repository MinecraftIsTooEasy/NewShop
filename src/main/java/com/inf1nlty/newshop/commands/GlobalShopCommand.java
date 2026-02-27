package com.inf1nlty.newshop.commands;

import com.inf1nlty.newshop.ShopConfig;
import com.inf1nlty.newshop.global.GlobalListing;
import com.inf1nlty.newshop.global.GlobalShopData;
import com.inf1nlty.newshop.util.Money;
import com.inf1nlty.newshop.util.MoneyManager;
import com.inf1nlty.newshop.util.PlayerIdentityUtil;
import com.inf1nlty.newshop.network.ShopS2C;
import net.minecraft.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * /gshop (alias /gs):
 *   open | sell <price> [amount] | buy <id[:meta]> <price> [amount] | my | unlist <id> | mailbox
 */
public class GlobalShopCommand extends CommandBase {

    @Override public String getCommandName() { return "gshop"; }
    @Override public List<String> getCommandAliases() { return Collections.singletonList("gs"); }
    @Override public String getCommandUsage(ICommandSender sender) { return "/gshop | sell <price> [amount] | buy <id[:meta]> <price> [amount] | my | unlist <id> | mailbox"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof ServerPlayer player)) {
            sender.sendChatToPlayer(ChatMessageComponent.createFromText("Only players"));
            return;
        }
        if (args.length == 0) { ShopS2C.openGlobalShop(player); return; }
        switch (args[0].toLowerCase()) {
            case "sell": case "s": handleSell(player, args); break;
            case "buy":  case "b": handleBuyOrder(player, args); break;
            case "my":   case "m": handleMy(player); break;
            case "unlist": case "u": handleUnlist(player, args); break;
            case "mailbox": case "mb": ShopS2C.openMailbox(player); break;
            default: ShopS2C.sendResult(player, "gshop.sell.usage");
        }
    }

    private void handleSell(ServerPlayer player, String[] args)
    {
        if (player.dimension != 0) { ShopS2C.sendResult(player, "gshop.sell.fail_only_overworld"); return; }
        if (args.length < 2) { ShopS2C.sendResult(player, "gshop.sell.usage"); return; }
        ItemStack hand = player.inventory.getCurrentItemStack();
        if (hand == null) { ShopS2C.sendResult(player, "gshop.listing.add.fail_no_item"); return; }

        if (!args[1].matches("-?\\d+(\\.\\d)?")) { ShopS2C.sendResult(player, "gshop.listing.add.fail_price"); return; }
        int priceTenths = parseTenths(args[1]);
        if (priceTenths <= 0) { ShopS2C.sendResult(player, "gshop.listing.add.fail_price"); return; }

        int desired = hand.stackSize;
        if (args.length >= 3) {
            if (!args[2].matches("\\d+")) { ShopS2C.sendResult(player, "gshop.listing.add.fail_amount"); return; }
            desired = Integer.parseInt(args[2]);
        }
        if (desired <= 0 || desired > hand.stackSize) { ShopS2C.sendResult(player, "gshop.listing.add.fail_stack"); return; }

        GlobalListing listing = GlobalShopData.addSellOrder(player, hand.itemID, hand.getItemDamage(), desired, priceTenths);
        hand.stackSize -= desired;
        if (hand.stackSize <= 0) player.inventory.mainInventory[player.inventory.currentItem] = null;

        ShopS2C.sendResult(player, "gshop.listing.add.success|itemID=" + listing.itemId + "|meta=" + listing.meta + "|count=" + listing.amount + "|price=" + Money.format(listing.priceTenths));
        if (ShopConfig.ANNOUNCE_GLOBAL_LISTING.getBooleanValue()) {
            broadcastResultAll(
                "gshop.listing.announce.line1.sell|player=" + player.username + "|itemID=" + listing.itemId + "|meta=" + listing.meta + "|count=" + listing.amount,
                "gshop.listing.announce.line2|price=" + Money.format(listing.priceTenths) + "|type=Sell"
            );
        }
    }

    private void handleBuyOrder(ServerPlayer player, String[] args)
    {
        if (args.length < 3) { ShopS2C.sendResult(player, "gshop.buy.usage"); return; }
        int itemId, meta = 0;
        String idMeta = args[1];
        if (idMeta.contains(":")) {
            String[] parts = idMeta.split(":");
            if (!parts[0].matches("\\d+") || !parts[1].matches("\\d+")) { ShopS2C.sendResult(player, "gshop.buy.usage"); return; }
            itemId = Integer.parseInt(parts[0]);
            meta = Integer.parseInt(parts[1]);
        } else {
            if (!idMeta.matches("\\d+")) { ShopS2C.sendResult(player, "gshop.buy.usage"); return; }
            itemId = Integer.parseInt(idMeta);
        }

        if (!args[2].matches("-?\\d+(\\.\\d)?")) { ShopS2C.sendResult(player, "gshop.buy.usage"); return; }
        int priceTenths = parseTenths(args[2]);
        if (priceTenths <= 0) { ShopS2C.sendResult(player, "gshop.buy.usage"); return; }

        int amount = 1;
        if (args.length >= 4) {
            if (!args[3].matches("\\d+")) { ShopS2C.sendResult(player, "gshop.buy.usage"); return; }
            amount = Integer.parseInt(args[3]);
        }
        if (amount <= 0) { ShopS2C.sendResult(player, "gshop.buy.usage"); return; }

        int totalCost = priceTenths * amount;
        if (MoneyManager.getBalanceTenths(player) < totalCost) { ShopS2C.sendResult(player, "gshop.buyorder.not_enough_money_for_post"); return; }
        MoneyManager.addTenths(player, -totalCost);
        ShopS2C.syncBalance(player);

        GlobalListing listing = GlobalShopData.addBuyOrder(player, itemId, meta, amount, priceTenths);
        ShopS2C.sendResult(player, "gshop.buyorder.add.success|itemID=" + listing.itemId + "|meta=" + listing.meta + "|count=" + listing.amount + "|price=" + Money.format(listing.priceTenths));
        if (ShopConfig.ANNOUNCE_GLOBAL_LISTING.getBooleanValue()) {
            broadcastResultAll(
                "gshop.listing.announce.line1.buy|player=" + player.username + "|itemID=" + listing.itemId + "|meta=" + listing.meta + "|count=" + listing.amount,
                "gshop.listing.announce.line2|price=" + Money.format(listing.priceTenths) + "|type=Buy"
            );
        }
    }

    private void broadcastResultAll(String... messages)
    {
        for (Object obj : net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList)
        {
            ServerPlayer onlinePlayer = (ServerPlayer) obj;
            for (String message : messages) ShopS2C.sendResult(onlinePlayer, message);
        }
    }

    private void handleMy(ServerPlayer player)
    {
        UUID id = PlayerIdentityUtil.getOfflineUUID(player.username);
        List<GlobalListing> mine = GlobalShopData.byOwner(id);
        ShopS2C.sendResult(player, "gshop.list.mine.header");
        for (GlobalListing g : mine) {
            ShopS2C.sendResult(player, "gshop.list.mine.line|id=" + g.listingId
                    + "|itemID=" + g.itemId + "|meta=" + g.meta
                    + "|amount=" + (g.amount == -1 ? "∞" : g.amount)
                    + "|price=" + Money.format(g.priceTenths)
                    + (g.isBuyOrder ? "|type=Buy" : "|type=Sell"));
        }
    }

    private void handleUnlist(ServerPlayer player, String[] args)
    {
        if (args.length < 2 || !args[1].matches("\\d+")) { ShopS2C.sendResult(player, "gshop.unlist.usage"); return; }
        int id = Integer.parseInt(args[1]);

        GlobalListing removed = GlobalShopData.remove(id, PlayerIdentityUtil.getOfflineUUID(player.username));
        if (removed != null) {
            if (removed.isBuyOrder) {
                ShopS2C.sendResult(player, "gshop.buyorder.remove.success|id=" + removed.listingId
                        + "|itemID=" + removed.itemId + "|meta=" + removed.meta
                        + "|count=" + (removed.amount == -1 ? "unlimited" : removed.amount));
            } else {
                refund(player, removed);
                ShopS2C.sendResult(player, "gshop.listing.remove.success|id=" + removed.listingId
                        + "|itemID=" + removed.itemId + "|meta=" + removed.meta
                        + "|count=" + removed.amount);
            }
        } else {
            GlobalListing gl = GlobalShopData.get(id);
            if (gl == null) ShopS2C.sendResult(player, "gshop.listing.remove.not_found|id=" + id);
            else ShopS2C.sendResult(player, "gshop.listing.remove.not_owner|id=" + id);
        }
    }

    private void refund(ServerPlayer player, GlobalListing global)
    {
        Item item = (global.itemId >= 0 && global.itemId < Item.itemsList.length) ? Item.itemsList[global.itemId] : null;
        if (item == null) return;
        int remaining = global.amount;
        int max = item.maxStackSize;
        while (remaining > 0)
        {
            int take = Math.min(max, remaining);
            ItemStack stack = new ItemStack(item, take, global.meta);
            if (global.nbt != null) stack.stackTagCompound = (NBTTagCompound) global.nbt.copy();
            if (!player.inventory.addItemStackToInventory(stack)) player.dropPlayerItem(stack);
            remaining -= take;
        }
        ShopS2C.syncInventory(player);
    }

    private int parseTenths(String raw)
    {
        raw = raw.trim();

        if (raw.contains("."))
        {
            String[] parts = raw.split("\\.");

            if (parts.length != 2) throw new NumberFormatException();

            int whole   = Integer.parseInt(parts[0]);

            String fracStr = parts[1];

            if (fracStr.length() > 1) fracStr = fracStr.substring(0, 1);

            int frac = Integer.parseInt(fracStr);

            return whole < 0 ? whole * 10 - frac : whole * 10 + frac;
        }

        return Integer.parseInt(raw) * 10;
    }
}