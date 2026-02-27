package com.inf1nlty.newshop.client.state;

/** Client-side ephemeral state for shop UIs. */
public class ShopClientData {

    /** Player balance in tenths (one decimal currency). */
    public static int balance = 0;

    public static boolean inShop       = false;
    public static boolean inGlobalShop = false;
    public static boolean inMailbox    = false;
}