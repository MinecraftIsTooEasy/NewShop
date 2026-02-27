package com.inf1nlty.newshop.mixin.client;

import com.inf1nlty.newshop.client.input.ShopKeyBindings;
import net.minecraft.GameSettings;
import net.minecraft.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/** Appends custom key bindings to GameSettings so they persist in options.txt. */
@Mixin(GameSettings.class)
public abstract class GameSettingsMixin {

    @Shadow public KeyBinding[] keyBindings;

    @Inject(method = "initKeybindings", at = @At("RETURN"))
    private void shop$injectCustomKeys(CallbackInfo ci)
    {
        if (!ShopKeyBindings.markRegistered()) return;

        ShopKeyBindings.OPEN_SHOP        = new KeyBinding("key.openShop",       Keyboard.KEY_B);
        ShopKeyBindings.OPEN_GLOBAL_SHOP = new KeyBinding("key.openGlobalShop", Keyboard.KEY_G);
        ShopKeyBindings.OPEN_MAILBOX     = new KeyBinding("key.openMailbox",     Keyboard.KEY_K);

        KeyBinding[] custom   = { ShopKeyBindings.OPEN_SHOP, ShopKeyBindings.OPEN_GLOBAL_SHOP, ShopKeyBindings.OPEN_MAILBOX };
        KeyBinding[] expanded = Arrays.copyOf(keyBindings, keyBindings.length + custom.length);
        System.arraycopy(custom, 0, expanded, keyBindings.length, custom.length);
        keyBindings = expanded;
    }
}