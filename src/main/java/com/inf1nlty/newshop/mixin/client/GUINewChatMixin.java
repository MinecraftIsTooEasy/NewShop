package com.inf1nlty.newshop.mixin.client;

import com.inf1nlty.newshop.util.ChatLocalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.GuiNewChat;

@Mixin(GuiNewChat.class)
public class GUINewChatMixin {

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), cancellable = true)
    private void onPrintChatMessageWithOptionalDeletion(String message, int deletionId, CallbackInfo ci)
    {
        if (ChatLocalizer.tryHandleSystemShopMessage(message)) ci.cancel();
    }
}