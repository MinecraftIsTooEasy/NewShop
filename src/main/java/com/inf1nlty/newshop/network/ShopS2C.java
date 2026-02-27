package com.inf1nlty.newshop.network;

import com.inf1nlty.newshop.GoodsConfig;
import com.inf1nlty.newshop.ShopConfig;
import com.inf1nlty.newshop.ShopListing;
import com.inf1nlty.newshop.client.gui.GuiGlobalShop.GlobalListingClient;
import com.inf1nlty.newshop.client.state.SystemShopClientCatalog;
import com.inf1nlty.newshop.global.GlobalListing;
import com.inf1nlty.newshop.global.GlobalShopData;
import com.inf1nlty.newshop.inventory.ContainerMailbox;
import com.inf1nlty.newshop.inventory.ContainerShopPlayer;
import com.inf1nlty.newshop.network.S2C.*;
import com.inf1nlty.newshop.util.MailboxManager;
import com.inf1nlty.newshop.util.Money;
import com.inf1nlty.newshop.util.MoneyManager;
import com.inf1nlty.newshop.util.PlayerIdentityUtil;
import moddedmite.rustedironcore.network.Network;
import net.minecraft.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

/** System & Global shop server-side logic. */
public class ShopS2C
{
    private static final List<ServerPlayer> GLOBAL_VIEWERS = new ArrayList<>();
    private static long lastConfigTouch = 0L;

    private ShopS2C() {}

    public static void ensureConfig(boolean force)
    {
        long now = System.currentTimeMillis();
        if (force || now - lastConfigTouch > 4000 || GoodsConfig.getItems().isEmpty())
        {
            GoodsConfig.reload();
            lastConfigTouch = now;
        }
    }

    // ===== System Shop =====

    public static void openSystemShop(ServerPlayer player)
    {
        player.currentWindowId = (player.currentWindowId % 100) + 1;
        ContainerShopPlayer container = new ContainerShopPlayer(player.inventory);
        container.windowId = player.currentWindowId;
        player.openContainer = container;
        sendSystemOpen(player);
    }

    private static void sendSystemOpen(ServerPlayer player)
    {
        int balance = MoneyManager.getBalanceTenths(player);
        List<ShopListing> items = GoodsConfig.getItems();
        List<SystemShopClientCatalog.Entry> entries = new ArrayList<>(items.size());

        for (ShopListing shopItem : items)
        {
            SystemShopClientCatalog.Entry entry = new SystemShopClientCatalog.Entry();
            entry.itemID    = shopItem.itemID;
            entry.meta      = shopItem.damage;
            entry.buyTenths  = shopItem.buyPriceTenths;
            entry.sellTenths = shopItem.sellPriceTenths;
            entries.add(entry);
        }

        Network.sendToClient(player, new S2CSystemShopOpenPacket(player.openContainer.windowId, balance, entries));
        syncInventory(player);
    }

    public static void buySystem(ServerPlayer player, int itemID, int meta, int count)
    {
        if (count <= 0) count = 1;

        ShopListing shopItem = GoodsConfig.get(itemID, meta);
        if (shopItem == null) { sendResult(player, "shop.item_not_supported"); return; }

        int cost = shopItem.buyPriceTenths * count;
        if (cost <= 0) { sendResult(player, "shop.item_not_supported"); return; }
        if (MoneyManager.getBalanceTenths(player) < cost) { sendResult(player, "shop.not_enough_money"); return; }

        ItemStack purchase = new ItemStack(shopItem.itemStack.getItem(), count, shopItem.damage);

        if (itemID == 383 && (meta == 90 || meta == 91 || meta == 92 || meta == 100 || meta == 95))
        {
            if (purchase.stackTagCompound == null) purchase.stackTagCompound = new NBTTagCompound();
            purchase.stackTagCompound.setBoolean("ShopBaby", true);
        }

        if (!canFit(player, purchase)) { sendResult(player, "shop.inventory.full"); return; }

        player.inventory.addItemStackToInventory(purchase);
        MoneyManager.addTenths(player, -cost);
        sendResult(player, "shop.buy.success|itemID=" + purchase.itemID + "|meta=" + purchase.getItemDamage() + "|count=" + count + "|cost=" + Money.format(cost));
        syncInventory(player);
        syncBalance(player);
    }

