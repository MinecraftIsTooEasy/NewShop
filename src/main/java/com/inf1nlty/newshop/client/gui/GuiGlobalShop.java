package com.inf1nlty.newshop.client.gui;

import com.inf1nlty.newshop.client.state.ShopClientData;
import com.inf1nlty.newshop.inventory.ContainerShopPlayer;
import com.inf1nlty.newshop.network.ShopC2S;
import com.inf1nlty.newshop.util.Money;
import com.inf1nlty.newshop.util.SearchHelper;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Global marketplace GUI (19 × 7 grid).
 * Supports sort (ASC/DESC by unit price) and filter (ALL / SELL_ONLY / BUY_ONLY).
 */
public class GuiGlobalShop extends GuiContainer {

    private static final int GUI_W = 356;
    private static final int GUI_H = 240;
    private static final int GRID_BG_W = 356;
    private static final int GRID_BG_H = 150;
    private static final int PLAYER_BG_X = 90;
    private static final int PLAYER_BG_Y = 150;
    private static final int PLAYER_BG_W = 176;
    private static final int PLAYER_BG_H = 90;

    private static final int LISTING_COLS = 19;
    private static final int LISTING_ROWS = 7;
    private static final int LISTING_START_X = 8;
    private static final int LISTING_START_Y = 18;
    private static final int SLOT_SIZE = 18;

    private static final int PLAYER_LABEL_X = 97;
    private static final int PLAYER_LABEL_Y = 145;
    private static final int HOVER_COLOR       = 0x40FFFFFF;
    private static final int SELL_BORDER_COLOR = 0xFFE000;
    private static final int BUY_BORDER_COLOR  = 0x6034DB34;

    private static final int TITLE_X = 15;
    private static final int TITLE_Y = 5;
    private static final int PAGE_Y  = 5;

    private static final String KEY_TITLE     = "globalshop.title";
    private static final String KEY_PAGE      = "shop.page";
    private static final String KEY_INVENTORY = "shop.inventory";
    private static final String KEY_MONEY     = "shop.money";

    // Button IDs
    private static final int BTN_PREV        = 200;
    private static final int BTN_NEXT        = 201;
    private static final int BTN_SORT        = 202;
    private static final int BTN_FILTER      = 203;
    private static final int BTN_UNLIST_BASE = 1000;

    // Side-panel button placement — right side OUTSIDE the GUI
    private static final int SIDE_BTN_OFFSET_X = GUI_W + 3;
    private static final int SIDE_BTN_SORT_Y   = 34;
    private static final int SIDE_BTN_FILTER_Y = 60;

    // Sort / filter state
    private enum SortOrder  { DEFAULT, ASC, DESC }
    private enum FilterMode { ALL, SELL_ONLY, BUY_ONLY }

    private SortOrder  sortOrder  = SortOrder.DEFAULT;
    private FilterMode filterMode = FilterMode.ALL;
    private String     searchQuery = "";

    public static final List<GlobalListingClient> CLIENT_LISTINGS = new ArrayList<>();
    /** View after applying current filter, search, then sort. */
    private List<GlobalListingClient> viewListings = new ArrayList<>();

    private int totalPages;
    private int currentPage;
    private ItemStack hoveredStack;
    private GlobalListingClient hoveredListing;
    private GuiTextField searchBox;
    private int searchBoxX, searchBoxY;

    public GuiGlobalShop(EntityPlayer player, ContainerShopPlayer container) {
        super(container);
        xSize = GUI_W;
        ySize = GUI_H;
    }

    public static void setSnapshot(List<GlobalListingClient> snapshot) {
        CLIENT_LISTINGS.clear();
        CLIENT_LISTINGS.addAll(snapshot);
        for (GlobalListingClient c : CLIENT_LISTINGS) {
            if (c.nbtCompressed != null && c.nbtCompressed.length > 0) {
                try { c.nbt = CompressedStreamTools.readCompressed(
                        new ByteArrayInputStream(c.nbtCompressed)); }
                catch (Exception ignored) {}
            }
        }
    }

