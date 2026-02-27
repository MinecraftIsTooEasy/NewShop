package com.inf1nlty.newshop.mixin.world;

import com.inf1nlty.newshop.util.MailboxManager;
import com.inf1nlty.newshop.util.MoneyManager;
import com.inf1nlty.newshop.util.PlayerIdentityUtil;
import com.inf1nlty.newshop.util.ShopPlayer;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.UUID;

/** Persist currency tenths and mailbox contents in player NBT. Also implements {@link ShopPlayer}. */
@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin implements ShopPlayer {

    @Unique
    @Override
    public MoneyManager newShop$getMoneyManager() {
        return new MoneyManager((EntityPlayer) (Object) this);
    }

    @Unique
    @Override
    public void newShop$displayGUIShop() {
        // Server-side opening is triggered via ShopS2C; no-op on client instances.
    }

    @Inject(method = "clonePlayer(Lnet/minecraft/EntityPlayer;Z)V", at = @At("TAIL"))
    private void shop$clonePlayer(EntityPlayer original, boolean wasDead, CallbackInfo ci)
    {
        EntityPlayer newPlayer = (EntityPlayer) (Object) this;
        UUID uuid = PlayerIdentityUtil.getOfflineUUID(original.username);
        int balance = MoneyManager.getBalanceTenths(uuid);
        MoneyManager.setBalanceTenths(newPlayer, balance);
        MoneyManager.setBalanceTenths(uuid, balance);
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void shop$write(NBTTagCompound tag, CallbackInfo ci)
    {
        EntityPlayer player = (EntityPlayer) (Object) this;
        int balance = MoneyManager.getBalanceTenths(player);
        MoneyManager.setBalanceTenths(PlayerIdentityUtil.getOfflineUUID(player.username), balance);
        tag.setInteger("shop_money", balance);

        NBTTagList mailboxList = new NBTTagList();
        InventoryBasic mailbox = MailboxManager.getMailbox(PlayerIdentityUtil.getOfflineUUID(player.username));
        for (int i = 0; i < mailbox.getSizeInventory(); i++)
        {
            ItemStack stack = mailbox.getStackInSlot(i);
            if (stack != null)
            {
                NBTTagCompound entry = new NBTTagCompound();
                stack.writeToNBT(entry);
                entry.setInteger("Slot", i);
                mailboxList.appendTag(entry);
            }
        }
        tag.setTag("shop_mailbox", mailboxList);
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void shop$read(NBTTagCompound tag, CallbackInfo ci)
    {
        EntityPlayer player = (EntityPlayer) (Object) this;
        UUID uuid = PlayerIdentityUtil.getOfflineUUID(player.username);
        MoneyManager.setBalanceTenths(player, MoneyManager.getBalanceTenths(uuid));

        InventoryBasic mailbox = MailboxManager.getMailbox(uuid);

        boolean mailboxFromDisk = false;
        for (int i = 0; i < mailbox.getSizeInventory(); i++)
        {
            if (mailbox.getStackInSlot(i) != null) { mailboxFromDisk = true; break; }
        }

        if (!mailboxFromDisk && tag.hasKey("shop_mailbox"))
        {
            Arrays.fill(mailbox.inventoryContents, null);
            NBTTagList mailboxList = tag.getTagList("shop_mailbox");
            for (int i = 0; i < mailboxList.tagCount(); i++)
            {
                NBTTagCompound entry = (NBTTagCompound) mailboxList.tagAt(i);
                int       slot  = entry.getInteger("Slot");
                ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
                if (slot >= 0 && slot < mailbox.getSizeInventory() && stack != null)
                    mailbox.setInventorySlotContents(slot, stack);
            }
            MailboxManager.saveMailbox(uuid, mailbox);
        }
    }
}