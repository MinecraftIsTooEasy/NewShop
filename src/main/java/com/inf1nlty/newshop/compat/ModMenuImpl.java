package com.inf1nlty.newshop.compat;

import com.inf1nlty.newshop.ShopConfig;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

public class ModMenuImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return parent -> ShopConfig.getInstance().getConfigScreen(parent);
    }
}