    private int capacityPerPage() { return LISTING_COLS * LISTING_ROWS; }

    private void rebuildView() {
        viewListings = new ArrayList<>();
        for (GlobalListingClient lc : CLIENT_LISTINGS) {
            // --- filter ---
            switch (filterMode) {
                case SELL_ONLY -> { if (lc.isBuyOrder)  continue; }
                case BUY_ONLY  -> { if (!lc.isBuyOrder) continue; }
                default        -> {}
            }
            // --- search ---
            if (!searchQuery.isBlank()) {
                Item item = (lc.itemId >= 0 && lc.itemId < Item.itemsList.length)
                        ? Item.itemsList[lc.itemId] : null;
                if (item == null) continue;
                String name = new ItemStack(item, 1, lc.meta).getDisplayName();
                if (!SearchHelper.matches(name, searchQuery)) continue;
            }
            viewListings.add(lc);
        }
        Comparator<GlobalListingClient> cmp = Comparator.comparingInt(lc -> lc.priceTenths);
        if (sortOrder == SortOrder.DEFAULT) {
            // Natural order: sort by listing ID (ascending), which reflects creation order
            viewListings.sort(Comparator.comparingInt(lc -> lc.listingId));
        } else {
            if (sortOrder == SortOrder.DESC) cmp = cmp.reversed();
            viewListings.sort(cmp);
        }

        int cap = capacityPerPage();
        totalPages = Math.max(1, (viewListings.size() + cap - 1) / cap);
        if (currentPage >= totalPages) currentPage = totalPages - 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        ShopClientData.inGlobalShop = true;
        ShopClientData.inShop = false;

        rebuildView();

        buttonList.clear();
        int gl = (width  - xSize) / 2;
        int gt = (height - ySize) / 2;

        buttonList.add(new GuiShop.IconButton(BTN_NEXT, gl + GUI_W - 1 - EnumIcon.PAGE_NEXT.w, gt + 1, EnumIcon.PAGE_NEXT));
        buttonList.add(new GuiShop.IconButton(BTN_PREV, gl + 1,                                gt + 1, EnumIcon.PAGE_PREV));
        buttonList.add(new GuiShop.IconButton(BTN_SORT,   gl + SIDE_BTN_OFFSET_X, gt + SIDE_BTN_SORT_Y,   EnumIcon.SORT_ORDER,  22, 22));
        buttonList.add(new GuiShop.IconButton(BTN_FILTER, gl + SIDE_BTN_OFFSET_X, gt + SIDE_BTN_FILTER_Y, EnumIcon.FILTER_TYPE, 22, 22));

        // Search box — same position as GuiShop
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
        ShopClientData.inGlobalShop = false;
        super.onGuiClosed();
    }