    public static void sellSystem(ServerPlayer player, int itemID, int count, int slotIndex)
    {
        if (count <= 0) count = 1;
        if (slotIndex < 0 || slotIndex >= player.inventory.mainInventory.length) { sendResult(player, "shop.failed"); return; }

        ItemStack target = player.inventory.mainInventory[slotIndex];
        if (target == null || target.itemID != itemID) { sendResult(player, "shop.failed"); return; }

        ShopListing shopItem = GoodsConfig.get(itemID, target.getItemDamage());

        if (!ShopConfig.FORCE_SELL_UNLISTED.getBooleanValue())
        {
            if (shopItem == null || shopItem.sellPriceTenths <= 0) { sendResult(player, "shop.item_not_supported"); return; }
        }

        if (shopItem == null)
        {
            int removed = removeFromSlot(player, slotIndex, count);
            if (removed > 0)
            {
                sendResult(player, "shop.force.dispose|itemID=" + target.itemID + "|meta=" + target.getItemDamage() + "|count=" + removed);
                syncInventory(player);
            }
            else sendResult(player, "shop.failed");
            return;
        }

        int removed = removeFromSlot(player, slotIndex, count);
        if (removed > 0)
        {
            int gain = shopItem.sellPriceTenths * removed;
            if (gain > 0) MoneyManager.addTenths(player, gain);
            syncBalance(player);
            sendResult(player, "shop.sell.success|itemID=" + target.itemID + "|meta=" + target.getItemDamage() + "|count=" + removed + "|gain=" + Money.format(gain));
            syncInventory(player);
        }
        else sendResult(player, "shop.failed");
    }

    // ===== Global Shop =====

    public static void openGlobalShop(ServerPlayer player)
    {
        if (!GLOBAL_VIEWERS.contains(player)) GLOBAL_VIEWERS.add(player);
        player.currentWindowId = (player.currentWindowId % 100) + 1;
        ContainerShopPlayer container = new ContainerShopPlayer(player.inventory);
        container.windowId = player.currentWindowId;
        player.openContainer = container;
        sendGlobalSnapshot(player, true);
    }

    public static void broadcastGlobalSnapshot()
    {
        for (ServerPlayer viewer : new ArrayList<>(GLOBAL_VIEWERS))
        {
            if (viewer.openContainer == null) continue;
            sendGlobalSnapshot(viewer, false);
        }
    }

    private static void sendGlobalSnapshot(ServerPlayer player, boolean isOpenRequest)
    {
        int balance = MoneyManager.getBalanceTenths(player);
        List<GlobalListing> listings = GlobalShopData.all();
        List<GlobalListingClient> clientListings = new ArrayList<>(listings.size());

        for (GlobalListing listing : listings)
        {
            GlobalListingClient clientListing = new GlobalListingClient();
            clientListing.listingId    = listing.listingId;
            clientListing.itemId       = listing.itemId;
            clientListing.meta         = listing.meta;
            clientListing.amount       = listing.amount;
            clientListing.priceTenths  = listing.priceTenths;
            clientListing.owner        = listing.ownerName;
            clientListing.isBuyOrder   = listing.isBuyOrder;
            if (listing.nbt != null) clientListing.nbtCompressed = compressNBT(listing.nbt);
            clientListings.add(clientListing);
        }

        Network.sendToClient(player, new S2CGlobalSnapshotPacket(isOpenRequest, player.openContainer.windowId, balance, clientListings));
    }

