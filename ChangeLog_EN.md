# 【New Shop】

Features such as a system shop, trading post, and purchase box have been added. The default key for the system shop is B, the default key for the trading post is G, and the default key for the mailbox is K (this does not use F3).

The system shop provides some default items, and you can also add your own. With OP privileges and in Creative mode, you can add/edit items by pressing ALT + left-clicking the EMI sidebar, or by adding items from your Creative mode inventory, or by adding them through the `.minecraft/config/newshop.cfg` file, which specifies the format for adding items. There is a configuration option for forced selling, used to destroy items (items without a set price are disabled for selling by default), which is disabled by default.

The trading post provides player-to-player trading functionality. Players can place buy/sell orders here. You can open the listing page by pressing ALT + right-clicking in your inventory/container interface. In OP and Creative modes, you can also directly list items from interfaces such as the EMI or Creative mode inventory for easy resource distribution. Furthermore, by locating the configuration file `newshop.json` (server configuration) and enabling the global announcement, the entire server will be notified of what items have been listed. Each player has a default list size of 5 (configurable). Currently, the purchase system only allows listing orders via the command `/gs b`, which has detailed instructions.

The mailbox is used to store purchase orders, in case you are offline and unable to receive them.

# 【New Shop】 v1.0.1

* Fixed some bugs

* Fixed to support all variant items, such as different colors of wool

* Now supports adding items with NBT data to the system shop, such as enchanted tools.