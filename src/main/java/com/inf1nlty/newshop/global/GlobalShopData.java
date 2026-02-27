package com.inf1nlty.newshop.global;

import com.inf1nlty.newshop.ShopConfig;
import com.inf1nlty.newshop.util.ListingIdGenerator;
import com.inf1nlty.newshop.util.PlayerIdentityUtil;
import net.minecraft.CompressedStreamTools;
import net.minecraft.EntityPlayer;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;

import java.io.*;
import java.util.*;
import java.util.Base64;

/**
 * Global shop persistence. One line per listing:
 *   id;ownerUUID;ownerName;itemId;meta;amount;priceTenths;base64NBT;type  (S=sell, B=buy)
 */
public final class GlobalShopData {

    private static final List<GlobalListing>             LIST     = new ArrayList<>();
    private static final Map<Integer, GlobalListing>     INDEX    = new HashMap<>();
    private static final Map<UUID, List<GlobalListing>>  BY_OWNER = new HashMap<>();

    private static File    SHOP_DIR    = null;
    private static File    FILE        = null;
    private static boolean initialized = false;

    private GlobalShopData() {}

    public static void init(File shopDir) {
        if (initialized) return;
        SHOP_DIR = shopDir;
        FILE = new File(SHOP_DIR, "global_shop.cfg");
        if (!SHOP_DIR.exists()) {
            if (!SHOP_DIR.mkdirs())
                throw new RuntimeException("Failed to create shop directory: " + SHOP_DIR.getAbsolutePath());
        }
        initialized = true;
    }

    private static void ensureInitialized() {
        if (!initialized || SHOP_DIR == null || FILE == null)
            throw new IllegalStateException("GlobalShopData not initialized.");
    }

