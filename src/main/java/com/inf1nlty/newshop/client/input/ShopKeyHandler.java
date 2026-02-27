package com.inf1nlty.newshop.client.input;

import com.inf1nlty.newshop.network.ShopC2S;
import net.minecraft.Minecraft;
import org.lwjgl.input.Keyboard;

public class ShopKeyHandler {

    private static boolean f3WasDown      = false;
    private static int     f3ReleaseDelay = 0;

    public static void onClientTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.thePlayer == null || mc.currentScreen != null)
        {
            f3WasDown      = Keyboard.isKeyDown(Keyboard.KEY_F3);
            f3ReleaseDelay = 0;
            return;
        }

        boolean f3Down = Keyboard.isKeyDown(Keyboard.KEY_F3);

        if (f3Down)
        {
            f3WasDown      = true;
            f3ReleaseDelay = 2;
            clearShopKeys();
            return;
        }

        if (f3WasDown)
        {
            if (f3ReleaseDelay > 0)
            {
                f3ReleaseDelay--;
                clearShopKeys();
                if (f3ReleaseDelay == 0) f3WasDown = false;
                return;
            }

            f3WasDown = false;
        }

        if (ShopKeyBindings.OPEN_SHOP.isPressed())             ShopC2S.sendOpenRequest();

        else if (ShopKeyBindings.OPEN_GLOBAL_SHOP.isPressed()) ShopC2S.sendGlobalOpenRequest();

        else if (ShopKeyBindings.OPEN_MAILBOX.isPressed())     ShopC2S.sendMailboxOpen();
    }

    private static void clearShopKeys()
    {
        ShopKeyBindings.OPEN_SHOP.pressed        = false;
        ShopKeyBindings.OPEN_GLOBAL_SHOP.pressed = false;
        ShopKeyBindings.OPEN_MAILBOX.pressed     = false;
        ShopKeyBindings.OPEN_SHOP.pressTime      = 0;
        ShopKeyBindings.OPEN_GLOBAL_SHOP.pressTime = 0;
        ShopKeyBindings.OPEN_MAILBOX.pressTime   = 0;
        while (Keyboard.next())
        {
            int keyCode = Keyboard.getEventKey();
            if (keyCode == Keyboard.KEY_G || keyCode == Keyboard.KEY_B || keyCode == Keyboard.KEY_K)
            {
                // discard event
            }
        }
    }
}