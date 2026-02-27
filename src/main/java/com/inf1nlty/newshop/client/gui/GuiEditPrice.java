package com.inf1nlty.newshop.client.gui;

import com.inf1nlty.newshop.ShopListing;
import com.inf1nlty.newshop.network.ShopC2S;
import net.minecraft.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.ByteArrayOutputStream;

/** GUI for editing the buy/sell price of a system shop item. Opened by Alt+Left-Click on an EMI item. */
public class GuiEditPrice extends GuiScreen {

    private static final RenderItem ITEM_RENDERER = new RenderItem();
    private static final int BTN_DONE = 1;

    private final ItemStack editStack;
    private final GuiScreen parentScreen;

    private GuiTextField buyPriceField;
    private GuiTextField sellPriceField;

    public GuiEditPrice(GuiScreen parentScreen, ItemStack editStack)
    {
        this.parentScreen = parentScreen;
        this.editStack    = editStack;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int fieldX = width / 2 - 100;
        int buyY   = height / 2 - 20;
        int sellY  = height / 2 + 20;
        int doneY  = sellY + 30;

        GuiButton doneButton = new GuiButton(BTN_DONE, width / 2 - 100, doneY, I18n.getString("gui.done"));
        doneButton.enabled = true;
        buttonList.add(doneButton);

        buyPriceField = new GuiTextField(fontRenderer, fieldX, buyY, 200, 20);
        buyPriceField.setMaxStringLength(16);
        buyPriceField.setText("0.");

        sellPriceField = new GuiTextField(fontRenderer, fieldX, sellY, 200, 20);
        sellPriceField.setMaxStringLength(16);
        sellPriceField.setText("0.");
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    @Override
    public void updateScreen()
    {
        buyPriceField.updateCursorCounter();
        sellPriceField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (!button.enabled || button.id != BTN_DONE) return;

        NBTTagCompound gameplayNbt = ShopListing.stripShopTags(editStack.stackTagCompound);
        byte[] nbtData = null;
        if (gameplayNbt != null) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                CompressedStreamTools.writeCompressed(gameplayNbt, bos);
                nbtData = bos.toByteArray();
            } catch (Exception ignored) {}
        }
        ShopC2S.sendSetPrice(editStack.itemID, editStack.getItemSubtype(), parseTenths(buyPriceField), parseTenths(sellPriceField), nbtData);
        mc.displayGuiScreen(parentScreen);
    }

    @Override
    protected void keyTyped(char ch, int keyCode)
    {
        buyPriceField.textboxKeyTyped(ch, keyCode);
        sellPriceField.textboxKeyTyped(ch, keyCode);
        updateDoneButton();
        if (keyCode == 28 || keyCode == 156) actionPerformed((GuiButton) buttonList.get(0));
        super.keyTyped(ch, keyCode);
    }

    @Override
    protected void mouseClicked(int x, int y, int button)
    {
        super.mouseClicked(x, y, button);
        buyPriceField.mouseClicked(x, y, button);
        sellPriceField.mouseClicked(x, y, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial)
    {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.getStringParams("shop.editPrice.title", editStack.getDisplayName()), width / 2, 15, 0xFFFFFF);
        drawString(fontRenderer, I18n.getString("shop.editPrice.buyPrice"),  width / 2 - 100, height / 2 - 30, 0x404040);
        drawString(fontRenderer, I18n.getString("shop.editPrice.soldPrice"), width / 2 - 100, height / 2 + 10, 0x404040);
        buyPriceField.drawTextBox();
        sellPriceField.drawTextBox();
        renderItemPreview();
        super.drawScreen(mouseX, mouseY, partial);
    }

    private void renderItemPreview()
    {
        ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        GL11.glPushMatrix();
        GL11.glScalef(2.0F, 2.0F, 1.0F);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();

        int rx = (sr.getScaledWidth()  / 2) / 2 - 8;
        int ry = (sr.getScaledHeight() / 4) / 2 - 8;

        float prevZ     = this.zLevel;
        float prevItemZ = ITEM_RENDERER.zLevel;

        this.zLevel = ITEM_RENDERER.zLevel = 200.0F;

        ITEM_RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), editStack, rx, ry);
        ITEM_RENDERER.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), editStack, rx, ry);

        this.zLevel = prevZ;

        ITEM_RENDERER.zLevel = prevItemZ;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    private void updateDoneButton()
    {
        ((GuiButton) buttonList.get(0)).enabled =
                !buyPriceField.getText().trim().isEmpty() && !sellPriceField.getText().trim().isEmpty();
    }

    private static int parseTenths(GuiTextField field)
    {
        String text = field.getText().trim();
        if (text.isEmpty()) return 0;
        try { return (int) Math.round(Double.parseDouble(text) * 10.0); }
        catch (NumberFormatException ignored) { return 0; }
    }
}