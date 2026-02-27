package com.inf1nlty.newshop.client.gui;

import com.inf1nlty.newshop.client.state.ShopClientData;
import com.inf1nlty.newshop.client.state.SystemShopClientCatalog;
import com.inf1nlty.newshop.inventory.ContainerShopPlayer;
import com.inf1nlty.newshop.network.ShopC2S;
import com.inf1nlty.newshop.util.Money;
import com.inf1nlty.newshop.util.SearchHelper;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * System shop GUI (server catalog).
 * 19 × 7 grid with sort (ASC/DESC by buy price) and filter (ALL / BUY_ONLY / SELL_ONLY).
 */
public class GuiShop extends GuiContainer {

    private static final int GUI_W = 356;
    private static final int GUI_H = 240;
    private static final int SHOP_BG_W = 356;
    private static final int SHOP_BG_H = 150;
    private static final int PLAYER_BG_X = 90;
    private static final int PLAYER_BG_Y = 150;
    private static final int PLAYER_BG_W = 176;
    private static final int PLAYER_BG_H = 90;

    private static final int SHOP_COLS = 19;
    private static final int SHOP_ROWS = 7;
    private static final int SHOP_START_X = 8;
    private static final int SHOP_START_Y = 18;
    private static final int SLOT_SIZE = 18;

    private static final int PLAYER_LABEL_X = 97;
    private static final int PLAYER_LABEL_Y = 145;
    private static final int HOVER_COLOR = 0x40FFFFFF;

    private static final int TITLE_X = 15;
    private static final int TITLE_Y = 5;
    private static final int PAGE_Y  = 5;

    private static final String KEY_TITLE     = "shop.title";
    private static final String KEY_INVENTORY = "shop.inventory";
    private static final String KEY_MONEY     = "shop.money";
    private static final String KEY_PAGE      = "shop.page";

    // Button IDs
    private static final int BTN_PREV   = 100;
    private static final int BTN_NEXT   = 101;
    private static final int BTN_SORT   = 102;
    private static final int BTN_FILTER = 103;

    // Side-panel buttons placed just to the RIGHT of the GUI, stacked vertically.
    // The background is 22×22; icon (18×22) is centred inside it (2px left padding).
    private static final int SIDE_BTN_OFFSET_X = GUI_W + 3; // relative to guiLeft
    private static final int SIDE_BTN_SORT_Y   = 34;        // relative to guiTop
    private static final int SIDE_BTN_FILTER_Y = 60;        // relative to guiTop

    // Sort / filter state
    private enum SortOrder   { DEFAULT, ASC, DESC }
    private enum FilterMode  { ALL, BUY_ONLY, SELL_ONLY }

    private SortOrder  sortOrder  = SortOrder.DEFAULT;
    private FilterMode filterMode = FilterMode.ALL;
    private String     searchQuery = "";

    private final List<SystemShopClientCatalog.Entry> allEntries;
    /** View after applying current sort + filter + search. */
    private List<SystemShopClientCatalog.Entry> viewEntries = new ArrayList<>();

    private int totalPages;
    private int currentPage;
    private ItemStack hoveredStack;
    private GuiTextField searchBox;
    private int searchBoxX, searchBoxY;

    public GuiShop(EntityPlayer player, ContainerShopPlayer container) {
        super(container);
        this.allEntries = SystemShopClientCatalog.get();
        xSize = GUI_W;
        ySize = GUI_H;
    }

    private int capacityPerPage() { return SHOP_COLS * SHOP_ROWS; }