    private void updatePageButtons() {
        for (Object o : buttonList) {
            if (o instanceof GuiShop.IconButton b) {
                if (b.id == BTN_PREV) b.enabled = currentPage > 0;
                if (b.id == BTN_NEXT) b.enabled = currentPage < totalPages - 1;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int cap = capacityPerPage();
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
            currentPage = 0; rebuildView(); updatePageButtons();
            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
        } else if (button.id == BTN_FILTER) {
            filterMode = switch (filterMode) {
                case ALL       -> FilterMode.SELL_ONLY;
                case SELL_ONLY -> FilterMode.BUY_ONLY;
                case BUY_ONLY  -> FilterMode.ALL;
            };
            currentPage = 0; rebuildView(); updatePageButtons();
            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
        } else if (button.id >= BTN_UNLIST_BASE) {
            int local = button.id - BTN_UNLIST_BASE;
            int gIdx  = currentPage * cap + local;
            if (gIdx >= 0 && gIdx < viewListings.size()) {
                GlobalListingClient lc = viewListings.get(gIdx);
                if (lc.owner != null && lc.owner.equals(mc.thePlayer.getEntityName())) {
                    ShopC2S.sendGlobalUnlist(lc.listingId);
                    mc.sndManager.playSoundFX("random.pop", 1.0F, 1.0F);
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchBox != null) searchBox.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        updateHoveredListing(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partial);
        if (theSlot == null && hoveredStack != null) drawItemStackTooltip(hoveredStack, mouseX, mouseY);
        drawSideButtonTooltips(mouseX, mouseY);
    }

    private void drawSideButtonTooltips(int mx, int my) {
        for (Object o : buttonList) {
            if (!(o instanceof GuiShop.IconButton b)) continue;
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
                    case SELL_ONLY -> "shop.filter.sell_only";
                    case BUY_ONLY  -> "shop.filter.buy_only";
                });
            }
            if (tip != null) { List<String> l = new ArrayList<>(); l.add(tip); func_102021_a(l, mx, my); }
        }
    }

    private void updateHoveredListing(int mouseXAbs, int mouseYAbs) {
        hoveredStack = null; hoveredListing = null;
        int cap = capacityPerPage();
        int gl = (width - xSize) / 2, gt = (height - ySize) / 2;
        int local = getHoverLocalIndex(mouseXAbs - gl, mouseYAbs - gt);
        if (local < 0) return;
        int gIdx = currentPage * cap + local;
        if (gIdx < 0 || gIdx >= viewListings.size()) return;
        GlobalListingClient lc = viewListings.get(gIdx);
        Item item = (lc.itemId >= 0 && lc.itemId < Item.itemsList.length) ? Item.itemsList[lc.itemId] : null;
        if (item == null) return;
        ItemStack stack = new ItemStack(item, Math.min(item.maxStackSize, lc.amount), lc.meta);
        if (lc.nbt != null) stack.stackTagCompound = (NBTTagCompound) lc.nbt.copy();
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger("GShopPriceTenths", lc.priceTenths);
        stack.stackTagCompound.setInteger("GShopAmount", lc.amount);
        stack.stackTagCompound.setString("GShopSeller", lc.owner);
        hoveredStack = stack; hoveredListing = lc;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.renderEngine.bindTexture(TextureManager.TEX);
        int gl = (width - xSize) / 2, gt = (height - ySize) / 2;
        blit(gl, gt, 0, 0, GRID_BG_W, GRID_BG_H);
        blit(gl + PLAYER_BG_X, gt + PLAYER_BG_Y, PLAYER_BG_X, PLAYER_BG_Y, PLAYER_BG_W, PLAYER_BG_H);
        renderAllButtons(mouseX, mouseY);
        // Draw search box here (background layer) so it isn't affected by foreground GL translate
        if (searchBox != null) {
            GL11.glDisable(GL11.GL_LIGHTING);
            searchBox.drawTextBox();
            GL11.glColor4f(1, 1, 1, 1);
        }
        renderListingItems(gl, gt, mouseX, mouseY);
    }

    private void renderAllButtons(int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        mc.renderEngine.bindTexture(TextureManager.TEX);
        for (Object object : buttonList) {
            if (!(object instanceof GuiShop.IconButton b) || !b.drawButton) continue;
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
        // Restore GL state to avoid polluting subsequent rendering
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);
    }

    @SuppressWarnings("unchecked")
    private void renderListingItems(int gl, int gt, int mouseXAbs, int mouseYAbs) {
        buttonList.removeIf(b -> b instanceof TinyButton && ((TinyButton) b).isUnlist);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);

        int cap = capacityPerPage();
        int startIndex = currentPage * cap;
        int endIndex   = Math.min(viewListings.size(), startIndex + cap);
        int hoverLocal = getHoverLocalIndex(mouseXAbs - gl, mouseYAbs - gt);

