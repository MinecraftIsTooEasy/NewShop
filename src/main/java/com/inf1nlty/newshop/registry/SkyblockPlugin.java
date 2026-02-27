package com.inf1nlty.newshop.registry;

import com.inf1nlty.newshop.api.ShopPlugin;
import com.inf1nlty.newshop.api.ShopRegistry;
import net.minecraft.Item;
import net.minecraft.ItemMonsterPlacer;

/**
 * Adds spawn-egg entries to the system shop.
 *
 * <p>This plugin is only registered when Sky-Block mode is enabled
 * (see {@link ShopPropertyRegistry}).
 * In normal gameplay, spawn eggs are not purchasable via the shop.
 *
 * <p>Spawn egg variants are distinguished by their subtype / damage value,
 * which encodes the entity type ID (e.g. 90 = Pig, 91 = Sheep, …).
 * We locate the {@link ItemMonsterPlacer} instance by class to avoid relying
 * on a fixed item-ID that may be reassigned by MITE's IDUtil.
 */
public class SkyblockPlugin implements ShopPlugin {

    @Override
    public void register(ShopRegistry registry) {
        Item spawnEgg = findSpawnEggItem();
        if (spawnEgg == null) return;

        // Common farm animals / utility – purchasable only (no sell-back)
        registry.addItem(spawnEgg,  90, 240.0, 0.0); // Pig
        registry.addItem(spawnEgg,  91, 480.0, 0.0); // Sheep
        registry.addItem(spawnEgg,  92, 240.0, 0.0); // Cow
        registry.addItem(spawnEgg, 100, 240.0, 0.0); // Horse
        registry.addItem(spawnEgg,  95,  80.0, 0.0); // Wolf
    }

    /** Find the {@link ItemMonsterPlacer} item by scanning the item registry. */
    private static Item findSpawnEggItem() {
        for (Item item : Item.itemsList) {
            if (item instanceof ItemMonsterPlacer) {
                return item;
            }
        }
        return null;
    }
}