    /** Rebuild viewEntries from allEntries applying current filter, search, then sort. */
    private void rebuildView() {
        viewEntries = new ArrayList<>();
        for (SystemShopClientCatalog.Entry e : allEntries) {
            // --- filter ---
            switch (filterMode) {
                case BUY_ONLY  -> { if (e.buyTenths  <= 0) continue; }
                case SELL_ONLY -> { if (e.sellTenths <= 0) continue; }
                default        -> {}
            }
            // --- search ---
            if (!searchQuery.isBlank()) {
                ItemStack st = e.toStack();
                String name = (st != null) ? st.getDisplayName() : "";
                if (!SearchHelper.matches(name, searchQuery)) continue;
            }
            viewEntries.add(e);
        }
        Comparator<SystemShopClientCatalog.Entry> cmp =
                Comparator.comparingInt(e -> e.buyTenths);
        if (sortOrder == SortOrder.DEFAULT) {
            // Natural order: sort by item ID (then meta), matching the default MC item registry order
            viewEntries.sort(Comparator.comparingInt((SystemShopClientCatalog.Entry e) -> e.itemID)
                    .thenComparingInt(e -> e.meta));
        } else {
            if (sortOrder == SortOrder.DESC) cmp = cmp.reversed();
            viewEntries.sort(cmp);
        }

        int cap = capacityPerPage();
        totalPages = Math.max(1, (viewEntries.size() + cap - 1) / cap);
        if (currentPage >= totalPages) currentPage = totalPages - 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        ShopClientData.inShop = true;
        ShopClientData.inGlobalShop = false;

        rebuildView();

        buttonList.clear();
        int gl = (width  - xSize) / 2;
        int gt = (height - ySize) / 2;

        // Page buttons — original positions
        buttonList.add(new IconButton(BTN_NEXT, gl + GUI_W - 1 - EnumIcon.PAGE_NEXT.w, gt + 1, EnumIcon.PAGE_NEXT));
        buttonList.add(new IconButton(BTN_PREV, gl + 1,                                gt + 1, EnumIcon.PAGE_PREV));

        // Sort / filter — right side OUTSIDE the GUI
        buttonList.add(new IconButton(BTN_SORT,   gl + SIDE_BTN_OFFSET_X, gt + SIDE_BTN_SORT_Y,   EnumIcon.SORT_ORDER,  22, 22));
        buttonList.add(new IconButton(BTN_FILTER, gl + SIDE_BTN_OFFSET_X, gt + SIDE_BTN_FILTER_Y, EnumIcon.FILTER_TYPE, 22, 22));

        // Search box — top-right area of the grid background, 120 wide, 10 tall
        int sbW = 120, sbH = 10;
        searchBoxX = gl + GUI_W - sbW - 15;
        searchBoxY = gt + 4;
        searchBox = new GuiTextField(fontRenderer, searchBoxX, searchBoxY, sbW, sbH);
        searchBox.setMaxStringLength(64);
        searchBox.setText(searchQuery);
        searchBox.setFocused(false);

        updatePageButtons();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        ShopClientData.inShop = false;
        super.onGuiClosed();
    }

