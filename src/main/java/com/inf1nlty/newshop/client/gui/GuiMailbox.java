package com.inf1nlty.newshop.client.gui;

import com.inf1nlty.newshop.client.state.ShopClientData;
import com.inf1nlty.newshop.inventory.ContainerMailbox;
import net.minecraft.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.List;

public class GuiMailbox extends GuiContainer {

    private static final int GUI_W = 356;
    private static final int GUI_H = 240;

    private static final int MAILBOX_BG_W = 356;
    private static final int MAILBOX_BG_H = 150;

    private static final int PLAYER_BG_X = 90;
    private static final int PLAYER_BG_Y = 150;
    private static final int PLAYER_BG_W = 176;
    private static final int PLAYER_BG_H = 90;

    private static final int MAILBOX_COLS = 19;
    private static final int MAILBOX_ROWS = 7;
    private static final int MAILBOX_START_X = 8;
    private static final int MAILBOX_START_Y = 18;
    private static final int SLOT_SIZE = 18;

    private static final int TITLE_X = 15;
    private static final int TITLE_Y = 5;
    private static final int PAGE_Y = 5;
    private static final int PLAYER_LABEL_X = 97;
    private static final int PLAYER_LABEL_Y = 145;

    private static final int BTN_PREV = 100;
    private static final int BTN_NEXT = 101;
    private static final int HOVER_COLOR = 0x40FFFFFF;

    private static final String KEY_TITLE = "mailbox.title";
    private static final String KEY_INVENTORY = "shop.inventory";
    private static final String KEY_PAGE = "shop.page";

    // Page button texture regions (512x512 atlas, button size 10x15)
    private static final int BTN_W = 10;
    private static final int BTN_H = 15;
    private static final int BTN_U_DISABLED = 501;
    private static final int BTN_U_HOVER    = 489;
    private static final int BTN_U_NORMAL   = 477;
    private static final int BTN_V_NEXT     = 1;
    private static final int BTN_V_PREV     = 20;

    private final IInventory mailboxInv;
    private int totalPages;
    private int currentPage;
    private ItemStack hoveredStack;

    public GuiMailbox(EntityPlayer player, ContainerMailbox container) {
        super(container);
        this.mailboxInv = container.getSlot(0).inventory;
        xSize = GUI_W;
        ySize = GUI_H;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        ShopClientData.inMailbox = true;
        int capacityPerPage = MAILBOX_COLS * MAILBOX_ROWS;
        totalPages = Math.max(1, (mailboxInv.getSizeInventory() + capacityPerPage - 1) / capacityPerPage);
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        buttonList.clear();
        int gl = (width - xSize) / 2;
        int gt = (height - ySize) / 2;
        buttonList.add(new TexturedPageButton(BTN_NEXT, gl + GUI_W - 1 - BTN_W, gt + 1, false));
        buttonList.add(new TexturedPageButton(BTN_PREV, gl + 1, gt + 1, true));
        updateButtons();
    }

    @Override
    public void onGuiClosed() {
        ShopClientData.inMailbox = false;
        super.onGuiClosed();
    }

    private int capacityPerPage() {
        return MAILBOX_COLS * MAILBOX_ROWS;
    }

