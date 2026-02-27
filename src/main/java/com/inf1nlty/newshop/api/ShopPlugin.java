package com.inf1nlty.newshop.api;

import net.minecraft.Item;

/**
 * Implement this interface and register via {@code ShopPluginLoader.register(ShopPlugin)}
 * during your mod's init phase to add items to the system shop.
 *
 * <p>Because MITE dynamically allocates item IDs through IDUtil, hard-coding numeric IDs
 * in {@code shop.cfg} is fragile. Using this API lets you reference Item
 * instances directly, so the correct runtime ID is always used.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyPlugin implements ShopPlugin {
 *     @Override
 *     public void register(ShopRegistry registry) {
 *         registry.addItem(MyMod.myItem, 0, 10.0, 5.0);
 *     }
 * }
 * // In your mod init:
 * ShopPluginLoader.register(new MyPlugin());
 * }</pre>
 */
public interface ShopPlugin {
    /**
     * Called once when the shop config is (re-)loaded, before the final item list
     * is published to players.  Use {@code registry} to add or override entries.
     *
     * @param registry the registry to add items to
     */
    void register(ShopRegistry registry);
}