package com.inf1nlty.newshop;

import com.inf1nlty.newshop.api.ShopPluginLoader;
import net.minecraft.Block;
import net.minecraft.Item;
import net.minecraft.ItemStack;

import java.io.*;
import java.util.*;

/**
 * Loads system shop items from config/newshop.cfg.
 */
public class GoodsConfig {

    private static Map<Integer, ShopListing> itemMap  = new HashMap<>();
    private static List<ShopListing>         itemList = new ArrayList<>();
    private static long lastLoadTime = 0L;
    private static final long RELOAD_MS = 3000;

    private GoodsConfig() {}

    public static synchronized ShopListing get(int id, int dmg) {
        ensure();
        return itemMap.get(compositeKey(id, dmg));
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

        Map<Integer, ShopListing> deduped = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("force_sell_unlisted") || line.startsWith("skyblock_mode") || line.startsWith("announceGlobalListing")) continue;

                String[] parts = line.split("=");
                if (parts.length != 2) continue;

                IdMeta parsed = parseIdentifier(parts[0].trim());
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

                ShopListing shopItem        = new ShopListing();
                shopItem.itemID          = parsed.id;
                shopItem.damage          = parsed.meta;
                shopItem.buyPriceTenths  = buy;
                shopItem.sellPriceTenths = sell;
                shopItem.itemStack       = new ItemStack(base, 1, parsed.meta);
                shopItem.displayName     = shopItem.itemStack.getDisplayName();
                deduped.put(compositeKey(parsed.id, parsed.meta), shopItem);
            }
        }
        catch (Exception ignored) {}

        List<ShopListing> list = new ArrayList<>(deduped.values());
        Map<Integer, ShopListing> map = new LinkedHashMap<>(deduped);

        ShopPluginLoader.applyAll(list, map);

        itemList     = list;
        itemMap      = map;
        lastLoadTime = System.currentTimeMillis();
    }

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
                writer.write("# 系统商店配置文件，支持 id[:meta] 或 minecraft:name[:meta] 格式；价格为一位小数 (buyPrice,sellPrice)\n");
                writer.write("# System Shop Config: Supports id[:meta] or minecraft:name[:meta] format; price is one decimal (buyPrice,sellPrice)\n");
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

    /** Sets or updates the buy/sell price for an item in newshop.cfg and in-memory state. */
    public static synchronized void savePrice(int itemID, int meta, int buyTenths, int sellTenths) {
        File file = new File("config/newshop.cfg");
        if (!file.exists()) generateDefault(file);

        String key     = meta == 0 ? String.valueOf(itemID) : (itemID + ":" + meta);
        String newLine = key + "=" + formatTenths(buyTenths) + "," + formatTenths(sellTenths);

        try {
            List<String> lines = new ArrayList<>();
            boolean found = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                        String[] parts = trimmed.split("=");
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

        ShopListing shopItem        = new ShopListing();
        shopItem.itemID          = itemID;
        shopItem.damage          = meta;
        shopItem.buyPriceTenths  = buyTenths;
        shopItem.sellPriceTenths = sellTenths;
        shopItem.itemStack       = new ItemStack(base, 1, meta);
        shopItem.displayName     = shopItem.itemStack.getDisplayName();

        itemMap.put(compositeKey(itemID, meta), shopItem);

        for (ShopListing existing : itemList) {
            if (existing.itemID == itemID && existing.damage == meta) {
                existing.buyPriceTenths  = buyTenths;
                existing.sellPriceTenths = sellTenths;
                lastLoadTime = 0L;
                return;
            }
        }
        itemList.add(shopItem);
        lastLoadTime = 0L;
    }

    private static String formatTenths(int tenths) {
        return (tenths / 10) + "." + Math.abs(tenths % 10);
    }
}