package com.inf1nlty.newshop;

import com.inf1nlty.newshop.api.ShopPluginLoader;
import net.minecraft.Block;
import net.minecraft.CompressedStreamTools;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;

import java.io.*;
import java.util.*;
import java.util.Base64;

/**
 * Loads system shop items from config/newshop.cfg.
 *
 * <p>Config line format:
 * <pre>
 *   id[:meta][|base64nbt] = buyPrice,sellPrice
 * </pre>
 * Items without gameplay NBT use the fast {@code int} composite key.
 * Items WITH NBT (enchanted books, gear, …) use a secondary {@code String} map
 * keyed by {@code "id:meta:base64nbt"} so they don't collide with plain items.
 */
public class GoodsConfig {

    private static Map<Integer, ShopListing> itemMap    = new HashMap<>();
    /** Secondary map for NBT-specific entries (e.g. enchanted items). */
    private static Map<String,  ShopListing> nbtItemMap = new LinkedHashMap<>();
    private static List<ShopListing>         itemList   = new ArrayList<>();
    private static long lastLoadTime = 0L;
    private static final long RELOAD_MS = 3000;

    private GoodsConfig() {}

    /** Looks up a plain (non-NBT) listing by itemID + meta. */
    public static synchronized ShopListing get(int id, int dmg) {
        ensure();
        return itemMap.get(compositeKey(id, dmg));
    }

    /**
     * Looks up the best-matching listing for an ItemStack.
     * Tries the NBT-specific map first, then falls back to the plain map.
     */
    public static synchronized ShopListing get(ItemStack stack) {
        if (stack == null) return null;
        ensure();
        NBTTagCompound gameplay = ShopListing.stripShopTags(stack.stackTagCompound);
        if (gameplay != null) {
            String nbtKey = nbtCompositeKey(stack.itemID, stack.getItemSubtype(), gameplay);
            ShopListing hit = nbtItemMap.get(nbtKey);
            if (hit != null) return hit;
        }
        return itemMap.get(compositeKey(stack.itemID, stack.getItemSubtype()));
    }

    public static synchronized List<ShopListing> getItems() {
        ensure();
        return itemList;
    }

    public static synchronized void reload() {
        loadInternal(new File("config/newshop.cfg"));
    }

    private static void ensure() {
        if (System.currentTimeMillis() - lastLoadTime < RELOAD_MS && !itemMap.isEmpty()) return;
        loadInternal(new File("config/newshop.cfg"));
    }

    public static synchronized void regenerateDefault() {
        File file = new File("config/newshop.cfg");
        if (file.exists()) file.delete();
        generateDefault(file);
        loadInternal(file);
        lastLoadTime = 0L;
    }

    private static void loadInternal(File file) {
        if (!file.exists()) generateDefault(file);

        Map<Integer, ShopListing> deduped    = new LinkedHashMap<>();
        Map<String,  ShopListing> dedupedNbt = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("force_sell_unlisted") || line.startsWith("skyblock_mode") || line.startsWith("announceGlobalListing")) continue;

                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                // Split identifier: "id[:meta][|base64nbt]"
                String   identRaw = parts[0].trim();
                String   nbtB64   = null;
                int      pipeIdx  = identRaw.indexOf('|');
                if (pipeIdx >= 0) {
                    nbtB64   = identRaw.substring(pipeIdx + 1).trim();
                    identRaw = identRaw.substring(0, pipeIdx).trim();
                }

                IdMeta parsed = parseIdentifier(identRaw);
                if (parsed.id < 0) continue;

                String[] priceParts = parts[1].split(",");
                if (priceParts.length < 2) continue;

                String  buyRaw  = priceParts[0].split("[ #]")[0].trim();
                String  sellRaw = priceParts[1].split("[ #]")[0].trim();
                Integer buy     = parsePriceTenths(buyRaw);
                Integer sell    = parsePriceTenths(sellRaw);
                if (buy == null || sell == null) continue;

                Item base = Item.itemsList[parsed.id];
                if (base == null) continue;

