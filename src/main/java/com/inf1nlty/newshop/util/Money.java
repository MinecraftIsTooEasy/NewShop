package com.inf1nlty.newshop.util;

/** Currency formatting utilities (hundredths, two decimal places). */
public final class Money {

    private Money() {}

    public static String format(int hundredths)
    {
        int whole = hundredths / 100;
        int frac  = Math.abs(hundredths % 100);
        return whole + "." + String.format("%02d", frac);
    }
}