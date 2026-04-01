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
 * Loads system shop items from config/NewShop.cfg.
 *
 * <p>Config line format:
 * <pre>
 *   modid:name[:meta][|base64nbt] = buyPrice,sellPrice
 * </pre>
 * Vanilla items use {@code minecraft} as the modid and keep {@code item./tile.} kind
 * (e.g. {@code minecraft:item.swordIron=10.00,5.00}, {@code minecraft:tile.stone=10.00,5.00}).
 * Mod items use their own modid (e.g. {@code mymod:item.myItem=5.00,2.50}).
 * The legacy numeric-ID format ({@code 256=10.00,5.00}) is still accepted for backward compatibility.
 * Prices support up to two decimal places.  A price of {@code 0,0} means the entry is disabled.
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
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE_NAME = "NewShop.cfg";

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
        loadInternal(resolveConfigForLoad());
    }

    private static void ensure() {
        if (System.currentTimeMillis() - lastLoadTime < RELOAD_MS && !itemMap.isEmpty()) return;
        loadInternal(resolveConfigForLoad());
    }

    public static synchronized void regenerateDefault() {
        File file = resolveConfigForWrite();
        if (file.exists()) file.delete();
        generateDefault(file);
        loadInternal(file);
        lastLoadTime = 0L;
    }

    private static File primaryConfigFile() {
        return new File(CONFIG_DIR, CONFIG_FILE_NAME);
    }

    private static File resolveConfigForLoad() {
        return primaryConfigFile();
    }

    private static File resolveConfigForWrite() {
        return primaryConfigFile();
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
                // Skip zero-price entries — they mark items as "not available" in the shop.
                // This also ensures plugin-registered items are never shadowed by a 0,0 cfg line.
                if (buy == 0 && sell == 0) continue;

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
        if (!raw.matches("-?\\d+(\\.\\d{1,2})?")) return null;
        if (raw.contains(".")) {
            String[] parts = raw.split("\\.");
            int whole = Integer.parseInt(parts[0]);
            // Pad or truncate fraction to exactly 2 digits
            String fracStr = (parts[1] + "0").substring(0, 2);
            int frac = Integer.parseInt(fracStr);
            return whole < 0 ? whole * 100 - frac : whole * 100 + frac;
        }
        return Integer.parseInt(raw) * 100;
    }

    private static class IdMeta { int id = -1; int meta = 0; }

    private static class BareIdentifier {
        String modid = "minecraft";
        String name = "";
    }

    private static class NameSpec {
        String kind;
        String name = "";
    }

    private static class UnlocalizedInfo {
        String kind;
        String modid = "minecraft";
        String name = "";
        String body = "";
    }

    private static IdMeta parseIdentifier(String raw) {
        IdMeta result = new IdMeta();
        if (raw.matches("^\\d+(?::\\d+)?$")) {
            String[] segments = raw.split(":");
            result.id = Integer.parseInt(segments[0]);
            if (segments.length == 2) result.meta = parseIntSafe(segments[1]);
            return result;
        }

        String ident = raw.trim();
        int meta = 0;

        // Optional trailing ":meta", while allowing dots in the name body.
        int lastColon = ident.lastIndexOf(':');
        if (lastColon > 0) {
            String maybeMeta = ident.substring(lastColon + 1).trim();
            if (maybeMeta.matches("\\d+")) {
                meta = parseIntSafe(maybeMeta);
                ident = ident.substring(0, lastColon).trim();
            }
        }

        String modid;
        String name;
        int firstColon = ident.indexOf(':');
        if (firstColon > 0) {
            modid = ident.substring(0, firstColon).trim();
            name  = ident.substring(firstColon + 1).trim();
        } else {
            BareIdentifier bare = parseBareIdentifier(ident);
            modid = bare.modid;
            name = bare.name;
        }

        int found = findItemId(modid, name);
        if (found >= 0) {
            result.id = found;
            result.meta = meta;
        }

        return result;
    }

    private static int parseIntSafe(String str) {
        if (str == null || str.isEmpty()) return 0;
        try { return Integer.parseInt(str); } catch (NumberFormatException ignored) { return 0; }
    }

    private static BareIdentifier parseBareIdentifier(String raw) {
        BareIdentifier parsed = new BareIdentifier();
        String normalized = stripNameSuffix(raw);

        if (normalized.startsWith("item.") || normalized.startsWith("tile.")) {
            String kind = normalized.startsWith("item.") ? "item" : "tile";
            // Prefer vanilla interpretation first (important for names like "tile.wood.oak").
            parsed.modid = "minecraft";
            parsed.name = normalized;
            if (findItemId(parsed.modid, parsed.name) >= 0) {
                return parsed;
            }

            // Fallback: modded bare key style "item.modid.name" / "tile.modid.name".
            String body = normalized.substring(5);
            int dot = body.indexOf('.');
            if (dot > 0) {
                String guessedModid = body.substring(0, dot);
                String guessedName = kind + "." + body.substring(dot + 1);
                if (findItemId(guessedModid, guessedName) >= 0) {
                    parsed.modid = guessedModid;
                    parsed.name = guessedName;
                }
            }
            return parsed;
        }

        parsed.modid = "minecraft";
        parsed.name = normalized;
        return parsed;
    }

    private static String stripNameSuffix(String key) {
        if (key == null) return "";
        String trimmed = key.trim();
        return trimmed.endsWith(".name") ? trimmed.substring(0, trimmed.length() - 5) : trimmed;
    }

    private static NameSpec parseNameSpec(String rawName) {
        NameSpec spec = new NameSpec();
        String normalized = stripNameSuffix(rawName);
        if (normalized.startsWith("item.")) {
            spec.kind = "item";
            normalized = normalized.substring(5);
        } else if (normalized.startsWith("tile.")) {
            spec.kind = "tile";
            normalized = normalized.substring(5);
        }
        spec.name = normalized;
        return spec;
    }

    private static UnlocalizedInfo parseUnlocalized(String unlocalized) {
        if (unlocalized == null || unlocalized.isEmpty()) return null;

        String normalized = stripNameSuffix(unlocalized);
        UnlocalizedInfo info = new UnlocalizedInfo();
        if (normalized.startsWith("item.")) {
            info.kind = "item";
            info.body = normalized.substring(5);
        } else if (normalized.startsWith("tile.")) {
            info.kind = "tile";
            info.body = normalized.substring(5);
        } else {
            info.body = normalized;
        }

        int dot = info.body.indexOf('.');
        if (dot > 0) {
            info.modid = info.body.substring(0, dot);
            info.name = info.body.substring(dot + 1);
        } else {
            info.modid = "minecraft";
            info.name = info.body;
        }
        return info;
    }

    private static boolean matchesUnlocalized(String modid, NameSpec requested, String unlocalized) {
        UnlocalizedInfo info = parseUnlocalized(unlocalized);
        if (info == null) return false;
        if (!info.modid.equals(modid)) return false;
        if (requested.kind != null && !requested.kind.equals(info.kind)) return false;
        if (requested.name == null || requested.name.isEmpty()) return false;
        return requested.name.equals(info.name) || requested.name.equals(info.body);
    }

    private static int findItemId(String modid, String name) {
        NameSpec requested = parseNameSpec(name);

        for (Item item : Item.itemsList) {
            if (item == null) continue;
            if (matchesUnlocalized(modid, requested, item.getUnlocalizedName())) return item.itemID;
        }
        for (Block block : Block.blocksList) {
            if (block == null) continue;
            if (matchesUnlocalized(modid, requested, block.getUnlocalizedName())) return block.blockID;
        }
        return -1;
    }

    private static void generateDefault(File file) {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            if (!file.exists()) file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("# 系统商店配置文件\n");
                writer.write("# System Shop Config\n");
                writer.write("# 格式: modid:name[:meta][|base64nbt]=buyPrice,sellPrice  (价格支持两位小数)\n");
                writer.write("# Format: modid:name[:meta][|base64nbt]=buyPrice,sellPrice  (price supports two decimal places)\n");
                writer.write("# 原版物品 modid 为 minecraft，例如: minecraft:item.swordIron=10.00,5.00\n");
                writer.write("# Vanilla examples: minecraft:item.swordIron=10.00,5.00 and minecraft:tile.stone=10.00,5.00\n");
                writer.write("# 价格 0,0 表示不启用，修改价格即可启用 / price 0,0 = disabled; change price to enable\n");
                writer.write("# 向后兼容: 支持纯数字 ID 格式 (e.g. 256=10.00,5.00)\n");
                writer.write("# Backward compat: legacy numeric-ID format still works (e.g. 256=10.00,5.00)\n\n");
                writeAllItemEntries(writer);
            }
        }
        catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static void writeAllItemEntries(BufferedWriter writer) throws java.io.IOException {
        Set<String> written = new LinkedHashSet<>();

        for (Item item : Item.itemsList) {
            if (item == null) continue;

            List<ItemStack> subs;
            try { subs = item.getSubItems(); } catch (Exception ignored) { subs = null; }
            if (subs == null || subs.isEmpty()) {
                subs = Collections.singletonList(new ItemStack(item, 1, 0));
            }

            boolean hasSubtypes = subs.size() > 1;

            String nsBase = itemToNamespaceKey(item);

            for (ItemStack stack : subs) {
                if (stack == null) continue;
                int meta;
                try { meta = stack.getItemSubtype(); } catch (Exception ignored) { meta = stack.getItemDamage(); }

                String key = meta == 0 ? nsBase : (nsBase + ":" + meta);
                if (!written.add(key)) continue;

                String displayName;
                try { displayName = stack.getDisplayName(); } catch (Exception ignored) { displayName = null; }
                if (displayName == null || displayName.isEmpty()) displayName = item.getUnlocalizedName();

                writer.write("# " + displayName + (hasSubtypes ? "  meta:" + meta : "") + "\n");
                writer.write(key + "=0.00,0.00\n\n");
            }
        }
    }

    /**
     * Derives a stable key from an Item's unlocalized name.
     * Output keeps kind information as {@code modid:item.name} or {@code modid:tile.name}.
     * <ul>
     *   <li>Vanilla items (unlocalized = {@code item.swordIron}) → {@code minecraft:swordIron}</li>
     *   <li>Mod items    (unlocalized = {@code item.modid.itemName}) → {@code modid:itemName}</li>
     * </ul>
     * Falls back to the numeric ID if the unlocalized name is absent.
     */
    static String itemToNamespaceKey(Item item) {
        String unlocalized = stripNameSuffix(item.getUnlocalizedName());
        if (unlocalized == null || unlocalized.isEmpty()) return String.valueOf(item.itemID);

        // Strip leading "item." / "tile." prefix that MITE always adds
        String kind = "item";
        String trimmed = unlocalized;
        if (trimmed.startsWith("item.")) {
            kind = "item";
            trimmed = trimmed.substring(5);
        }
        else if (trimmed.startsWith("tile.")) {
            kind = "tile";
            trimmed = trimmed.substring(5);
        }

        // If there is still a dot, the part before it is the modid
        int dot = trimmed.indexOf('.');
        if (dot > 0) {
            return trimmed.substring(0, dot) + ":" + kind + "." + trimmed.substring(dot + 1);
        }
        // No dot → vanilla item
        return "minecraft:" + kind + "." + trimmed;
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

    /** Sets or updates the buy/sell price for an item (with optional gameplay NBT) in NewShop.cfg and in-memory state. */
    public static synchronized void savePrice(int itemID, int meta, int buyTenths, int sellTenths, NBTTagCompound nbt) {
        File file = resolveConfigForWrite();
        if (!file.exists()) generateDefault(file);

        Item base = Item.itemsList[itemID];

        // Build the namespace key (e.g. "minecraft:item.swordIron") for writing;
        // keep the legacy numeric key around so we can upgrade old lines in-place.
        String nsBase    = (base != null) ? itemToNamespaceKey(base) : String.valueOf(itemID);
        String numBase   = (meta == 0 ? String.valueOf(itemID) : (itemID + ":" + meta));

        String nbtB64 = "";
        if (nbt != null) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CompressedStreamTools.writeCompressed(nbt, bos);
                nbtB64 = "|" + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
            } catch (Exception ignored) {}
        }

        String nsKey  = (meta == 0 ? nsBase  : (nsBase  + ":" + meta)) + nbtB64;
        String numKey = numBase + nbtB64;
        String newLine = nsKey + "=" + formatTenths(buyTenths) + "," + formatTenths(sellTenths);

        try {
            List<String> lines = new ArrayList<>();
            boolean found = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                        String[] parts = trimmed.split("=", 2);
                        if (parts.length >= 2) {
                            String existingKey = parts[0].trim();
                            // Match both the new namespace key and the old numeric key
                            if (existingKey.equals(nsKey) || existingKey.equals(numKey)) {
                                lines.add(newLine);
                                found = true;
                                continue;
                            }
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

    private static String formatTenths(int hundredths) {
        return (hundredths / 100) + "." + String.format("%02d", Math.abs(hundredths % 100));
    }
}
