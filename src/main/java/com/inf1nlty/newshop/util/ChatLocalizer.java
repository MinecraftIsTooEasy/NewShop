package com.inf1nlty.newshop.util;

import net.minecraft.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client-side localizer for shop messages sent as raw key|param=value strings. */
public class ChatLocalizer {

    private static final Map<String, List<String>> ARG_ORDER = Map.ofEntries(
            Map.entry("shop.buy.success",                  List.of("item", "count", "cost")),
            Map.entry("shop.sell.success",                 List.of("item", "count", "gain")),
            Map.entry("shop.force.dispose",                List.of("item", "count")),
            Map.entry("gshop.listing.add.success",         List.of("item", "count", "price")),
            Map.entry("gshop.listing.remove.success",      List.of("item", "count", "id")),
            Map.entry("gshop.listing.announce.line1.sell", List.of("player", "item", "count")),
            Map.entry("gshop.listing.announce.line1.buy",  List.of("player", "item", "count")),
            Map.entry("gshop.buy.success",                 List.of("cost", "seller", "item", "count")),
            Map.entry("gshop.sale.success",                List.of("buyer", "revenue", "item", "count")),
            Map.entry("gshop.buyorder.add.success",        List.of("item", "count")),
            Map.entry("gshop.buyorder.remove.success",     List.of("item", "count", "id")),
            Map.entry("gshop.buyorder.sell.success",       List.of("item", "count", "buyer")),
            Map.entry("gshop.buyorder.sell.fail_zero",     List.of("item")),
            Map.entry("gshop.unlist.item.invalid",         List.of("id", "item", "count"))
    );

    /** Returns true if the raw string was a shop message, handled and displayed with localization. */
    public static boolean tryHandleSystemShopMessage(String raw)
    {
        if (raw == null || (!raw.contains("shop.") && !raw.contains("gshop.")) || !raw.contains("|")) return false;

        String[] parts = raw.split("\\|");
        String   key   = parts[0];

        if (!ARG_ORDER.containsKey(key)) return false;

        int    itemId   = -1;
        int    meta     = 0;
        int    count    = 1;
        String itemName = "unknown";

        Map<String, String> paramMap = new HashMap<>();
        for (String part : parts)
        {
            int eqIndex = part.indexOf('=');
            if (eqIndex > 0)
                paramMap.put(part.substring(0, eqIndex), part.substring(eqIndex + 1));
        }

        if (paramMap.containsKey("itemID")) itemId = parseIntSafe(paramMap.get("itemID"));
        if (paramMap.containsKey("meta"))   meta   = parseIntSafe(paramMap.get("meta"));
        if (paramMap.containsKey("count"))  count  = parseIntSafe(paramMap.get("count"));

        if (itemId >= 0 && itemId < Item.itemsList.length && Item.itemsList[itemId] != null)
            itemName = new ItemStack(Item.itemsList[itemId], 1, meta).getDisplayName();

        List<String> argOrder = ARG_ORDER.get(key);
        Object[] args = new Object[argOrder.size()];
        for (int argIndex = 0; argIndex < argOrder.size(); argIndex++)
        {
            String argName = argOrder.get(argIndex);

            if ("item".equals(argName))        args[argIndex] = itemName;

            else if ("count".equals(argName))  args[argIndex] = count;

            else args[argIndex] = paramMap.getOrDefault(argName, "");
        }

        String localized = I18n.getStringParams(key, args);
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        if (player != null) player.addChatMessage(localized);
        return true;
    }

    private static int parseIntSafe(String str)
    {
        if (str == null || str.isEmpty()) return 0;
        try { return Integer.parseInt(str); } catch (NumberFormatException ignored) { return 0; }
    }
}