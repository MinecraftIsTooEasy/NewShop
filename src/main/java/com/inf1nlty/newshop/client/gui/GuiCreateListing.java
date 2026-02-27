package com.inf1nlty.newshop.client.gui;

import com.inf1nlty.newshop.network.ShopC2S;
import net.minecraft.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.ByteArrayOutputStream;

/**
 * GUI to create a global marketplace listing from the player's inventory.
 * Opened by Alt+Right-Click on an inventory slot. Allows the player to set
 * a sell price and quantity (<= held amount). Sends a C2SGlobalListPacket.
 */
public class GuiCreateListing extends GuiScreen {

    private static final RenderItem ITEM_RENDERER = new RenderItem();

    private final ItemStack editStack; // original stack (may have full count)
    private final GuiScreen parentScreen;
    private final int slotIndex;
    /** windowId of the open container; -1 if coming from player inventory. */
    private final int windowId;
    private final boolean fromContainer;
    /** True when the player is in creative/OP mode — no inventory check on server. */
    private final boolean creative;

    private GuiTextField priceField;
    private GuiTextField amountField;

    private static final int BTN_DONE = 1;

    /** Player inventory slot constructor (backward compatible). */
    public GuiCreateListing(GuiScreen parent, ItemStack editStack, int slotIndex) {
        this(parent, editStack, slotIndex, -1, false, false);
    }

    /** Full constructor for both player-inventory and external-container slots. */
    public GuiCreateListing(GuiScreen parent, ItemStack editStack, int slotIndex, int windowId, boolean fromContainer) {
        this(parent, editStack, slotIndex, windowId, fromContainer, false);
    }

    /** Full constructor including creative/OP bypass flag. */
    public GuiCreateListing(GuiScreen parent, ItemStack editStack, int slotIndex, int windowId, boolean fromContainer, boolean creative) {
        this.parentScreen  = parent;
        this.editStack     = editStack;
        this.slotIndex     = slotIndex;
        this.windowId      = windowId;
        this.fromContainer = fromContainer;
        this.creative      = creative;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int centerX = width / 2;
        int buyY = height / 2 - 20;
        int sellY = height / 2 + 20;

        int doneButtonY = sellY + 30;
        buttonList.add(new GuiButton(BTN_DONE, centerX - 100, doneButtonY, I18n.getString("gui.done")));

        // Price field
        priceField = new GuiTextField(fontRenderer, centerX - 100, buyY, 200, 20);
        priceField.setMaxStringLength(16);
        priceField.setText("0.");

        // Amount field — creative/OP can list any positive amount freely
        amountField = new GuiTextField(fontRenderer, centerX - 100, sellY, 200, 20);
        amountField.setMaxStringLength(8);
        amountField.setText(creative ? "1" : String.valueOf(editStack.stackSize));

        updateButtons();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        priceField.updateCursorCounter();
        amountField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == BTN_DONE) {
            int priceTenths = parseTenths(priceField);
            int amount;
            try {
                amount = Integer.parseInt(amountField.getText().trim());
            } catch (NumberFormatException e) {
                amount = -1;
            }
            if (priceTenths <= 0) {
                return;
            }
            if (amount <= 0) {
                return;
            }
            // Non-creative players cannot list more than they hold
            if (!creative && amount > editStack.stackSize) {
                return;
            }

            // Use dedicated packet to list from slot so server will deduct items from that slot
            if (fromContainer) {
                ShopC2S.sendGlobalListFromContainerSlot(editStack.itemID, editStack.getItemDamage(), amount, this.slotIndex, priceTenths, this.windowId, creative);
            } else {
                // For creative/template listings (slotIndex == -1), compress the item's full NBT
                byte[] nbtData = null;
                if (creative && editStack.stackTagCompound != null) {
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        CompressedStreamTools.writeCompressed(editStack.stackTagCompound, bos);
                        nbtData = bos.toByteArray();
                    } catch (Exception ignored) {}
                }
                ShopC2S.sendGlobalListFromSlot(editStack.itemID, editStack.getItemDamage(), amount, this.slotIndex, priceTenths, creative, nbtData);
            }
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        priceField.textboxKeyTyped(c, keyCode);
        amountField.textboxKeyTyped(c, keyCode);
        updateButtons();

        if (keyCode == 28 || keyCode == 156) { // Enter
            actionPerformed((GuiButton) buttonList.get(0));
        }
        super.keyTyped(c, keyCode);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        priceField.mouseClicked(x, y, button);
        amountField.mouseClicked(x, y, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        drawDefaultBackground();
        String title = I18n.getStringParams("gshop.listing.add.title", editStack.getDisplayName());
        drawCenteredString(fontRenderer, title, width / 2, 15, 0xFFFFFF);

        drawString(fontRenderer, I18n.getString("gshop.listing.add.price"), width / 2 - 100, height / 2 - 30, 0x404040);
        String amountKey = creative ? "gshop.listing.add.amount.creative" : "gshop.listing.add.amount";
        drawString(fontRenderer, I18n.getString(amountKey), width / 2 - 100, height / 2 + 10, creative ? 0x55AAFF : 0x404040);

        priceField.drawTextBox();
        amountField.drawTextBox();

        renderItemPreview();
        super.drawScreen(mouseX, mouseY, partial);
    }

    private void renderItemPreview() {
        ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        GL11.glPushMatrix();
        GL11.glScalef(2.0F, 2.0F, 1.0F);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        int rx = (sr.getScaledWidth()  / 2) / 2 - 8;
        int ry = (sr.getScaledHeight() / 4) / 2 - 8;

        float prevZ = this.zLevel;
        float prevItemZ = ITEM_RENDERER.zLevel;
        this.zLevel = 200.0F;
        ITEM_RENDERER.zLevel = 200.0F;
        ITEM_RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), editStack, rx, ry);
        ITEM_RENDERER.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), editStack, rx, ry);
        this.zLevel = prevZ;
        ITEM_RENDERER.zLevel = prevItemZ;
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    private void updateButtons() {
        ((GuiButton) buttonList.get(0)).enabled = !priceField.getText().trim().isEmpty() && !amountField.getText().trim().isEmpty();
    }

    private static int parseTenths(GuiTextField field) {
        try {
            double v = Double.parseDouble(field.getText().trim());
            return (int) Math.round(v * 10.0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}