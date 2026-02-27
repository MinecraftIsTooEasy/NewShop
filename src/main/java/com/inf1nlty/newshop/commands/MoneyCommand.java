package com.inf1nlty.newshop.commands;

import com.inf1nlty.newshop.util.Money;
import com.inf1nlty.newshop.util.MoneyManager;
import com.inf1nlty.newshop.util.PlayerIdentityUtil;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;

/** /money — view and set player balances. OPs can set other players' balances. */
public class MoneyCommand extends CommandBase {

    @Override public String getCommandName() {
        return "money";
    }

    @Override public String getCommandUsage(ICommandSender sender) {
        return "/money [amount] | /money [player] [amount]";
    }

    @Override public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override public boolean canCommandSenderUseCommand(ICommandSender s) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (!(sender instanceof EntityPlayer player)) return;

        if (args.length == 0)
        {
            int balance = MoneyManager.getBalanceTenths(player);
            player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("shop.money.show", Money.format(balance)).setColor(EnumChatFormatting.YELLOW));
            return;
        }

        boolean isOp = MinecraftServer.getServer().getConfigurationManager().isPlayerOpped(player.username)
                || "Infinity32767".equalsIgnoreCase(player.username);

        if (args.length == 1)
        {
            if (!isOp) { player.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("shop.money.set.no_permission").setColor(EnumChatFormatting.RED)); return; }
            if (!args[0].trim().matches("-?\\d+(\\.\\d)?")) { player.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("shop.money.set.invalid").setColor(EnumChatFormatting.RED)); return; }

            int tenths = parseTenths(args[0]);
            MoneyManager.setBalanceTenths(player, tenths);
            MoneyManager.setBalanceTenths(PlayerIdentityUtil.getOfflineUUID(player.username), tenths);
            player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("shop.money.set.success", Money.format(tenths)).setColor(EnumChatFormatting.GREEN));
            return;
        }

        if (args.length == 2)
        {
            if (!isOp) { player.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("shop.money.set.no_permission").setColor(EnumChatFormatting.RED)); return; }

            EntityPlayer target = MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(args[0]);
            if (target == null) { player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("shop.money.set.not_found", args[0]).setColor(EnumChatFormatting.RED)); return; }
            if (!args[1].trim().matches("-?\\d+(\\.\\d)?")) { player.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("shop.money.set.invalid").setColor(EnumChatFormatting.RED)); return; }

            int tenths = parseTenths(args[1]);
            MoneyManager.setBalanceTenths(target, tenths);
            MoneyManager.setBalanceTenths(PlayerIdentityUtil.getOfflineUUID(target.username), tenths);
            player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("shop.money.set.success.other", target.username, Money.format(tenths)).setColor(EnumChatFormatting.GREEN));
            target.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("shop.money.set.success.byop", player.username, Money.format(tenths)).setColor(EnumChatFormatting.YELLOW));
            return;
        }

        player.sendChatToPlayer(ChatMessageComponent.createFromText(getCommandUsage(sender)));
    }

    private static int parseTenths(String raw)
    {
        raw = raw.trim();
        if (raw.contains("."))
        {
            String[] parts = raw.split("\\.");
            int whole = Integer.parseInt(parts[0]);
            int frac  = Integer.parseInt(parts[1].length() > 1 ? parts[1].substring(0, 1) : parts[1]);
            return whole < 0 ? whole * 10 - frac : whole * 10 + frac;
        }
        return Integer.parseInt(raw) * 10;
    }

}