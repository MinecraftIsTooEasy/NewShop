package com.inf1nlty.newshop.util;

import me.towdium.pinin.PinyinMatch;
import net.minecraft.I18n;
import net.minecraft.Minecraft;

/** Search helper: zh_CN uses pinyin matching, other locales use case-insensitive substring. */
public final class SearchHelper {

    private SearchHelper() {}

    /** Returns true if text matches query. Blank query always matches. */
    public static boolean matches(String text, String query)
    {
        if (query == null || query.isBlank()) return true;

        if (text == null || text.isEmpty()) return false;

        return isChineseLocale()
                ? PinyinMatch.contains(text, query)
                : text.toLowerCase().contains(query.toLowerCase());
    }

    public static boolean isChineseLocale()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null && mc.gameSettings != null && mc.gameSettings.language != null)

            return mc.gameSettings.language.startsWith("zh_CN");

        String title = I18n.getString("shop.title");
        return title != null && containsChinese(title);
    }

    private static boolean containsChinese(String str)
    {
        for (char ch : str.toCharArray())
            if (ch >= 0x4E00 && ch <= 0x9FFF) return true;
        return false;
    }

}