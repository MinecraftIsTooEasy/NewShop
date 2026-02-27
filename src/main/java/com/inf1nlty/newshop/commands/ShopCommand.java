package com.inf1nlty.newshop.commands;

import com.inf1nlty.newshop.GoodsConfig;
import com.inf1nlty.newshop.network.ShopS2C;
import com.inf1nlty.newshop.registry.ShopPropertyRegistry;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/** /shop [reload|regen] — also opens system shop when no argument given. */
public class ShopCommand extends CommandBase {

    @Override public String getCommandName() {
        return "shop";
    }
    @Override public String getCommandUsage(ICommandSender s) {
        return "/shop [reload|regen]";
    }

    @Override public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override public boolean canCommandSenderUseCommand(ICommandSender s) {
        return true;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length == 1)
        {
            String prefix = args[0].toLowerCase();
            List<String> res = new ArrayList<>();
            if ("reload".startsWith(prefix)) res.add("reload");
            return res;
        }
        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0]))
        {
            GoodsConfig.reload();
            ShopPropertyRegistry.run();
            ShopS2C.ensureConfig(true);
            sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("shop.config.reload").setColor(EnumChatFormatting.AQUA));
            return;
        }

        if (args.length == 1 && "regen".equalsIgnoreCase(args[0]))
        {
            boolean allowed = !(sender instanceof ServerPlayer mp)
                    || MinecraftServer.getServer().getConfigurationManager().isPlayerOpped(mp.username);

            if (!allowed)
            {
                sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("shop.config.regen.no_permission").setColor(EnumChatFormatting.RED));
                return;
            }

            GoodsConfig.regenerateDefault();
            GoodsConfig.reload();
            ShopS2C.ensureConfig(true);
            sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("shop.config.regen").setColor(EnumChatFormatting.AQUA));
            return;
        }

        if (sender instanceof ServerPlayer serverPlayer)
        {
            ShopS2C.ensureConfig(false);
            ShopS2C.openSystemShop(serverPlayer);
        }
    }
}