        for (int idx = startIndex; idx < endIndex; idx++) {
            GlobalListingClient lc = viewListings.get(idx);
            int local = idx - startIndex;
            int sx = gl + LISTING_START_X + (local % LISTING_COLS) * SLOT_SIZE;
            int sy = gt + LISTING_START_Y + (local / LISTING_COLS) * SLOT_SIZE;
            Item item = (lc.itemId >= 0 && lc.itemId < Item.itemsList.length) ? Item.itemsList[lc.itemId] : null;
            if (item == null) continue;
            ItemStack stack = new ItemStack(item, Math.min(item.maxStackSize, lc.amount), lc.meta);
            if (lc.nbt != null) stack.stackTagCompound = (NBTTagCompound) lc.nbt.copy();
            // Reset zLevel before each item to prevent enchantment effect leaking
            this.zLevel = 100.0F;
            itemRenderer.zLevel = 100.0F;
            itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.renderEngine, stack, sx, sy);
            itemRenderer.renderItemOverlayIntoGUI(fontRenderer, mc.renderEngine, stack, sx, sy);
            this.zLevel = 0.0F;
            itemRenderer.zLevel = 0.0F;
            drawRect(sx - 1, sy - 1, sx + 17, sy + 17, lc.isBuyOrder ? BUY_BORDER_COLOR : SELL_BORDER_COLOR);
            if (local == hoverLocal) {
                GL11.glDisable(GL11.GL_LIGHTING);
                drawGradientRect(sx, sy, sx + 16, sy + 16, HOVER_COLOR, HOVER_COLOR);
                GL11.glEnable(GL11.GL_LIGHTING);
            }
            if (lc.amount == -1) {
                String s = "∞";
                fontRenderer.drawStringWithShadow(s, sx + 16 - fontRenderer.getStringWidth(s), sy + 10, 0xFFFFFF);
            }
            if (lc.owner != null && lc.owner.equals(mc.thePlayer.username)) {
                int btnId = BTN_UNLIST_BASE + local;
                TinyButton ub = new TinyButton(btnId, sx + SLOT_SIZE - 5, sy, 4, 4, "X", true);
                ub.enabled = true;
                ub.drawButton(mc, mouseXAbs, mouseYAbs);
                buttonList.add(ub);
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
    protected void drawItemStackTooltip(ItemStack par1ItemStack, int par2, int par3) {
        EmiScreenManager.lastStackTooltipRendered = par1ItemStack;
        boolean ext = mc.gameSettings.advancedItemTooltips || isShiftKeyDown();
        List tip = par1ItemStack.getTooltip(mc.thePlayer, ext, theSlot);
        for (int i = 0; i < tip.size(); i++) {
            if (i == 0) tip.set(i, "§" + Integer.toHexString(par1ItemStack.getRarity().rarityColor) + tip.get(i));
            else        tip.set(i, EnumChatFormatting.GRAY + (String) tip.get(i));
        }
        if (ShopClientData.inGlobalShop && hoveredListing != null) {
            boolean buy = hoveredListing.isBuyOrder;
            tip.add("§9" + I18n.getString(buy ? "gshop.buyorder" : "gshop.sellorder"));
            tip.add("§e" + I18n.getString("gshop.price")  + ": §f" + Money.format(hoveredListing.priceTenths));
            tip.add("§b" + I18n.getString("gshop.amount") + ": §f" + (hoveredListing.amount == -1 ? "∞" : hoveredListing.amount));
            tip.add((buy ? "§b" : "§d") + I18n.getString(buy ? "gshop.buyer" : "gshop.seller") + ": §f" + hoveredListing.owner);
        }
        func_102021_a(tip, par2, par3);
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
        fontRenderer.drawString(money, PLAYER_BG_X + (PLAYER_BG_W - fontRenderer.getStringWidth(money)) / 2, PLAYER_LABEL_Y, 0xFF3498DB);

        // Placeholder text inside search box when empty and unfocused
        if (searchBox != null && !searchBox.isFocused() && searchBox.getText().isEmpty()) {
            fontRenderer.drawString(I18n.getString("shop.search.hint"),
                    searchBoxX - guiLeft + 2, searchBoxY - guiTop + 1, 0x888888);
        }
    }

    @Override
    protected void mouseClicked(int mouseXAbs, int mouseYAbs, int button) {
        if (searchBox != null) searchBox.mouseClicked(mouseXAbs, mouseYAbs, button);
        for (Object o : buttonList) {
            if (o instanceof GuiShop.IconButton b && b.mousePressed(mc, mouseXAbs, mouseYAbs)) { actionPerformed(b); return; }
        }
        for (Object o : buttonList) {
            if (o instanceof TinyButton b && b.mousePressed(mc, mouseXAbs, mouseYAbs)) { actionPerformed(b); return; }
        }
        int cap = capacityPerPage();
        int gl = (width - xSize) / 2, gt = (height - ySize) / 2;
        int local = getHoverLocalIndex(mouseXAbs - gl, mouseYAbs - gt);
        boolean alt   = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        boolean shift = isShiftKeyDown();

        if (local >= 0 && button == 0) {
            int gIdx = currentPage * cap + local;
            if (gIdx < viewListings.size()) {
                GlobalListingClient lc = viewListings.get(gIdx);
                if (lc.isBuyOrder) {
                    if (alt) {
                        for (int i = 0; i < mc.thePlayer.inventory.mainInventory.length; i++) {
                            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
                            if (stack != null && stack.itemID == lc.itemId && stack.getItemDamage() == lc.meta && !isEquipment(stack.getItem()))
                                ShopC2S.sendSellToBuyOrder(lc.listingId, stack.stackSize, i);
                        }
                        return;
                    }
                    int si = mc.thePlayer.inventory.currentItem;
                    ItemStack hand = mc.thePlayer.inventory.mainInventory[si];
                    if (shift) {
                        if (hand != null && hand.itemID == lc.itemId && hand.getItemDamage() == lc.meta && !isEquipment(hand.getItem()))
                            ShopC2S.sendSellToBuyOrder(lc.listingId, Math.min(hand.stackSize, hand.getItem().maxStackSize), si);
                        else mc.thePlayer.addChatMessage(I18n.getString("gshop.listing.add.fail_no_item"));
                        return;
                    }
                    if (hand != null && hand.itemID == lc.itemId && hand.getItemDamage() == lc.meta)
                        ShopC2S.sendSellToBuyOrder(lc.listingId, 1, si);
                    else mc.thePlayer.addChatMessage(I18n.getString("gshop.listing.add.fail_no_item"));
                } else {
                    int count = shift ? Math.min(Item.itemsList[lc.itemId].maxStackSize, lc.amount) : 1;
                    ShopC2S.sendGlobalBuy(lc.listingId, count);
                }
                return;
            }
        }
        super.mouseClicked(mouseXAbs, mouseYAbs, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        if (wheel > 0 && currentPage > 0) { currentPage--; updatePageButtons(); }
        else if (wheel < 0 && currentPage < totalPages - 1) { currentPage++; updatePageButtons(); }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        // When search box is focused: only ESC dismisses the GUI; every other key
        // (including the inventory-close hotkey) is consumed by the text field.
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.thePlayer.closeScreen(); return;
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
            mc.thePlayer.closeScreen(); return;
        }
        super.keyTyped(c, keyCode);
    }

    @Override public boolean doesGuiPauseGame() {
        return false;
    }

    private int getHoverLocalIndex(int relX, int relY) {
        int x = relX - LISTING_START_X, y = relY - LISTING_START_Y;
        if (x < 0 || y < 0) return -1;
        int col = x / SLOT_SIZE, row = y / SLOT_SIZE;
        if (col >= LISTING_COLS || row >= LISTING_ROWS) return -1;
        return row * LISTING_COLS + col;
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

    private static class TinyButton extends GuiButton {
        final boolean isUnlist;
        TinyButton(int id, int x, int y, int w, int h, String txt, boolean isUnlist) {
            super(id, x, y, w, h, txt);
            this.isUnlist = isUnlist;
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my)
        {
            if (!drawButton) return;
            boolean hov = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
            if (isUnlist)
            {
                EnumIcon icon = EnumIcon.UNLIST;
                int ix = xPosition + (width  - icon.w) / 2;
                int iy = yPosition + (height - icon.h) / 2;
                GL11.glColor4f(1, 1, 1, 1);
                mc.renderEngine.bindTexture(TextureManager.TEX);
                float fw = 1F / 512F, fh = 1F / 512F;
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(ix,          iy + icon.h, 0, icon.uNormal * fw,              (icon.vNormal + icon.h) * fh);
                tessellator.addVertexWithUV(ix + icon.w, iy + icon.h, 0, (icon.uNormal + icon.w) * fw,   (icon.vNormal + icon.h) * fh);
                tessellator.addVertexWithUV(ix + icon.w, iy,          0, (icon.uNormal + icon.w) * fw,   icon.vNormal * fh);
                tessellator.addVertexWithUV(ix,          iy,          0, icon.uNormal * fw,               icon.vNormal * fh);
                tessellator.draw();
            }
            else
            {
                int bg = hov ? 0x60FFFFFF : 0x30FFFFFF;
                int fg = hov ? 0xFFD700   : 0xE0E0E0;
                drawRect(xPosition, yPosition, xPosition + width, yPosition + height, bg);
                mc.fontRenderer.drawStringWithShadow("§l" + displayString,
                        xPosition + (width  - mc.fontRenderer.getStringWidth("§l" + displayString)) / 2,
                        yPosition + (height - mc.fontRenderer.FONT_HEIGHT) / 2, fg);
            }
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mx, int my) {
            return enabled && mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        }
    }

    public static class GlobalListingClient {
        public int listingId, itemId, meta, amount, priceTenths;
        public String owner;
        public boolean isBuyOrder;
        public byte[] nbtCompressed;
        public NBTTagCompound nbt;
    }

    @SuppressWarnings("unchecked")
    public void refreshListings() {
        rebuildView();
        // Re-add page + side buttons
        int gl = (width - xSize) / 2, gt = (height - ySize) / 2;
        buttonList.removeIf(b -> b instanceof GuiShop.IconButton);
        buttonList.add(new GuiShop.IconButton(BTN_NEXT, gl + GUI_W - 1 - EnumIcon.PAGE_NEXT.w, gt + 1, EnumIcon.PAGE_NEXT));
        buttonList.add(new GuiShop.IconButton(BTN_PREV, gl + 1,                                gt + 1, EnumIcon.PAGE_PREV));
        buttonList.add(new GuiShop.IconButton(BTN_SORT,   gl + SIDE_BTN_OFFSET_X, gt + SIDE_BTN_SORT_Y,   EnumIcon.SORT_ORDER,  22, 22));
        buttonList.add(new GuiShop.IconButton(BTN_FILTER, gl + SIDE_BTN_OFFSET_X, gt + SIDE_BTN_FILTER_Y, EnumIcon.FILTER_TYPE, 22, 22));
        // Recreate search box preserving current query
        if (searchBox == null) {
            int sbW = 120, sbH = 10;
            searchBoxX = gl + GUI_W - sbW - 15;
            searchBoxY = gt + 4;
            searchBox = new GuiTextField(fontRenderer, searchBoxX, searchBoxY, sbW, sbH);
            searchBox.setMaxStringLength(64);
            searchBox.setText(searchQuery);
            searchBox.setFocused(false);
        }
        updatePageButtons();
    }
}

