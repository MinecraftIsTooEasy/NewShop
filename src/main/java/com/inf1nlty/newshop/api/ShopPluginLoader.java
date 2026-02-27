package com.inf1nlty.newshop.api;

import com.inf1nlty.newshop.GoodsConfig;
import com.inf1nlty.newshop.ShopListing;
import net.minecraft.Item;
import net.minecraft.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Registry for {@link ShopPlugin} instances. */
public final class ShopPluginLoader {

    private static final List<ShopPlugin> PLUGINS = new ArrayList<>();

    private ShopPluginLoader() {}

    public static synchronized void register(ShopPlugin plugin)
    {
        if (plugin == null) throw new NullPointerException("plugin must not be null");
        PLUGINS.add(plugin);
    }

    public static synchronized void clearAll() {
        PLUGINS.clear();
    }

    /**
     * Invoked by {@link GoodsConfig} after loading newshop.cfg.
     * Plugin entries fill in items not already defined by the cfg file; cfg-file entries always win.
     */
    public static synchronized void applyAll(List<ShopListing> itemList, Map<Integer, ShopListing> itemMap)
    {
        if (PLUGINS.isEmpty()) return;

        RegistryImpl registry = new RegistryImpl(itemList, itemMap);

        for (ShopPlugin plugin : PLUGINS)
            plugin.register(registry);
    }

    private static final class RegistryImpl implements ShopRegistry
    {
        private final List<ShopListing>         itemList;
        private final Map<Integer, ShopListing>  itemMap;

        RegistryImpl(List<ShopListing> itemList, Map<Integer, ShopListing> itemMap)
        {
            this.itemList = itemList;
            this.itemMap  = itemMap;
        }

        @Override
        public void addItem(Item item, int meta, double buyPrice, double sellPrice)
        {
            if (item == null) return;

            int ckey       = GoodsConfig.compositeKey(item.itemID, meta);
            int buyTenths  = toTenths(buyPrice);
            int sellTenths = toTenths(sellPrice);

            if (itemMap.containsKey(ckey)) return;

            if (buyTenths == 0 && sellTenths == 0) return;

            ShopListing shopItem     = new ShopListing();
            shopItem.itemID          = item.itemID;
            shopItem.damage          = meta;
            shopItem.buyPriceTenths  = buyTenths;
            shopItem.sellPriceTenths = sellTenths;
            shopItem.itemStack       = new ItemStack(item, 1, meta);
            shopItem.displayName     = shopItem.itemStack.getDisplayName();
            itemList.add(shopItem);
            itemMap.put(ckey, shopItem);
        }

        private static int toTenths(double price) {
            return (int) Math.round(price * 10.0);
        }
    }
}