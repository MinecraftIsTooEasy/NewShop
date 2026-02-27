package com.inf1nlty.newshop.network.S2C;

import com.inf1nlty.newshop.client.gui.GuiMailbox;
import com.inf1nlty.newshop.inventory.ContainerMailbox;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.*;

import java.io.ByteArrayInputStream;

/**
 * S2C: Open mailbox GUI with inventory contents.
 */
public class S2CMailboxOpenPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "mailbox_open");

    private final int windowId;
    private final ItemStack[] mailboxItems;

    public S2CMailboxOpenPacket(PacketByteBuf buf) {
        this.windowId = buf.readInt();
        this.mailboxItems = new ItemStack[133];
        for (int i = 0; i < 133; i++) {
            short id = buf.readShort();
            if (id < 0) {
                mailboxItems[i] = null;
            } else {
                int size = buf.readByte();
                int dmg = buf.readShort();
                boolean hasNbt = buf.readBoolean();
                ItemStack stack = new ItemStack(Item.itemsList[id], size, dmg);
                if (hasNbt) {
                    int nbtLen = buf.readShort();
                    byte[] nbt = new byte[nbtLen];
                    buf.readFully(nbt);
                    try {
                        stack.stackTagCompound = CompressedStreamTools.readCompressed(new ByteArrayInputStream(nbt));
                    } catch (Exception ignored) {}
                }
                mailboxItems[i] = stack;
            }
        }
    }

    public S2CMailboxOpenPacket(int windowId, IInventory mailbox) {
        this.windowId = windowId;
        this.mailboxItems = new ItemStack[133];
        for (int i = 0; i < 133; i++) {
            this.mailboxItems[i] = mailbox.getStackInSlot(i);
        }
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(windowId);
        for (int i = 0; i < 133; i++) {
            ItemStack stack = mailboxItems[i];
            if (stack == null) {
                buf.writeShort(-1);
            } else {
                buf.writeShort((short) stack.itemID);
                buf.writeByte(stack.stackSize);
                buf.writeShort((short) stack.getItemSubtype());
                buf.writeBoolean(stack.stackTagCompound != null);
                if (stack.stackTagCompound != null) {
                    byte[] nbt = compressNBT(stack.stackTagCompound);
                    buf.writeShort(nbt.length);
                    buf.write(nbt, 0, nbt.length);
                }
            }
        }
    }

    @Override
    public void apply(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        InventoryBasic mailboxInv = new InventoryBasic("Mailbox", false, 133);
        for (int i = 0; i < 133; i++) {
            mailboxInv.setInventorySlotContents(i, mailboxItems[i]);
        }
        ContainerMailbox container = new ContainerMailbox(mc.thePlayer.inventory, mailboxInv);
        container.windowId = windowId;
        mc.thePlayer.openContainer = container;
        mc.displayGuiScreen(new GuiMailbox(mc.thePlayer, container));
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }

    private static byte[] compressNBT(NBTTagCompound tag) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(tag, bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}