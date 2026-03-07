package com.inf1nlty.newshop.client.state;

/** Client-side ephemeral state for shop UIs. */
public class ShopClientData {

    /** Player balance in hundredths (two decimal currency, e.g. 1234 = 12.34). */
    public static int balance = 0;

    public static boolean inShop = false;
    public static boolean inGlobalShop = false;
    public static boolean inMailbox = false;
}