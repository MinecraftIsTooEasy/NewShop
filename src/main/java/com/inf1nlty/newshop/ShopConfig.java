package com.inf1nlty.newshop;

import fi.dy.masa.malilib.config.ConfigTab;
import fi.dy.masa.malilib.config.SimpleConfigs;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;

import java.util.ArrayList;
import java.util.List;

public class ShopConfig extends SimpleConfigs {

    public static final ConfigBoolean FORCE_SELL_UNLISTED = new ConfigBoolean(
            "shop.forceSellUnlisted", false, "shop.forceSellUnlisted");

    public static final ConfigBoolean SKYBLOCK_MODE = new ConfigBoolean(
            "shop.skyblockMode", false, "shop.skyblockMode");

    public static final ConfigBoolean ANNOUNCE_GLOBAL_LISTING = new ConfigBoolean(
            "shop.announceGlobalListing", false, "shop.announceGlobalListing");

    public static final ConfigInteger MAX_GLOBAL_LISTINGS = new ConfigInteger(
            "shop.maxGlobalListings", 5, 1, 999, "shop.maxGlobalListings");

    public static final List<ConfigBase<?>> ALL;
    public static final List<ConfigTab>     TABS;
    private static final ShopConfig         INSTANCE;

    static
    {
        ALL = List.of(FORCE_SELL_UNLISTED, SKYBLOCK_MODE, ANNOUNCE_GLOBAL_LISTING, MAX_GLOBAL_LISTINGS);
        TABS = new ArrayList<>();
        TABS.add(new ConfigTab("general", ALL));
        INSTANCE = new ShopConfig("NewShop", ALL);
    }

    private ShopConfig(String name, List<ConfigBase<?>> values)
    {
        super(name, null, values);
    }

    @Override
    public List<ConfigTab> getConfigTabs() {
        return TABS;
    }

    public static ShopConfig getInstance() {
        return INSTANCE;
    }
}