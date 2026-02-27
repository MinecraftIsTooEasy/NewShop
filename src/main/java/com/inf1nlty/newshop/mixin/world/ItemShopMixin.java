package com.inf1nlty.newshop.mixin.world;

import com.inf1nlty.newshop.GoodsConfig;
import com.inf1nlty.newshop.ShopItem;
import com.inf1nlty.newshop.ShopListing;
import net.minecraft.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class ItemShopMixin implements ShopItem {

    @Unique
    @Override
    public double newShop$getSoldPrice(int subtype) {
        Item self = (Item) (Object) this;
        ShopListing listing = GoodsConfig.get(self.itemID, subtype);
        return listing != null ? listing.sellPriceTenths / 10.0 : 0.0;
    }

    @Unique
    @Override
    public double newShop$getBuyPrice(int subtype) {
        Item self = (Item) (Object) this;
        ShopListing listing = GoodsConfig.get(self.itemID, subtype);
        return listing != null ? listing.buyPriceTenths / 10.0 : 0.0;
    }

    @Unique
    @Override
    public void newShop$setSoldPrice(int subtype, double soldPrice) {
        Item self = (Item) (Object) this;
        ShopListing existing = GoodsConfig.get(self.itemID, subtype);
        int buyTenths = existing != null ? existing.buyPriceTenths : 0;
        GoodsConfig.savePrice(self.itemID, subtype, buyTenths, (int) Math.round(soldPrice * 10.0));
    }

    @Unique
    @Override
    public void newShop$setBuyPrice(int subtype, double buyPrice) {
        Item self = (Item) (Object) this;
        ShopListing existing = GoodsConfig.get(self.itemID, subtype);
        int sellTenths = existing != null ? existing.sellPriceTenths : 0;
        GoodsConfig.savePrice(self.itemID, subtype, (int) Math.round(buyPrice * 10.0), sellTenths);
    }

    @Unique
    @Override
    public void newShop$clearPrice() {
        Item self = (Item) (Object) this;
        GoodsConfig.savePrice(self.itemID, 0, 0, 0);
    }
}