package com.inf1nlty.newshop.util;

import net.minecraft.EntityPlayer;

/**
 * Compatibility interface
 * <p>Applied to every {@link EntityPlayer} instance via the EntityPlayerMixin.
 * <p>Third-party mods can cast {@code (ShopPlayer) player} to obtain an instance-based
 * {@link MoneyManager} for the player.
 */
public interface ShopPlayer {

    MoneyManager newShop$getMoneyManager();

    static MoneyManager getMoneyManager(EntityPlayer player) {
        return ((ShopPlayer) player).newShop$getMoneyManager();
    }

    void newShop$displayGUIShop();
}