    private void updateButtons() {
        for (Object o : buttonList) {
            if (o instanceof TexturedPageButton b) {
                if (b.id == BTN_PREV) b.enabled = currentPage > 0;
                if (b.id == BTN_NEXT) b.enabled = currentPage < totalPages - 1;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BTN_PREV && currentPage > 0) {
            currentPage--;
            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
            updateButtons();
        } else if (button.id == BTN_NEXT && currentPage < totalPages - 1) {
            currentPage++;
            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
            updateButtons();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        updateHovered(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partial);
        if (theSlot == null && hoveredStack != null) {
            drawItemStackTooltip(hoveredStack, mouseX, mouseY);
        }
    }

    private void updateHovered(int mx, int my) {
        hoveredStack = null;
        int cap = capacityPerPage();
        int gl = (width - xSize) / 2;
        int gt = (height - ySize) / 2;
        int relX = mx - gl;
        int relY = my - gt;
        int local = getHoverLocalIndex(relX, relY);
        if (local < 0) return;
        int index = currentPage * cap + local;
        if (index < 0 || index >= mailboxInv.getSizeInventory()) return;
        hoveredStack = mailboxInv.getStackInSlot(index);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GL11.glColor4f(1,1,1,1);
        mc.renderEngine.bindTexture(TextureManager.TEX);
        int gl = (width - xSize)/2;
        int gt = (height - ySize)/2;
        blit(gl, gt, 0, 0, MAILBOX_BG_W, MAILBOX_BG_H);
        blit(gl + PLAYER_BG_X, gt + PLAYER_BG_Y, PLAYER_BG_X, PLAYER_BG_Y, PLAYER_BG_W, PLAYER_BG_H);
        renderPageButtons(mouseX, mouseY);
        renderItems(gl, gt, mouseX, mouseY);
    }

    private void renderPageButtons(int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.renderEngine.bindTexture(TextureManager.TEX);
        for (Object o : buttonList) {
            if (o instanceof TexturedPageButton b) {
                b.drawTextured(mouseX, mouseY, this);
            }
        }
    }

    private void renderItems(int gl, int gt, int mx, int my) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);

        int cap = capacityPerPage();
        int start = currentPage * cap;
        int end = Math.min(mailboxInv.getSizeInventory(), start + cap);

        int relMouseX = mx - gl;
        int relMouseY = my - gt;
        int hoverLocal = getHoverLocalIndex(relMouseX, relMouseY);

        for (int idx = start; idx < end; idx++) {
            int local = idx - start;
            int row = local / MAILBOX_COLS;
            int col = local % MAILBOX_COLS;
            int sx = gl + MAILBOX_START_X + col * SLOT_SIZE;
            int sy = gt + MAILBOX_START_Y + row * SLOT_SIZE;
            ItemStack stack = mailboxInv.getStackInSlot(idx);
            if (stack == null) continue;
            // Reset zLevel before each item to prevent enchantment effect leaking
            this.zLevel = 100.0F;
            itemRenderer.zLevel = 100.0F;
            itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.renderEngine, stack, sx, sy);
            itemRenderer.renderItemOverlayIntoGUI(fontRenderer, mc.renderEngine, stack, sx, sy);
            this.zLevel = 0.0F;
            itemRenderer.zLevel = 0.0F;
            if (local == hoverLocal) {
                GL11.glDisable(GL11.GL_LIGHTING);
                drawGradientRect(sx, sy, sx + 16, sy + 16, HOVER_COLOR, HOVER_COLOR);
                GL11.glEnable(GL11.GL_LIGHTING);
            }
        }

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        // Fully restore GL blend state that enchantment effect may have changed
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPopMatrix();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void drawItemStackTooltip(ItemStack stack, int x, int y) {
        boolean extended = this.mc.gameSettings.advancedItemTooltips || isShiftKeyDown();
        List<String> tooltip = stack.getTooltip(this.mc.thePlayer, extended, theSlot);
        for(int i = 0; i < tooltip.size(); ++i) {
            if (i == 0) {
                String color = Integer.toHexString(stack.getRarity().rarityColor);
                tooltip.set(i, "§" + color + tooltip.get(i));
            } else {
                tooltip.set(i, EnumChatFormatting.GRAY + tooltip.get(i));
            }
        }
        this.func_102021_a(tooltip, x, y);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseXRel, int mouseYRel) {
        String title = I18n.getString(KEY_TITLE);
        fontRenderer.drawString(title, TITLE_X, TITLE_Y, 0x404040);

        String pageStr = I18n.getString(KEY_PAGE)
                .replace("{page}", String.valueOf(currentPage + 1))
                .replace("{pages}", String.valueOf(totalPages));
        fontRenderer.drawString(pageStr, (xSize - fontRenderer.getStringWidth(pageStr)) / 2, PAGE_Y, 0x606060);

        fontRenderer.drawString(I18n.getString(KEY_INVENTORY), PLAYER_LABEL_X, PLAYER_LABEL_Y, 0x404040);
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == mc.gameSettings.keyBindInventory.keyCode) {
            mc.thePlayer.closeScreen();
            return;
        }
        super.keyTyped(c, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private int getHoverLocalIndex(int relX, int relY) {
        int x = relX - MAILBOX_START_X;
        int y = relY - MAILBOX_START_Y;
        if (x < 0 || y < 0) return -1;
        int col = x / SLOT_SIZE;
        int row = y / SLOT_SIZE;
        if (col >= MAILBOX_COLS || row >= MAILBOX_ROWS) return -1;
        return row * MAILBOX_COLS + col;
    }

    private void blit(int x, int y, int u, int v, int w, int h) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        float fW = 1F / 512F;
        float fH = 1F / 512F;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x,     y + h, zLevel, u * fW,       (v + h) * fH);
        t.addVertexWithUV(x + w, y + h, zLevel, (u + w) * fW, (v + h) * fH);
        t.addVertexWithUV(x + w, y,     zLevel, (u + w) * fW, v * fH);
        t.addVertexWithUV(x,     y,     zLevel, u * fW,       v * fH);
        t.draw();
    }

    private static class TexturedPageButton extends GuiButton {
        private final boolean isPrev;

        public TexturedPageButton(int id, int x, int y, boolean isPrev) {
            super(id, x, y, BTN_W, BTN_H, "");
            this.isPrev = isPrev;
        }

        public void drawTextured(int mouseX, int mouseY, GuiMailbox gui) {
            if (!drawButton) return;
            boolean hover = mouseX >= xPosition && mouseY >= yPosition
                    && mouseX < xPosition + BTN_W && mouseY < yPosition + BTN_H;
            int u = enabled ? (hover ? BTN_U_HOVER : BTN_U_NORMAL) : BTN_U_DISABLED;
            int v = isPrev ? BTN_V_PREV : BTN_V_NEXT;
            gui.blit(xPosition, yPosition, u, v, BTN_W, BTN_H);
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my) {}

        @Override
        public boolean mousePressed(Minecraft mc, int mx, int my) {
            return enabled && drawButton
                    && mx >= xPosition && my >= yPosition
                    && mx < xPosition + BTN_W && my < yPosition + BTN_H;
        }
    }
}