    private void updatePageButtons() {
        for (Object o : buttonList) {
            if (o instanceof IconButton b) {
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
            updatePageButtons();
        } else if (button.id == BTN_NEXT && currentPage < totalPages - 1) {
            currentPage++;
            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
            updatePageButtons();
        } else if (button.id == BTN_SORT) {
            sortOrder = switch (sortOrder) {
                case DEFAULT -> SortOrder.ASC;
                case ASC     -> SortOrder.DESC;
                case DESC    -> SortOrder.DEFAULT;
            };
            currentPage = 0;
            rebuildView();
            updatePageButtons();
            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
        } else if (button.id == BTN_FILTER) {
            filterMode = switch (filterMode) {
                case ALL       -> FilterMode.BUY_ONLY;
                case BUY_ONLY  -> FilterMode.SELL_ONLY;
                case SELL_ONLY -> FilterMode.ALL;
            };
            currentPage = 0;
            rebuildView();
            updatePageButtons();
            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchBox != null) searchBox.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        updateHovered(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partial);
        if (theSlot == null && hoveredStack != null) {
            drawItemStackTooltip(hoveredStack, mouseX, mouseY);
        }
        drawSideButtonTooltips(mouseX, mouseY);
    }

    private void drawSideButtonTooltips(int mx, int my) {
        for (Object o : buttonList) {
            if (!(o instanceof IconButton b)) continue;
            if (!b.isHovered(mx, my)) continue;
            String tip = null;
            if (b.id == BTN_SORT) {
                tip = I18n.getString(switch (sortOrder) {
                    case DEFAULT -> "shop.sort.default";
                    case ASC     -> "shop.sort.asc";
                    case DESC    -> "shop.sort.desc";
                });
            } else if (b.id == BTN_FILTER) {
                tip = I18n.getString(switch (filterMode) {
                    case ALL       -> "shop.filter.all";
                    case BUY_ONLY  -> "shop.filter.buy_only";
                    case SELL_ONLY -> "shop.filter.sell_only";
                });
            }
            if (tip != null) {
                List<String> lines = new ArrayList<>();
                lines.add(tip);
                func_102021_a(lines, mx, my);
            }
        }
    }

    private void updateHovered(int mx, int my) {
        hoveredStack = null;
        int cap = capacityPerPage();
        int gl = (width - xSize) / 2;
        int gt = (height - ySize) / 2;
        int local = getHoverLocalIndex(mx - gl, my - gt);
        if (local < 0) return;
        int index = currentPage * cap + local;
        if (index < 0 || index >= viewEntries.size()) return;
        hoveredStack = viewEntries.get(index).toStack();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.renderEngine.bindTexture(TextureManager.TEX);
        int gl = (width - xSize) / 2;
        int gt = (height - ySize) / 2;
        blit(gl, gt, 0, 0, SHOP_BG_W, SHOP_BG_H);
        blit(gl + PLAYER_BG_X, gt + PLAYER_BG_Y, PLAYER_BG_X, PLAYER_BG_Y, PLAYER_BG_W, PLAYER_BG_H);
        renderAllButtons(mouseX, mouseY);
        // Draw search box here (background layer) so it isn't affected by foreground GL translate
        if (searchBox != null) {
            GL11.glDisable(GL11.GL_LIGHTING);
            searchBox.drawTextBox();
            GL11.glColor4f(1, 1, 1, 1);
        }
        renderItems(gl, gt, mouseX, mouseY);
    }

    private void renderAllButtons(int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        for (Object o : buttonList) {
            if (!(o instanceof IconButton b) || !b.drawButton) continue;
            boolean hov = b.isHovered(mouseX, mouseY);
            if (b.id == BTN_SORT || b.id == BTN_FILTER) {
                GL11.glColor4f(1, 1, 1, 1);
                mc.renderEngine.bindTexture(TextureManager.TEX);
                EnumIcon bg = hov ? EnumIcon.BTN_BG_ACTIVE : EnumIcon.BTN_BG_NORMAL;
                blit(b.xPosition, b.yPosition, bg.uNormal, bg.vNormal, bg.w, bg.h);
                mc.renderEngine.bindTexture(TextureManager.TEX);
                blit(b.xPosition + 2, b.yPosition, b.icon.uNormal, b.icon.vNormal, b.icon.w, b.icon.h);
            } else {
                GL11.glColor4f(1, 1, 1, 1);
                mc.renderEngine.bindTexture(TextureManager.TEX);
                blit(b.xPosition, b.yPosition,
                        b.icon.u(b.enabled, hov), b.icon.v(b.enabled, hov),
                        b.icon.w, b.icon.h);
            }
        }
        // Restore GL state
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);
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
        int end = Math.min(viewEntries.size(), start + cap);
        int hoverLocal = getHoverLocalIndex(mx - gl, my - gt);

        for (int idx = start; idx < end; idx++) {
            int local = idx - start;
            int sx = gl + SHOP_START_X + (local % SHOP_COLS) * SLOT_SIZE;
            int sy = gt + SHOP_START_Y + (local / SHOP_COLS) * SLOT_SIZE;
            ItemStack stack = viewEntries.get(idx).toStack();
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
    @SuppressWarnings("unchecked, rawtypes")
    protected void drawItemStackTooltip(ItemStack stack, int x, int y) {
        EmiScreenManager.lastStackTooltipRendered = stack;
        boolean ext = mc.gameSettings.advancedItemTooltips || isShiftKeyDown();
        List tip = stack.getTooltip(mc.thePlayer, ext, theSlot);
        for (int i = 0; i < tip.size(); i++) {
            if (i == 0) {
                tip.set(i, "§" + Integer.toHexString(stack.getRarity().rarityColor) + tip.get(i));
            } else {
                tip.set(i, EnumChatFormatting.GRAY + (String) tip.get(i));
            }
        }
        if (ShopClientData.inShop) {
            for (SystemShopClientCatalog.Entry e : viewEntries) {
                if (e.itemID == stack.itemID && e.meta == stack.getItemDamage()) {
                    tip.add("§e" + I18n.getString("shop.price")     + ": §f" + Money.format(e.buyTenths));
                    tip.add("§a" + I18n.getString("shop.sellprice") + ": §f" + Money.format(e.sellTenths));
                    if (mc.thePlayer.capabilities.isCreativeMode)
                        tip.add("§7" + I18n.getString("shop.editPrice.hint"));
                    break;
                }
            }
        }
        func_102021_a(tip, x, y);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseXRel, int mouseYRel) {
        fontRenderer.drawString(I18n.getString(KEY_TITLE), TITLE_X, TITLE_Y, 0x404040);

        String pageStr = I18n.getString(KEY_PAGE)
                .replace("{page}",  String.valueOf(currentPage + 1))
                .replace("{pages}", String.valueOf(totalPages));
        fontRenderer.drawString(pageStr, (xSize - fontRenderer.getStringWidth(pageStr)) / 2, PAGE_Y, 0x606060);

        fontRenderer.drawString(I18n.getString(KEY_INVENTORY), PLAYER_LABEL_X, PLAYER_LABEL_Y, 0x404040);

        String money = I18n.getString(KEY_MONEY) + ": " + Money.format(ShopClientData.balance);
        fontRenderer.drawString(money,
                PLAYER_BG_X + (PLAYER_BG_W - fontRenderer.getStringWidth(money)) / 2,
                PLAYER_LABEL_Y, 0xFF3498DB);

        // Placeholder text inside search box when empty and unfocused
        if (searchBox != null && !searchBox.isFocused() && searchBox.getText().isEmpty()) {
            fontRenderer.drawString(I18n.getString("shop.search.hint"),
                    searchBoxX - guiLeft + 2, searchBoxY - guiTop + 1, 0x888888);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if (searchBox != null) searchBox.mouseClicked(mx, my, button);
        for (Object o : buttonList) {
            if (o instanceof IconButton b && b.mousePressed(mc, mx, my)) {
                actionPerformed(b);
                return;
            }
        }

        boolean shift = isShiftKeyDown();
        boolean alt   = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        Slot slot = getSlotUnderMouse(mx, my);

        if (slot != null && slot.inventory == mc.thePlayer.inventory && button == 0 && alt) {
            ItemStack s = slot.getStack();
            if (s != null && !isEquipment(s.getItem())) {
                for (int i = 0; i < mc.thePlayer.inventory.mainInventory.length; i++) {
                    ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
                    if (stack != null && stack.itemID == s.itemID
                            && stack.getItemDamage() == s.getItemDamage()
                            && !isEquipment(stack.getItem())) {
                        ShopC2S.sendSellRequest(stack.itemID, stack.stackSize, i);
                    }
                }
                return;
            }
        }

        if (slot != null && slot.inventory == mc.thePlayer.inventory && button == 0 && shift) {
            ItemStack s = slot.getStack();
            if (s != null) {
                int ci = slot.slotNumber;
                ShopC2S.sendSellRequest(s.itemID, s.stackSize,
                        ci < 27 ? (9 + ci) : (ci - 27));
                return;
            }
        }

        int cap = capacityPerPage();
        int gl  = (width - xSize) / 2;
        int gt  = (height - ySize) / 2;
        int local = getHoverLocalIndex(mx - gl, my - gt);

        if (local >= 0 && button == 0) {
            int index = currentPage * cap + local;
            if (index < viewEntries.size())
            {
                SystemShopClientCatalog.Entry entry = entries(index);
                if (alt && mc.thePlayer.capabilities.isCreativeMode)
                {
                    ItemStack editStack = entry.toStack();
                    if (editStack != null)
                    {
                        editStack.stackSize = 1;
                        mc.displayGuiScreen(new GuiEditPrice(this, editStack));
                    }
                    return;
                }
                int count = shift ? Item.itemsList[entry.itemID].maxStackSize : 1;
                if (!alt) ShopC2S.sendPurchaseRequest(entry.itemID, entry.meta, count);
                return;
            }
        }
        super.mouseClicked(mx, my, button);
    }

    private SystemShopClientCatalog.Entry entries(int index) {
        return viewEntries.get(index);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel > 0 && currentPage > 0) { currentPage--; updatePageButtons(); }
        else if (wheel < 0 && currentPage < totalPages - 1) { currentPage++; updatePageButtons(); }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        // When search box is focused: only ESC dismisses the GUI; every other key
        // (including the inventory-close hotkey) is consumed by the text field.
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.thePlayer.closeScreen();
                return;
            }
            searchBox.textboxKeyTyped(c, keyCode);
            String newQuery = searchBox.getText().trim();
            if (!newQuery.equals(searchQuery)) {
                searchQuery = newQuery;
                currentPage = 0;
                rebuildView();
                updatePageButtons();
            }
            return;
        }
        // Search box not focused: allow normal GUI close keys
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == mc.gameSettings.keyBindInventory.keyCode) {
            mc.thePlayer.closeScreen();
            return;
        }
        super.keyTyped(c, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private int getHoverLocalIndex(int relX, int relY) {
        int x = relX - SHOP_START_X;
        int y = relY - SHOP_START_Y;
        if (x < 0 || y < 0) return -1;
        int col = x / SLOT_SIZE, row = y / SLOT_SIZE;
        if (col >= SHOP_COLS || row >= SHOP_ROWS) return -1;
        return row * SHOP_COLS + col;
    }

    private Slot getSlotUnderMouse(int mouseX, int mouseY) {
        for (Object o : inventorySlots.inventorySlots) {
            Slot s = (Slot) o;
            int x = guiLeft + s.xDisplayPosition;
            int y = guiTop  + s.yDisplayPosition;
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) return s;
        }
        return null;
    }