                NBTTagCompound nbt = null;
                if (nbtB64 != null && !nbtB64.isEmpty()) {
                    try {
                        byte[] data = Base64.getUrlDecoder().decode(nbtB64);
                        nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(data));
                    } catch (Exception ignored) {}
                }

                ShopListing shopItem     = new ShopListing();
                shopItem.itemID          = parsed.id;
                shopItem.damage          = parsed.meta;
                shopItem.buyPriceTenths  = buy;
                shopItem.sellPriceTenths = sell;
                shopItem.nbt             = nbt;
                shopItem.itemStack       = new ItemStack(base, 1, parsed.meta);
                if (nbt != null) shopItem.itemStack.stackTagCompound = (NBTTagCompound) nbt.copy();
                shopItem.displayName     = shopItem.itemStack.getDisplayName();

                if (nbt != null) {
                    dedupedNbt.put(nbtCompositeKey(parsed.id, parsed.meta, nbt), shopItem);
                } else {
                    deduped.put(compositeKey(parsed.id, parsed.meta), shopItem);
                }
            }
        }
        catch (Exception ignored) {}

        List<ShopListing> list = new ArrayList<>(deduped.values());
        list.addAll(dedupedNbt.values());
        Map<Integer, ShopListing> map = new LinkedHashMap<>(deduped);

        ShopPluginLoader.applyAll(list, map);

        itemList     = list;
        itemMap      = map;
        nbtItemMap   = new LinkedHashMap<>(dedupedNbt);
        // Add any NBT entries added by plugins into nbtItemMap too
        for (ShopListing sl : list) {
            if (sl.nbt != null) nbtItemMap.putIfAbsent(nbtCompositeKey(sl.itemID, sl.damage, sl.nbt), sl);
        }
        lastLoadTime = System.currentTimeMillis();
    }

    // ...existing code...

    private static Integer parsePriceTenths(String raw) {
        raw = raw.trim();
        if (!raw.matches("-?\\d+(\\.\\d)?")) return null;
        if (raw.contains(".")) {
            String[] parts = raw.split("\\.");
            int whole = Integer.parseInt(parts[0]);
            int frac  = Integer.parseInt(parts[1]);
            return whole < 0 ? whole * 10 - frac : whole * 10 + frac;
        }
        return Integer.parseInt(raw) * 10;
    }

    private static class IdMeta { int id = -1; int meta = 0; }

    private static IdMeta parseIdentifier(String raw) {
        IdMeta result = new IdMeta();
        if (raw.matches("^\\d+(?::\\d+)?$")) {
            String[] segments = raw.split(":");
            result.id = Integer.parseInt(segments[0]);
            if (segments.length == 2) result.meta = parseIntSafe(segments[1]);
            return result;
        }
        String[] segments = raw.split(":");
        if (segments.length >= 2) {
            String modid = segments[0];
            String name  = segments[1];
            int    meta  = (segments.length == 3 && segments[2].matches("\\d+")) ? parseIntSafe(segments[2]) : 0;
            int    found = findItemId(modid, name);
            if (found >= 0) { result.id = found; result.meta = meta; }
        }
        return result;
    }

    private static int parseIntSafe(String str) {
        if (str == null || str.isEmpty()) return 0;
        try { return Integer.parseInt(str); } catch (NumberFormatException ignored) { return 0; }
    }

    private static int findItemId(String modid, String name) {
        String searchUnlocalized = modid + "." + name;
        for (Item item : Item.itemsList) {
            if (item == null) continue;
            String unlocalized = item.getUnlocalizedName();
            if (unlocalized == null) continue;
            String trimmed = unlocalized.replace("item.", "").replace("tile.", "");
            if (trimmed.equals(searchUnlocalized)) return item.itemID;
            if (modid.equals("minecraft") && trimmed.equals(name)) return item.itemID;
        }
        for (Block block : Block.blocksList) {
            if (block == null) continue;
            String unlocalized = block.getUnlocalizedName();
            if (unlocalized == null) continue;
            String trimmed = unlocalized.replace("item.", "").replace("tile.", "");
            if (trimmed.equals(searchUnlocalized)) return block.blockID;
            if (modid.equals("minecraft") && trimmed.equals(name)) return block.blockID;
        }
        return -1;
    }

    private static void generateDefault(File file) {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            if (!file.exists()) file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("# 系统商店配置文件，支持 id[:meta][|base64nbt] 或 minecraft:name[:meta] 格式；价格为一位小数 (buyPrice,sellPrice)\n");
                writer.write("# System Shop Config: Supports id[:meta][|base64nbt] or minecraft:name[:meta] format; price is one decimal (buyPrice,sellPrice)\n");
                writer.write("# id=buy,sell\n\n");
                writer.write("# 1就是石头的ID，=号后面为出售价,回收价\n\n");
                writer.write("# 1=1,1\n\n");
            }
        }
        catch (Exception ignored) {}
    }

    public static int compositeKey(int id, int dmg) {
        return ((id & 0xFFFF) << 16) | (dmg & 0xFFFF);
    }

    /** Stable string key for NBT-specific entries: "id:meta:base64nbt". */
    public static String nbtCompositeKey(int id, int meta, NBTTagCompound nbt) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(nbt, bos);
            return id + ":" + meta + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
        } catch (Exception e) { return id + ":" + meta + ":?"; }
    }

    /** Sets or updates the buy/sell price for an item (with optional gameplay NBT) in newshop.cfg and in-memory state. */
    public static synchronized void savePrice(int itemID, int meta, int buyTenths, int sellTenths, NBTTagCompound nbt) {
        File file = new File("config/newshop.cfg");
        if (!file.exists()) generateDefault(file);

        String nbtB64  = "";
        if (nbt != null) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CompressedStreamTools.writeCompressed(nbt, bos);
                nbtB64 = "|" + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
            } catch (Exception ignored) {}
        }
        String key     = (meta == 0 ? String.valueOf(itemID) : (itemID + ":" + meta)) + nbtB64;
        String newLine = key + "=" + formatTenths(buyTenths) + "," + formatTenths(sellTenths);

        try {
            List<String> lines = new ArrayList<>();
            boolean found = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                        String[] parts = trimmed.split("=", 2);
                        if (parts.length >= 2 && parts[0].trim().equals(key)) {
                            lines.add(newLine);
                            found = true;
                            continue;
                        }
                    }
                    lines.add(line);
                }
            }
            if (!found) lines.add(newLine);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : lines) { writer.write(line); writer.newLine(); }
            }
        }
        catch (Exception ignored) {}

        Item base = Item.itemsList[itemID];
        if (base == null) return;

        ShopListing shopItem     = new ShopListing();
        shopItem.itemID          = itemID;
        shopItem.damage          = meta;
        shopItem.buyPriceTenths  = buyTenths;
        shopItem.sellPriceTenths = sellTenths;
        shopItem.nbt             = nbt;
        shopItem.itemStack       = new ItemStack(base, 1, meta);
        if (nbt != null) shopItem.itemStack.stackTagCompound = (NBTTagCompound) nbt.copy();
        shopItem.displayName     = shopItem.itemStack.getDisplayName();

        if (nbt != null)
        {
            nbtItemMap.put(nbtCompositeKey(itemID, meta, nbt), shopItem);
            // Also update itemList
            String nbtK = nbtCompositeKey(itemID, meta, nbt);
            for (ShopListing existing : itemList)
            {
                if (existing.nbt != null && nbtCompositeKey(existing.itemID, existing.damage, existing.nbt).equals(nbtK))
                {
                    existing.buyPriceTenths = buyTenths; existing.sellPriceTenths = sellTenths; lastLoadTime = 0L; return;
                }
            }
        }
        else
        {
            itemMap.put(compositeKey(itemID, meta), shopItem);
            for (ShopListing existing : itemList) {
                if (existing.nbt == null && existing.itemID == itemID && existing.damage == meta) {
                    existing.buyPriceTenths = buyTenths; existing.sellPriceTenths = sellTenths; lastLoadTime = 0L; return;
                }
            }
        }
        itemList.add(shopItem);
        lastLoadTime = 0L;
    }

    /** Convenience overload for plain (non-NBT) items. */
    public static synchronized void savePrice(int itemID, int meta, int buyTenths, int sellTenths) {
        savePrice(itemID, meta, buyTenths, sellTenths, null);
    }

    private static String formatTenths(int tenths) {
        return (tenths / 10) + "." + Math.abs(tenths % 10);
    }
}