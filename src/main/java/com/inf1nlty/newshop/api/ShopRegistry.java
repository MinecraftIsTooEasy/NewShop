package com.inf1nlty.newshop.api;

import net.minecraft.Block;
import net.minecraft.Item;
import net.minecraft.ItemStack;

import java.util.List;

/**
 * Registry passed to {@link ShopPlugin#register(ShopRegistry)}.
 * All methods accept live {@link Item} / {@link Block} references so the
 * correct runtime item-ID assigned by MITE's IDUtil is always used.
 *
 * <p>In MITE, items have both a {@code damage} (metadata) value AND an internal
 * {@code subtype} (accessible via {@link ItemStack#getItemSubtype()}).
 * {@link Item#getSubItems()} returns an {@link ItemStack} for every distinct
 * variant of the item, with the correct damage value already set.
 * The {@link #addAllSubItems} / {@link #addAllSubBlocks} helpers use this to
 * register every variant automatically.
 */
public interface ShopRegistry {

    // -------------------------------------------------------------------------
    // Item-based registration
    // -------------------------------------------------------------------------

    /**
     * Add or override a single item variant identified by its {@code meta} /
     * damage value (i.e. {@link ItemStack#getItemDamage()}).
     *
     * @param item       the Item instance (runtime ID resolved automatically)
     * @param meta       damage / sub-type damage value (0 for default variant)
     * @param buyPrice   price to buy from the shop (0 = not available for purchase)
     * @param sellPrice  price to sell to the shop (0 = not sellable)
     */
    void addItem(Item item, int meta, double buyPrice, double sellPrice);

    /**
     * Register a specific item stack (preserving its NBT for NBT-subtype items).
     * Uses {@link ItemStack#getItemSubtype()} as the meta key.
     * Default implementation falls back to {@link #addItem(Item, int, double, double)}.
     */
    default void addItemStack(ItemStack source, double buyPrice, double sellPrice) {
        if (source == null || source.getItem() == null) return;
        addItem(source.getItem(), source.getItemSubtype(), buyPrice, sellPrice);
    }

    /**
     * Convenience – registers meta 0 only.
     */
    default void addItem(Item item, double buyPrice, double sellPrice) {
        addItem(item, 0, buyPrice, sellPrice);
    }

    /**
     * Register <em>all</em> sub-types / variants of an item at the same price.
     *
     * <p>Calls {@link Item#getSubItems()} to enumerate every variant, then
     * registers each one using its damage value as the meta key.
     * If the item reports no sub-items the method falls back to meta 0.
     */
    @SuppressWarnings("unchecked")
    default void addAllSubItems(Item item, double buyPrice, double sellPrice) {
        if (item == null) return;
        List<ItemStack> subs = item.getSubItems();
        if (subs == null || subs.isEmpty()) {
            addItem(item, 0, buyPrice, sellPrice);
        } else {
            for (ItemStack stack : subs) {
                if (stack != null) {
                    // Pass the full source stack so NBT-subtype items preserve their variant NBT
                    addItemStack(stack, buyPrice, sellPrice);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Block-based registration (delegates to Item.itemsList[block.blockID])
    // -------------------------------------------------------------------------

    /**
     * Add or override a block variant.
     *
     * @param block      the Block instance
     * @param meta       block meta / sub-type
     * @param buyPrice   price to buy
     * @param sellPrice  price to sell
     */
    default void addBlock(Block block, int meta, double buyPrice, double sellPrice) {
        Item item = Item.itemsList[block.blockID];
        if (item != null) addItem(item, meta, buyPrice, sellPrice);
    }

    /**
     * Convenience – registers block meta 0 only.
     */
    default void addBlock(Block block, double buyPrice, double sellPrice) {
        addBlock(block, 0, buyPrice, sellPrice);
    }

    /**
     * Register <em>all</em> sub-types / variants of a block at the same price.
     * Delegates to {@link #addAllSubItems} using the block's item form.
     */
    default void addAllSubBlocks(Block block, double buyPrice, double sellPrice) {
        Item item = Item.itemsList[block.blockID];
        if (item != null) addAllSubItems(item, buyPrice, sellPrice);
    }

    // -------------------------------------------------------------------------
    // Bulk / fluent helpers
    // -------------------------------------------------------------------------

    /**
     * Register multiple items all sharing the same buy/sell price.
     * Each entry uses meta 0 only – use {@link #addAllSubItems} if you need
     * all variants.
     */
    default void addItems(double buyPrice, double sellPrice, Item... items) {
        for (Item item : items) addItem(item, 0, buyPrice, sellPrice);
    }

    /**
     * Remove an item variant from the shop (price 0,0 → no entry).
     * Useful for overriding cfg-file entries that a plugin wants to suppress.
     */
    default void removeItem(Item item, int meta) {
        addItem(item, meta, 0.0, 0.0);
    }

    default void removeItem(Item item) {
        removeItem(item, 0);
    }
}