    private static byte[] compressNBT(NBTTagCompound tag)
    {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream())
        {
            CompressedStreamTools.writeCompressed(tag, buf);
            return buf.toByteArray();
        }
        catch (Exception ignored) { return new byte[0]; }
    }

    public static void buyGlobal(ServerPlayer buyer, int listingId, int requestedCount)
    {
        GlobalListing gl = GlobalShopData.get(listingId);
        if (gl == null)                                                          { sendResult(buyer, "gshop.buy.not_found");         return; }
        if (gl.isBuyOrder)                                                       { sendResult(buyer, "gshop.buyorder.not_supported"); return; }
        if (PlayerIdentityUtil.getOfflineUUID(buyer.username).equals(gl.ownerUUID)) { sendResult(buyer, "gshop.buy.self_not_allowed"); return; }

        if (requestedCount <= 0) requestedCount = 1;
        int tentativeCount = Math.min(requestedCount, gl.amount);
        if (tentativeCount <= 0) { sendResult(buyer, "gshop.buy.not_found"); return; }

        int tentativeCost = gl.priceTenths * tentativeCount;
        if (MoneyManager.getBalanceTenths(buyer) < tentativeCost) { sendResult(buyer, "gshop.buy.not_enough_money"); return; }

        Item item = (gl.itemId >= 0 && gl.itemId < Item.itemsList.length) ? Item.itemsList[gl.itemId] : null;
        if (item == null) { sendResult(buyer, "gshop.buy.not_found"); return; }

        ItemStack testStack = new ItemStack(item, tentativeCount, gl.meta);
        if (gl.nbt != null) testStack.stackTagCompound = (NBTTagCompound) gl.nbt.copy();
        if (!canFit(buyer, testStack)) { sendResult(buyer, "gshop.inventory.full"); return; }

        int bought = GlobalShopData.buy(listingId, tentativeCount);
        if (bought <= 0) { sendResult(buyer, "gshop.buy.not_found"); return; }

        int finalCost = gl.priceTenths * bought;
        if (MoneyManager.getBalanceTenths(buyer) < finalCost) { sendResult(buyer, "gshop.buy.not_enough_money"); return; }

        ItemStack deliver = new ItemStack(item, bought, gl.meta);
        if (gl.nbt != null) deliver.stackTagCompound = (NBTTagCompound) gl.nbt.copy();

        buyer.inventory.addItemStackToInventory(deliver);
        MoneyManager.addTenths(buyer, -finalCost);
        syncBalance(buyer);

        int revenue = gl.priceTenths * bought;
        MoneyManager.addTenths(gl.ownerUUID, revenue);

        for (Object obj : buyer.mcServer.getConfigurationManager().playerEntityList)
        {
            ServerPlayer onlinePlayer = (ServerPlayer) obj;
            if (PlayerIdentityUtil.getOfflineUUID(onlinePlayer.username).equals(gl.ownerUUID))
            {
                sendResult(onlinePlayer, "gshop.sale.success|buyer=" + buyer.username
                        + "|revenue=" + Money.format(revenue)
                        + "|itemID="  + deliver.itemID
                        + "|meta="    + deliver.getItemDamage()
                        + "|count="   + bought);
                syncInventory(onlinePlayer);
                sendGlobalSnapshot(onlinePlayer, false);
                syncBalance(onlinePlayer);
            }
        }

        sendResult(buyer, "gshop.buy.success|cost="   + Money.format(finalCost)
                + "|seller=" + gl.ownerName
                + "|itemID=" + deliver.itemID
                + "|meta="   + deliver.getItemDamage()
                + "|count="  + bought);

        broadcastGlobalSnapshot();
        syncInventory(buyer);
    }

    public static void listGlobal(ServerPlayer player, int itemId, int meta, int amount, int priceTenths)
    {
        if (priceTenths > 0)
        {
            GlobalShopData.addSellOrder(player, itemId, meta, amount, priceTenths);
        }
        else
        {
            if (amount <= 0)    { sendResult(player, "gshop.buyorder.amount_required"); return; }
            int buyPrice = -priceTenths;
            if (buyPrice <= 0)  { sendResult(player, "gshop.buyorder.price_required");  return; }

            int totalCost = buyPrice * amount;
            if (MoneyManager.getBalanceTenths(player) < totalCost) { sendResult(player, "gshop.buyorder.not_enough_money_for_post"); return; }

            MoneyManager.addTenths(player, -totalCost);
            syncBalance(player);
            sendResult(player, "gshop.buyorder.buyer_sync|delta=" + Money.format(-totalCost));
            GlobalShopData.addBuyOrder(player, itemId, meta, amount, buyPrice);
        }

        broadcastGlobalSnapshot();
    }

    public static void unlistGlobal(ServerPlayer player, int listingId)
    {
        GlobalListing gl = GlobalShopData.get(listingId);
        if (gl == null)
        {
            sendResult(player, "gshop.listing.remove.not_found|id=" + listingId);
            return;
        }

        if (!PlayerIdentityUtil.getOfflineUUID(player.username).equals(gl.ownerUUID))
        {
            sendResult(player, "gshop.listing.remove.not_owner|id=" + listingId);
            return;
        }

        if (gl.isBuyOrder)
        {
            if (gl.amount > 0 && gl.priceTenths > 0)
            {
                int refund = gl.priceTenths * gl.amount;
                MoneyManager.addTenths(player, refund);
                syncBalance(player);
                sendResult(player, "gshop.buyorder.refund|amount=" + gl.amount + "|refund=" + Money.format(refund));
            }

            GlobalShopData.remove(listingId, PlayerIdentityUtil.getOfflineUUID(player.username));
            sendResult(player, "gshop.buyorder.remove.success|itemID=" + gl.itemId
                    + "|meta="  + gl.meta
                    + "|count=" + gl.amount
                    + "|id="    + gl.listingId);
            broadcastGlobalSnapshot();
            return;
        }

        Item item = (gl.itemId >= 0 && gl.itemId < Item.itemsList.length) ? Item.itemsList[gl.itemId] : null;
        if (item == null)
        {
            sendResult(player, "gshop.unlist.item.invalid|id=" + gl.listingId
                    + "|itemID=" + gl.itemId
                    + "|meta="   + gl.meta
                    + "|count="  + gl.amount);
            broadcastGlobalSnapshot();
            return;
        }

        GlobalListing removed = GlobalShopData.remove(listingId, PlayerIdentityUtil.getOfflineUUID(player.username));
        if (removed == null) return;

        int remaining = removed.amount;
        int maxStack  = item.maxStackSize;

        while (remaining > 0)
        {
            int take = Math.min(maxStack, remaining);
            ItemStack stack = new ItemStack(item, take, removed.meta);
            if (removed.nbt != null) stack.stackTagCompound = (NBTTagCompound) removed.nbt.copy();

            if (!canFit(player, stack))
            {
                sendResult(player, "gshop.unlist.inventory.full");
                player.dropPlayerItem(stack);
            }
            else
            {
                player.inventory.addItemStackToInventory(stack);
            }

            remaining -= take;
        }

        sendResult(player, "gshop.listing.remove.success|itemID=" + removed.itemId
                + "|meta="  + removed.meta
                + "|count=" + removed.amount
                + "|id="    + removed.listingId);
        syncInventory(player);
        broadcastGlobalSnapshot();
    }

    // ===== Mailbox =====

    public static void openMailbox(ServerPlayer player)
    {
        UUID playerId = PlayerIdentityUtil.getOfflineUUID(player.username);
        ContainerMailbox container = new ContainerMailbox(player.inventory, MailboxManager.getMailbox(playerId));
        container.windowId = (player.currentWindowId % 100) + 1;
        player.openContainer = container;
        Network.sendToClient(player, new S2CMailboxOpenPacket(container.windowId, MailboxManager.getMailbox(playerId)));
        syncInventory(player);
    }

    // ===== Buy Order fulfillment =====

    public static void sellToBuyOrder(ServerPlayer seller, int listingId, int count, int slotIndex)
    {
        GlobalListing order = GlobalShopData.get(listingId);

        if (order == null || !order.isBuyOrder)                                     { sendResult(seller, "gshop.buyorder.not_found");         return; }
        if (PlayerIdentityUtil.getOfflineUUID(seller.username).equals(order.ownerUUID)) { sendResult(seller, "gshop.buy.self_not_allowed");       return; }
        if (slotIndex < 0 || slotIndex >= seller.inventory.mainInventory.length)    { sendResult(seller, "gshop.listing.add.fail_no_item");     return; }

        ItemStack stack = seller.inventory.mainInventory[slotIndex];
        if (stack == null)                                                           { sendResult(seller, "gshop.listing.add.fail_no_item");     return; }
        if (stack.itemID != order.itemId || stack.getItemDamage() != order.meta)    { sendResult(seller, "gshop.buyorder.wrong_item");           return; }

        int give;
        if (order.amount == -1)
        {
            give = Math.min(stack.stackSize, count);
        }
        else
        {
            if (order.amount <= 0)
            {
                GlobalShopData.remove(listingId, order.ownerUUID);
                broadcastGlobalSnapshot();
                sendResult(seller, "gshop.buyorder.fulfilled");
                return;
            }

            give = Math.min(stack.stackSize, Math.min(count, order.amount));
            if (give <= 0) { sendResult(seller, "gshop.buyorder.fulfilled"); return; }
        }

        UUID buyerId = order.ownerUUID;

        stack.stackSize -= give;
        if (stack.stackSize <= 0) seller.inventory.mainInventory[slotIndex] = null;

        ItemStack deliver = new ItemStack(stack.getItem(), give, stack.getItemDamage());
        if (stack.stackTagCompound != null) deliver.stackTagCompound = (NBTTagCompound) stack.stackTagCompound.copy();
        MailboxManager.deliver(buyerId, deliver);

        ServerPlayer onlineBuyer = null;
        for (Object obj : seller.mcServer.getConfigurationManager().playerEntityList)
        {
            ServerPlayer onlinePlayer = (ServerPlayer) obj;
            if (PlayerIdentityUtil.getOfflineUUID(onlinePlayer.username).equals(buyerId))
            {
                onlineBuyer = onlinePlayer;
                break;
            }
        }

        if (onlineBuyer != null)
        {
            syncInventory(onlineBuyer);
            sendGlobalSnapshot(onlineBuyer, false);
            syncBalance(onlineBuyer);
        }

        MoneyManager.addTenths(seller, order.priceTenths * give);
        syncBalance(seller);

        sendResult(seller, "gshop.buyorder.sell.success|itemID=" + deliver.itemID
                + "|meta="   + deliver.getItemDamage()
                + "|count="  + give
                + "|buyer="  + order.ownerName);

        if (order.amount != -1)
        {
            order.amount -= give;
            if (order.amount <= 0)
            {
                GlobalShopData.remove(listingId, order.ownerUUID);
                broadcastGlobalSnapshot();
                sendResult(seller, "gshop.buyorder.fulfilled");
                syncInventory(seller);
                return;
            }
            else GlobalShopData.save();
        }

        broadcastGlobalSnapshot();
        syncInventory(seller);
    }

    // ===== Slot-based listing =====

    public static void listGlobalFromSlot(ServerPlayer player, int itemId, int meta, int amount, int priceTenths, int slotIndex)
    {
        if (slotIndex < 0 || slotIndex >= player.inventory.mainInventory.length) { sendResult(player, "gshop.listing.add.fail_no_item"); return; }

        ItemStack slot = player.inventory.mainInventory[slotIndex];
        if (slot == null || slot.itemID != itemId || slot.getItemDamage() != meta) { sendResult(player, "gshop.listing.add.fail_no_item"); return; }
        if (priceTenths <= 0)                                                       { sendResult(player, "gshop.listing.add.fail_price");   return; }
        if (amount <= 0 || amount > slot.stackSize)                                 { sendResult(player, "gshop.listing.add.fail_stack");   return; }

        ItemStack nbtSource = slot.copy();
        int removed = removeFromSlot(player, slotIndex, amount);
        if (removed <= 0) { sendResult(player, "gshop.listing.add.fail_stack"); return; }

        GlobalListing listing = GlobalShopData.addSellOrder(player, itemId, meta, removed, priceTenths, nbtSource);
        if (listing == null)
        {
            // cap exceeded — refund the deducted items
            ItemStack refund = nbtSource.copy();
            refund.stackSize = removed;
            if (!player.inventory.addItemStackToInventory(refund)) player.dropPlayerItem(refund);
            sendResult(player, "gshop.listing.add.fail_cap");
            syncInventory(player);
            return;
        }
        sendResult(player, "gshop.listing.add.success|itemID=" + itemId + "|meta=" + meta + "|count=" + listing.amount + "|price=" + Money.format(listing.priceTenths));
        broadcastGlobalSnapshot();
        syncInventory(player);
    }

    /** Lists a sell order from a slot in the player's currently open external container (chest, etc.). */
    public static void listGlobalFromContainerSlot(ServerPlayer player, int itemId, int meta, int amount, int priceTenths, int windowId, int containerSlotNumber)
    {
        if (player.openContainer == null || player.openContainer.windowId != windowId) { sendResult(player, "gshop.listing.add.fail_no_item"); return; }
        if (priceTenths <= 0)                                                           { sendResult(player, "gshop.listing.add.fail_price");   return; }

        Slot containerSlot = player.openContainer.getSlot(containerSlotNumber);
        if (!containerSlot.getHasStack()) { sendResult(player, "gshop.listing.add.fail_no_item"); return; }

        ItemStack slotStack = containerSlot.getStack();
        if (slotStack.itemID != itemId || slotStack.getItemDamage() != meta) { sendResult(player, "gshop.listing.add.fail_no_item"); return; }
        if (amount <= 0 || amount > slotStack.stackSize)                     { sendResult(player, "gshop.listing.add.fail_stack");   return; }

        ItemStack nbtSource = slotStack.copy();
        boolean wasNulled = slotStack.stackSize == amount;
        slotStack.stackSize -= amount;
        if (slotStack.stackSize <= 0) containerSlot.putStack(null);
        else containerSlot.onSlotChanged();
        player.openContainer.detectAndSendChanges();

        GlobalListing listing = GlobalShopData.addSellOrder(player, itemId, meta, amount, priceTenths, nbtSource);
        if (listing == null)
        {
            // cap exceeded — restore the deducted items back into the slot
            ItemStack restore = nbtSource.copy();
            restore.stackSize = amount;
            if (wasNulled)
            {
                containerSlot.putStack(restore);
            }
            else
            {
                slotStack.stackSize += amount;
                containerSlot.onSlotChanged();
            }
            player.openContainer.detectAndSendChanges();
            sendResult(player, "gshop.listing.add.fail_cap");
            syncInventory(player);
            return;
        }
        sendResult(player, "gshop.listing.add.success|itemID=" + itemId + "|meta=" + meta + "|count=" + listing.amount + "|price=" + Money.format(listing.priceTenths));
        broadcastGlobalSnapshot();
        syncInventory(player);
    }

    // ===== Shared Utility =====

    private static int removeFromSlot(ServerPlayer player, int slotIndex, int want)
    {
        ItemStack stack = player.inventory.mainInventory[slotIndex];
        if (stack == null) return 0;
        int removed = Math.min(stack.stackSize, want);
        stack.stackSize -= removed;
        if (stack.stackSize <= 0) player.inventory.mainInventory[slotIndex] = null;
        return removed;
    }

    private static boolean canFit(ServerPlayer player, ItemStack stack)
    {
        if (stack == null || stack.stackSize <= 0) return true;

        int remaining = stack.stackSize;
        int maxStack  = stack.getItem().maxStackSize;
        ItemStack[] inv = player.inventory.mainInventory;

        for (ItemStack slot : inv)
        {
            if (slot == null) continue;
            if (slot.itemID == stack.itemID && slot.getItemDamage() == stack.getItemDamage() && nbtEqual(slot.stackTagCompound, stack.stackTagCompound))
            {
                int space = Math.min(maxStack, slot.getMaxStackSize()) - slot.stackSize;
                if (space > 0)
                {
                    remaining -= Math.min(space, remaining);
                    if (remaining <= 0) return true;
                }
            }
        }

        for (ItemStack slot : inv)
        {
            if (slot == null)
            {
                remaining -= Math.min(maxStack, remaining);
                if (remaining <= 0) return true;
            }
        }

        return false;
    }

    private static boolean nbtEqual(NBTTagCompound a, NBTTagCompound b)
    {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public static void sendResult(ServerPlayer player, String keyWithParams)
    {
        String key = keyWithParams.contains("|") ? keyWithParams.substring(0, keyWithParams.indexOf('|')) : keyWithParams;

        if (CLIENT_LOCALIZE_KEYS.contains(key))
        {
            player.sendChatToPlayer(ChatMessageComponent.createFromText(keyWithParams));
            return;
        }

        EnumChatFormatting color = EnumChatFormatting.YELLOW;
        Map<String, String> paramMap = new LinkedHashMap<>();

        if (keyWithParams.contains("|"))
        {
            String[] parts = keyWithParams.split("\\|");
            key = parts[0];
            for (int i = 1; i < parts.length; i++)
            {
                String[] kv = parts[i].split("=", 2);
                if (kv.length == 2) paramMap.put(kv[0], kv[1]);
            }
        }

        List<String> argNames = ARG_ORDER.getOrDefault(key, ARG_ORDER.get("default"));
        Object[] args = argNames.stream().map(name -> paramMap.getOrDefault(name, "")).toArray();

        if (key.startsWith("shop.failed") || key.contains("not_enough_money") || key.contains("not_supported") || key.contains("full") || key.contains("not_found") || key.contains("fail") || key.contains("not_owner"))
            color = EnumChatFormatting.RED;
        else if (key.contains("success") || key.contains("set") || key.contains("add") || key.contains("buy") || key.contains("sell") || key.contains("refund") || key.contains("dispose"))
            color = EnumChatFormatting.GREEN;
        else if (key.contains("announce") || key.contains("show") || key.contains("title") || key.contains("page"))
            color = EnumChatFormatting.AQUA;

        ChatMessageComponent msg = args.length == 0
                ? createMessage(key, color, false, false, false)
                : createFormattedMessage(key, color, false, false, false, args);

        player.sendChatToPlayer(msg);
    }

    public static void syncBalance(ServerPlayer player)
    {
        Network.sendToClient(player, new S2CBalanceSyncPacket(MoneyManager.getBalanceTenths(player)));
    }

    public static void syncInventory(ServerPlayer player)
    {
        Network.sendToClient(player, new S2CInventorySyncPacket(player.inventory.mainInventory));
    }

    public static ChatMessageComponent createMessage(String key, EnumChatFormatting color, boolean bold, boolean italic, boolean underline)
    {
        ChatMessageComponent msg = new ChatMessageComponent();
        msg.addKey(key);
        msg.setColor(color);
        msg.setBold(bold);
        msg.setItalic(italic);
        msg.setUnderline(underline);
        return msg;
    }

    public static ChatMessageComponent createFormattedMessage(String key, EnumChatFormatting color, boolean bold, boolean italic, boolean underline, Object... args)
    {
        ChatMessageComponent msg = new ChatMessageComponent();
        msg.addFormatted(key, args);
        msg.setColor(color);
        msg.setBold(bold);
        msg.setItalic(italic);
        msg.setUnderline(underline);
        return msg;
    }

    // ===== Arg / key tables =====

    private static final Map<String, List<String>> ARG_ORDER = Map.ofEntries(
            Map.entry("shop.buy.success",                  List.of("item", "count", "cost")),
            Map.entry("shop.sell.success",                 List.of("item", "count", "gain")),
            Map.entry("shop.force.dispose",                List.of("item", "count")),
            Map.entry("shop.money.show",                   List.of("money")),
            Map.entry("shop.money.set.success",            List.of("money")),
            Map.entry("shop.money.set.success.other",      List.of("player", "money")),
            Map.entry("shop.money.set.success.byop",       List.of("admin", "money")),
            Map.entry("shop.money.set.not_found",          List.of("player")),
            Map.entry("shop.page",                         List.of("page", "pages")),
            Map.entry("gshop.buy.success",                 List.of("cost", "seller", "item", "count")),
            Map.entry("gshop.sale.success",                List.of("buyer", "revenue", "item", "count")),
            Map.entry("gshop.listing.add.success",         List.of("item", "count", "price")),
            Map.entry("gshop.listing.remove.success",      List.of("item", "count", "id")),
            Map.entry("gshop.listing.remove.not_owner",    List.of("id")),
            Map.entry("gshop.listing.remove.not_found",    List.of("id")),
            Map.entry("gshop.listing.announce.line1.sell", List.of("player", "item", "count")),
            Map.entry("gshop.listing.announce.line1.buy",  List.of("player", "item", "count")),
            Map.entry("gshop.listing.announce.line2",      List.of("price", "type")),
            Map.entry("gshop.list.mine.line",              List.of("id", "item", "amount", "price")),
            Map.entry("gshop.buyorder.add.success",        List.of("item", "count")),
            Map.entry("gshop.buyorder.remove.success",     List.of("item", "count", "id")),
            Map.entry("gshop.buyorder.refund",             List.of("amount", "refund")),
            Map.entry("gshop.buyorder.sell.success",       List.of("item", "count", "buyer")),
            Map.entry("gshop.buyorder.buyer_sync",         List.of("delta")),
            Map.entry("gshop.buyorder.sell.fail_zero",     List.of("item")),
            Map.entry("gshop.unlist.item.invalid",         List.of("id", "item", "count")),
            Map.entry("gshop.unlist.usage",                List.of()),
            Map.entry("gshop.sell.usage",                  List.of()),
            Map.entry("gshop.buy.usage",                   List.of()),
            Map.entry("gshop.list.mine.header",            List.of()),
            Map.entry("gshop.buyorder.fulfilled",          List.of()),
            Map.entry("default",                           List.of())
    );

    private static final Set<String> CLIENT_LOCALIZE_KEYS = Set.of(
            "shop.buy.success",
            "shop.sell.success",
            "shop.force.dispose",
            "gshop.buy.success",
            "gshop.sale.success",
            "gshop.listing.add.success",
            "gshop.listing.remove.success",
            "gshop.listing.announce.line1.sell",
            "gshop.listing.announce.line1.buy",
            "gshop.buyorder.add.success",
            "gshop.buyorder.remove.success",
            "gshop.buyorder.sell.success",
            "gshop.buyorder.sell.fail_zero",
            "gshop.unlist.item.invalid"
    );

}