    public static synchronized void load() {
        ensureInitialized();
        LIST.clear();
        INDEX.clear();
        BY_OWNER.clear();
        int maxId = 0;
        if (!FILE.exists()) {
            save();
            ListingIdGenerator.seed(0);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(";", -1);
                if (parts.length < 9) continue;
                GlobalListing listing = new GlobalListing();
                try {
                    listing.listingId  = Integer.parseInt(parts[0]);
                    listing.ownerUUID  = UUID.fromString(parts[1]);
                    listing.ownerName  = parts[2];
                    listing.itemId     = Integer.parseInt(parts[3]);
                    listing.meta       = Integer.parseInt(parts[4]);
                    listing.amount     = Integer.parseInt(parts[5]);
                    listing.priceTenths = Integer.parseInt(parts[6]);
                    String base64 = parts[7];
                    if (!base64.isEmpty()) {
                        byte[] data = Base64.getDecoder().decode(base64);
                        listing.nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(data));
                    }
                    listing.isBuyOrder = "B".equalsIgnoreCase(parts[8]);
                } catch (Exception ignored) {
                    continue;
                }
                Item item = (listing.itemId >= 0 && listing.itemId < Item.itemsList.length) ? Item.itemsList[listing.itemId] : null;
                if (item == null || (!listing.isBuyOrder && listing.amount <= 0)) continue;
                LIST.add(listing);
                INDEX.put(listing.listingId, listing);
                BY_OWNER.computeIfAbsent(listing.ownerUUID, key -> new ArrayList<>()).add(listing);
                maxId = Math.max(maxId, listing.listingId);
            }
            ListingIdGenerator.seed(maxId);
        } catch (Exception ignored) {
            ListingIdGenerator.seed(0);
        }
    }

    public static synchronized void save() {
        ensureInitialized();
        try {
            if (!SHOP_DIR.exists()) SHOP_DIR.mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE))) {
                writer.write("# Global Shop Listings\n");
                for (GlobalListing listing : LIST) {
                    String base64 = "";
                    if (listing.nbt != null) {
                        try (ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                            CompressedStreamTools.writeCompressed(listing.nbt, buf);
                            base64 = Base64.getEncoder().encodeToString(buf.toByteArray());
                        } catch (Exception ignored) {}
                    }
                    writer.write(listing.listingId + ";" + listing.ownerUUID + ";" + escapeSemicolon(listing.ownerName) + ";"
                            + listing.itemId + ";" + listing.meta + ";" + listing.amount + ";" + listing.priceTenths + ";" + base64 + ";"
                            + (listing.isBuyOrder ? "B" : "S") + "\n");
                }
            }
        } catch (IOException ignored) {}
    }

    private static String escapeSemicolon(String str) {
        return str.replace(";", "_");
    }

    public static synchronized List<GlobalListing> all() {
        List<GlobalListing> result = new ArrayList<>(LIST.size());
        for (GlobalListing listing : LIST) result.add(listing.copyShallow());
        return result;
    }

    public static synchronized List<GlobalListing> byOwner(UUID owner) {
        List<GlobalListing> src = BY_OWNER.get(owner);
        if (src == null) return List.of();
        List<GlobalListing> result = new ArrayList<>(src.size());
        for (GlobalListing listing : src) result.add(listing.copyShallow());
        return result;
    }

    public static synchronized GlobalListing get(int id) {
        return INDEX.get(id);
    }

    public static synchronized GlobalListing addSellOrder(EntityPlayer player, int itemId, int meta, int amount, int priceTenths) {
        return addSellOrder(player, itemId, meta, amount, priceTenths, player.inventory.getCurrentItemStack());
    }

    /** Creates a sell order from the given source stack. Returns null if the player's sell listing cap is reached. */
    public static synchronized GlobalListing addSellOrder(EntityPlayer player, int itemId, int meta, int amount, int priceTenths, ItemStack source) {
        ensureInitialized();
        UUID ownerUUID = PlayerIdentityUtil.getOfflineUUID(player.username);
        List<GlobalListing> owned = BY_OWNER.getOrDefault(ownerUUID, List.of());
        int max = ShopConfig.MAX_GLOBAL_LISTINGS.getIntegerValue();
        long sellCount = owned.stream().filter(listing -> !listing.isBuyOrder).count();
        if (sellCount >= max) return null;

        GlobalListing listing   = new GlobalListing();
        listing.listingId   = ListingIdGenerator.nextId();
        listing.ownerName   = player.username;
        listing.ownerUUID   = ownerUUID;
        listing.itemId      = itemId;
        listing.meta        = meta;
        listing.amount      = amount;
        listing.priceTenths = priceTenths;
        if (source != null && source.stackTagCompound != null)
            listing.nbt = (NBTTagCompound) source.stackTagCompound.copy();
        listing.isBuyOrder = false;
        LIST.add(listing);
        INDEX.put(listing.listingId, listing);
        BY_OWNER.computeIfAbsent(ownerUUID, key -> new ArrayList<>()).add(listing);
        save();
        return listing.copyShallow();
    }

    public static synchronized GlobalListing addBuyOrder(EntityPlayer player, int itemId, int meta, int amount, int priceTenths) {
        ensureInitialized();
        UUID ownerUUID = PlayerIdentityUtil.getOfflineUUID(player.username);

        GlobalListing listing   = new GlobalListing();
        listing.listingId   = ListingIdGenerator.nextId();
        listing.ownerName   = player.username;
        listing.ownerUUID   = ownerUUID;
        listing.itemId      = itemId;
        listing.meta        = meta;
        listing.amount      = amount;
        listing.priceTenths = priceTenths;
        listing.isBuyOrder  = true;
        LIST.add(listing);
        INDEX.put(listing.listingId, listing);
        BY_OWNER.computeIfAbsent(ownerUUID, key -> new ArrayList<>()).add(listing);
        save();
        return listing.copyShallow();
    }

    public static synchronized GlobalListing remove(int listingId, UUID actor) {
        ensureInitialized();
        GlobalListing listing = INDEX.get(listingId);
        if (listing == null || !listing.ownerUUID.equals(actor)) return null;
        LIST.remove(listing);
        INDEX.remove(listingId);
        List<GlobalListing> owned = BY_OWNER.get(actor);
        if (owned != null) owned.remove(listing);
        save();
        return listing.copyShallow();
    }

    public static synchronized int buy(int listingId, int count) {
        ensureInitialized();
        GlobalListing listing = INDEX.get(listingId);
        if (listing == null) return -1;
        if (count <= 0) count = 1;
        int actual = Math.min(count, listing.amount);
        listing.amount -= actual;
        if (listing.amount <= 0) {
            LIST.remove(listing);
            INDEX.remove(listingId);
            List<GlobalListing> owned = BY_OWNER.get(listing.ownerUUID);
            if (owned != null) owned.remove(listing);
        }
        save();
        return actual;
    }

    public static void clearStatic() {
        LIST.clear();
        INDEX.clear();
        BY_OWNER.clear();
        SHOP_DIR    = null;
        FILE        = null;
        initialized = false;
    }

}