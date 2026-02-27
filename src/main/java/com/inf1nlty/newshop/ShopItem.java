package com.inf1nlty.newshop;

import net.minecraft.Item;

/**
 * Compatible interface
 * Injected into all {@link Item} instances via {@code ItemShopMixin},
 * Allows third-party mods to safely cast any {@code Item} to {@code ShopItem}.
 * Price data is read from {@link GoodsConfig}.
 */
public interface ShopItem {

    double newShop$getSoldPrice(int subtype);

    double newShop$getBuyPrice(int subtype);

    void newShop$setSoldPrice(int subtype, double soldPrice);

    void newShop$setBuyPrice(int subtype, double buyPrice);

    default ShopItem setBuyPriceForAllSubs(double price) {
        newShop$setBuyPrice(0, price);
        return this;
    }

    default ShopItem setSoldPriceForAllSubs(double price) {
        newShop$setSoldPrice(0, price);
        return this;
    }

    void newShop$clearPrice();

    static void setSoldPrice(Item item, int subtype, double soldPrice) {
        ((ShopItem) item).newShop$setSoldPrice(subtype, soldPrice);
    }

    static void setBuyPrice(Item item, int subtype, double buyPrice) {
        ((ShopItem) item).newShop$setBuyPrice(subtype, buyPrice);
    }
}