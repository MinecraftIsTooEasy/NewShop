package com.inf1nlty.newshop.network.S2C;

import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * S2C: Full inventory sync for the player's main inventory.
 */
public class S2CInventorySyncPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation("shop", "inventory_sync");

    private final ItemStack[] inventory;

    public S2CInventorySyncPacket(PacketByteBuf buf)
    {
        this.inventory = new ItemStack[36];

        for (int i = 0; i < 36; i++)
        {
            short id = buf.readShort();

            if (id < 0)
            {
                inventory[i] = null;
            }
            else
            {
                int size = buf.readByte();
                int dmg = buf.readShort();

                Item item = id < Item.itemsList.length ? Item.itemsList[id] : null;
                ItemStack stack = item != null ? new ItemStack(item, size, dmg) : null;

                boolean hasNbt = buf.readBoolean();

                if (hasNbt && stack != null)
                {
                    int nbtLen = buf.readShort();

                    byte[] nbt = new byte[nbtLen];
                    buf.readFully(nbt);
                    try
                    {
                        stack.stackTagCompound = CompressedStreamTools.readCompressed(new ByteArrayInputStream(nbt));
                    } catch (Exception ignored) {}
                }
                inventory[i] = stack;
            }
        }
    }

    public S2CInventorySyncPacket(ItemStack[] mainInventory) {
        this.inventory = new ItemStack[36];
        System.arraycopy(mainInventory, 0, this.inventory, 0, Math.min(mainInventory.length, 36));
    }

    @Override
    public void write(PacketByteBuf buf)
    {
        for (int i = 0; i < 36; i++)
        {
            ItemStack st = inventory[i];

            if (st == null)
            {
                buf.writeShort(-1);
            }
            else
            {
                buf.writeShort((short) st.itemID);
                buf.writeByte(st.stackSize);
                buf.writeShort((short) st.getItemSubtype());
                buf.writeBoolean(st.stackTagCompound != null);

                if (st.stackTagCompound != null)
                {
                    byte[] nbt = compressNBT(st.stackTagCompound);
                    buf.writeShort(nbt.length);
                    buf.write(nbt, 0, nbt.length);
                }
            }
        }
    }

    @Override
    public void apply(EntityPlayer player)
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer thePlayer = mc.thePlayer;

        if (thePlayer != null)
        {
            ItemStack[] main = thePlayer.inventory.mainInventory;
            for (int i = 0; i < Math.min(inventory.length, main.length); i++)
            {
                main[i] = inventory[i];
            }
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }

    private static byte[] compressNBT(NBTTagCompound tag)
    {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(tag, bos);
            return bos.toByteArray();
        }
        catch (Exception e)
        {
            return new byte[0];
        }
    }
}