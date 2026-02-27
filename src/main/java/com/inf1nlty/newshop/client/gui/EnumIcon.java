package com.inf1nlty.newshop.client.gui;

/**
 * Texture icon descriptors for the shop GUI atlas (512x512 shop_gui.png).
 * === Page-turn buttons (10×15, original positions) ===
 *   NEXT: normal U=477 V=1,  hover U=489 V=1,  disabled U=501 V=1
 *   PREV: normal U=477 V=20, hover U=489 V=20, disabled U=501 V=20
 * === Side-panel buttons (right side of GUI) ===
 *   SORT_ORDER  (18×22): right-most from right edge → U=494, V=37
 *   FILTER_TYPE (18×22): 23px gap from right edge   → U=471, V=37
 * === Side-panel button backgrounds (22×22) ===
 *   BTN_BG_ACTIVE  (selected bg): 42px from right → U=448, V=37
 *   BTN_BG_NORMAL  (default bg):  one step left   → U=426, V=37
 * === Unlist button icon (5×5) ===
 *   Top-right corner, 37px gap from right edge, hugging top → U=470, V=0
 */
public enum EnumIcon {

    // Page-turn (10 × 15) ── three horizontal states: normal / hover / disabled
    PAGE_NEXT (477, 1,  489, 1,  501, 1,  10, 15),
    PAGE_PREV (477, 20, 489, 20, 501, 20, 10, 15),

    // Side-panel icons (18 × 22) ── single sprite (no state variation needed; bg handles hover)
    SORT_ORDER  (494, 37, 494, 37, 494, 37, 18, 22),
    FILTER_TYPE (471, 37, 471, 37, 471, 37, 18, 22),

    // Side-panel button backgrounds (22 × 22)
    BTN_BG_ACTIVE (448, 37, 448, 37, 448, 37, 22, 22),  // selected / hovered
    BTN_BG_NORMAL (426, 37, 426, 37, 426, 37, 22, 22),  // default

    // Unlist icon (5 × 5) — top-right corner, 37px gap from right edge, V=0
    UNLIST (470, 0, 470, 0, 470, 0, 5, 5),
    ;

    /** UV for the normal (idle) state. */
    public final int uNormal, vNormal;
    /** UV for the hovered state. */
    public final int uHover, vHover;
    /** UV for the disabled state. */
    public final int uDisabled, vDisabled;
    /** Sprite dimensions. */
    public final int w, h;

    EnumIcon(int uN, int vN, int uH, int vH, int uD, int vD, int w, int h) {
        this.uNormal = uN; this.vNormal = vN;
        this.uHover  = uH; this.vHover  = vH;
        this.uDisabled = uD; this.vDisabled = vD;
        this.w = w; this.h = h;
    }

    public int u(boolean enabled, boolean hovered) {
        if (!enabled) return uDisabled;
        return hovered ? uHover : uNormal;
    }

    public int v(boolean enabled, boolean hovered) {
        if (!enabled) return vDisabled;
        return hovered ? vHover : vNormal;
    }
}
