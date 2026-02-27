package com.inf1nlty.newshop.registry;

import com.inf1nlty.newshop.api.ShopPlugin;
import com.inf1nlty.newshop.api.ShopRegistry;
import net.minecraft.Block;
import net.minecraft.Item;

/**
 * Built-in default prices for the system shop.
 *
 * <p>Using live {@link Item} / {@link Block} references ensures that the correct
 * runtime item-IDs assigned by MITE's IDUtil are always used, regardless of
 * what numeric IDs end up being assigned at runtime.
 *
 * <p>Only {@code sellPrice} (sell-to-shop) is set here for most entries.
 * Set {@code buyPrice} > 0 if players should also be able to purchase the item
 * from the shop.
 */
public class InternalPlugin implements ShopPlugin {

    @Override
    public void register(ShopRegistry registry) {
        registerPrices(registry);
    }

    private void registerPrices(ShopRegistry registry) {
        // --- Animal drops ---
        registry.addItem(Item.manure,           0, 0.0, 1.0);
        registry.addItem(Item.seeds,            0, 0.0, 0.25);
        registry.addItem(Item.sinew,            0, 0.0, 0.25);
        registry.addItem(Item.leather,          0, 0.0, 1.0);
        registry.addItem(Item.silk,             0, 0.0, 1.0);
        registry.addItem(Item.feather,          0, 0.0, 1.0);

        // --- Stone / mining basics ---
        registry.addItem(Item.flint,            0, 0.0, 2.5);
        registry.addItem(Item.chipFlint,        0, 0.0, 0.5);
        registry.addItem(Item.shardObsidian,    0, 0.0, 1.0);
        registry.addItem(Item.shardEmerald,     0, 0.0, 2.5);
        registry.addItem(Item.shardDiamond,     0, 0.0, 2.5);
        registry.addItem(Item.redstone,         0, 0.0, 2.5);
        registry.addItem(Item.coal,             0, 0.0, 5.0);

        // --- Mob drops ---
        registry.addItem(Item.bone,             0, 0.0, 1.0);
        registry.addItem(Item.gunpowder,        0, 0.0, 1.0);
        registry.addItem(Item.rottenFlesh,      0, 0.0, 1.0);
        registry.addItem(Item.spiderEye,        0, 0.0, 1.0);

        // --- Doors ---
        registry.addItem(Item.doorWood,         0, 0.0, 10.0);
        registry.addItem(Item.doorCopper,       0, 0.0, 60.0);
        registry.addItem(Item.doorSilver,       0, 0.0, 60.0);
        registry.addItem(Item.doorGold,         0, 0.0, 60.0);
        registry.addItem(Item.doorIron,         0, 0.0, 120.0);
        registry.addItem(Item.doorAncientMetal, 0, 0.0, 180.0);
        registry.addItem(Item.doorMithril,      0, 0.0, 240.0);
        registry.addItem(Item.doorAdamantium,   0, 0.0, 480.0);

        // --- Ingots ---
        registry.addItem(Item.ingotCopper,       0, 0.0, 10.0);
        registry.addItem(Item.ingotSilver,       0, 0.0, 10.0);
        registry.addItem(Item.ingotGold,         0, 0.0, 10.0);
        registry.addItem(Item.ingotIron,         0, 0.0, 20.0);
        registry.addItem(Item.ingotAncientMetal, 0, 0.0, 30.0);
        registry.addItem(Item.ingotMithril,      0, 0.0, 40.0);
        registry.addItem(Item.ingotAdamantium,   0, 0.0, 80.0);

        // --- Nuggets ---
        registry.addItem(Item.copperNugget,       0, 0.0, 1.0);
        registry.addItem(Item.silverNugget,       0, 0.0, 1.0);
        registry.addItem(Item.goldNugget,         0, 0.0, 1.0);
        registry.addItem(Item.ironNugget,         0, 0.0, 2.0);
        registry.addItem(Item.ancientMetalNugget, 0, 0.0, 3.0);
        registry.addItem(Item.mithrilNugget,      0, 0.0, 4.0);
        registry.addItem(Item.adamantiumNugget,   0, 0.0, 8.0);

        // --- Misc items ---
        registry.addItem(Item.minecartEmpty, 0, 0.0, 100.0);
        registry.addItem(Item.saddle,        0, 0.0, 5.0);

        // --- Horse armour ---
        registry.addItem(Item.horseArmorMithril,    0, 0.0, 250.0);
        registry.addItem(Item.horseArmorAdamantium, 0, 0.0, 500.0);

        // --- Blocks (via live block reference) ---
        registry.addBlock(Block.plantYellow,    0, 0.0, 0.25);
        registry.addBlock(Block.plantRed,       0, 0.0, 0.25);
        registry.addBlock(Block.leaves,         0, 0.0, 0.5);
        registry.addBlock(Block.planks,         0, 0.0, 1.25);
        registry.addBlock(Block.pumpkin,        0, 0.0, 2.0);
        registry.addBlock(Block.dirt,           0, 0.0, 0.5);
        registry.addBlock(Block.sand,           0, 0.0, 0.5);
        registry.addBlock(Block.cobblestone,    0, 0.0, 1.0);
        registry.addBlock(Block.stone,          0, 0.0, 1.0);
        registry.addBlock(Block.cobblestoneWall,0, 0.0, 1.5);
        registry.addBlock(Block.wood,           0, 0.0, 1.0);
    }
}
