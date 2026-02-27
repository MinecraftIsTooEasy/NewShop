package com.inf1nlty.newshop.event;

import com.inf1nlty.newshop.commands.GlobalShopCommand;
import com.inf1nlty.newshop.commands.MoneyCommand;
import com.inf1nlty.newshop.commands.ShopCommand;
import moddedmite.rustedironcore.api.event.events.CommandRegisterEvent;

import java.util.function.Consumer;

public class ShopCommandEvents implements Consumer<CommandRegisterEvent> {

    @Override
    public void accept(CommandRegisterEvent event) {
        event.register(new ShopCommand());
        event.register(new GlobalShopCommand());
        event.register(new MoneyCommand());
    }
}