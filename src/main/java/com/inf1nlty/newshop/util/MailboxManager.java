package com.inf1nlty.newshop.util;

import net.minecraft.*;

import java.io.*;
import java.util.*;

/** Per-player mailbox inventories for buy order fulfillment, persisted to disk. */
public class MailboxManager {

    private static final Map<UUID, InventoryBasic> MAILBOXES = new HashMap<>();
    private static File SHOP_DIR = null;
    private static File MAILBOX_DIR = null;
    private static boolean initialized = false;

    private MailboxManager() {}

    public static void init(File shopDir) {
        if (initialized) return;
        SHOP_DIR = shopDir;
        MAILBOX_DIR = new File(SHOP_DIR, "mailboxes");
        if (!MAILBOX_DIR.exists()) MAILBOX_DIR.mkdirs();
        initialized = true;
    }

    private static void ensureInitialized() {
        if (!initialized || SHOP_DIR == null || MAILBOX_DIR == null)
            throw new IllegalStateException("MailboxManager not initialized.");
    }

    public static InventoryBasic getMailbox(UUID playerId) {
        ensureInitialized();
        return MAILBOXES.computeIfAbsent(playerId, MailboxManager::loadMailbox);
    }

    public static void deliver(UUID playerId, ItemStack stack) {
        ensureInitialized();
        InventoryBasic mailbox = getMailbox(playerId);
        addToInventory(mailbox, stack);
        saveMailbox(playerId, mailbox);
    }

    /** Adds a stack to an inventory, merging with existing stacks first. */
    public static boolean addToInventory(IInventory inventory, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false;
        int remaining = stack.stackSize;
        int maxStack  = stack.getItem().maxStackSize;

        for (int slotIndex = 0; slotIndex < inventory.getSizeInventory(); slotIndex++) {
            ItemStack existing = inventory.getStackInSlot(slotIndex);
            if (existing != null && existing.itemID == stack.itemID
                    && existing.getItemSubtype() == stack.getItemSubtype()
                    && Objects.equals(existing.stackTagCompound, stack.stackTagCompound)) {
                int space = maxStack - existing.stackSize;
                if (space > 0) {
                    int toAdd = Math.min(space, remaining);
                    existing.stackSize += toAdd;
                    remaining -= toAdd;
                    if (remaining == 0) return true;
                }
            }
        }
        for (int slotIndex = 0; slotIndex < inventory.getSizeInventory(); slotIndex++) {
            if (inventory.getStackInSlot(slotIndex) == null) {
                int toAdd = Math.min(maxStack, remaining);
                ItemStack copy = stack.copy();
                copy.stackSize = toAdd;
                inventory.setInventorySlotContents(slotIndex, copy);
                remaining -= toAdd;
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    public static void flushAllMailboxesToNBT(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            NBTTagCompound tag = new NBTTagCompound();
            player.writeEntityToNBT(tag);
        }
    }

    public static void saveMailbox(UUID playerId, InventoryBasic mailbox) {
        ensureInitialized();
        try {
            if (!MAILBOX_DIR.exists()) MAILBOX_DIR.mkdirs();
            File mailboxFile = new File(MAILBOX_DIR, playerId.toString() + ".dat");
            NBTTagList nbtList = new NBTTagList();
            for (int slotIndex = 0; slotIndex < mailbox.getSizeInventory(); slotIndex++) {
                ItemStack stack = mailbox.getStackInSlot(slotIndex);
                if (stack != null) {
                    NBTTagCompound entry = new NBTTagCompound();
                    stack.writeToNBT(entry);
                    entry.setInteger("Slot", slotIndex);
                    nbtList.appendTag(entry);
                }
            }
            NBTTagCompound root = new NBTTagCompound();
            root.setTag("mailbox", nbtList);
            CompressedStreamTools.writeCompressed(root, new FileOutputStream(mailboxFile));
        } catch (Exception ignored) {}
    }

    private static InventoryBasic loadMailbox(UUID playerId) {
        ensureInitialized();
        File mailboxFile = new File(MAILBOX_DIR, playerId.toString() + ".dat");
        InventoryBasic mailbox = new InventoryBasic("Mailbox", false, 133);
        if (!mailboxFile.exists()) return mailbox;
        try {
            NBTTagCompound root = CompressedStreamTools.readCompressed(new FileInputStream(mailboxFile));
            if (root != null && root.hasKey("mailbox")) {
                NBTTagList nbtList = root.getTagList("mailbox");
                for (int index = 0; index < nbtList.tagCount(); index++) {
                    NBTTagCompound entry = (NBTTagCompound) nbtList.tagAt(index);
                    int       slotIndex = entry.getInteger("Slot");
                    ItemStack stack     = ItemStack.loadItemStackFromNBT(entry);
                    if (slotIndex >= 0 && slotIndex < mailbox.getSizeInventory() && stack != null)
                        mailbox.setInventorySlotContents(slotIndex, stack);
                }
            }
        } catch (Exception ignored) {}
        return mailbox;
    }

    public static void clearStatic() {
        MAILBOXES.clear();
        SHOP_DIR    = null;
        MAILBOX_DIR = null;
        initialized = false;
    }
}