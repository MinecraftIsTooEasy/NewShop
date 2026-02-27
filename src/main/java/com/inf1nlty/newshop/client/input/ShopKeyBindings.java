package com.inf1nlty.newshop.client.input;

import net.minecraft.KeyBinding;

/** Custom shop key bindings appended to GameSettings by {@link com.inf1nlty.newshop.mixin.client.GameSettingsMixin}. */
public final class ShopKeyBindings {

    public static KeyBinding OPEN_SHOP;
    public static KeyBinding OPEN_GLOBAL_SHOP;
    public static KeyBinding OPEN_MAILBOX;

    private static boolean registered;

    private ShopKeyBindings() {}

    /** Returns true only the first time; used to guard the GameSettings array expansion. */
    public static boolean markRegistered()
    {
        if (registered) return false;
        registered = true;
        return true;
    }
}