    private boolean isEquipment(Item item) {
        return item instanceof ItemArmor || item instanceof ItemTool
                || item instanceof ItemBow || item instanceof ItemHoe
                || item instanceof ItemFishingRod || item instanceof ItemShears;
    }

    void blit(int x, int y, int u, int v, int w, int h) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        float fw = 1F / 512F, fh = 1F / 512F;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x,     y + h, zLevel, u * fw,       (v + h) * fh);
        t.addVertexWithUV(x + w, y + h, zLevel, (u + w) * fw, (v + h) * fh);
        t.addVertexWithUV(x + w, y,     zLevel, (u + w) * fw, v * fh);
        t.addVertexWithUV(x,     y,     zLevel, u * fw,       v * fh);
        t.draw();
    }

    /**
     * Generic icon button backed by {@link EnumIcon}.
     * Renders the correct UV state (normal / hover / disabled) directly onto the atlas.
     * {@code drawButton()} is suppressed; rendering happens in the background layer.
     */
    static class IconButton extends GuiButton {

        final EnumIcon icon;

        IconButton(int id, int x, int y, EnumIcon icon) {
            super(id, x, y, icon.w, icon.h, "");
            this.icon = icon;
        }

        /** Use this when the clickable area (e.g. 22×22 bg) differs from the icon sprite size. */
        IconButton(int id, int x, int y, EnumIcon icon, int hitW, int hitH) {
            super(id, x, y, hitW, hitH, "");
            this.icon = icon;
        }

        boolean isHovered(int mx, int my) {
            return mx >= xPosition && my >= yPosition
                    && mx < xPosition + width && my < yPosition + height;
        }

        @Override public void drawButton(Minecraft mc, int mx, int my) {}

        @Override
        public boolean mousePressed(Minecraft mc, int mx, int my) {
            return enabled && drawButton && isHovered(mx, my);
        }
    }
}