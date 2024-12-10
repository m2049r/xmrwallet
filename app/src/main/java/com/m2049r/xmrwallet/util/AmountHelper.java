package com.m2049r.xmrwallet.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class AmountHelper {
    static final DecimalFormat AmountFormatter_12;

    static {
        AmountFormatter_12 = new DecimalFormat();
        AmountFormatter_12.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        AmountFormatter_12.setMinimumIntegerDigits(1);
        AmountFormatter_12.setMaximumFractionDigits(12);
        AmountFormatter_12.setGroupingUsed(false);
    }

    public static String format(double amount) {
        return AmountFormatter_12.format(amount);
    }

    public static String format_6(double amount) {
        int n = (int) Math.ceil(Math.log10(amount));
        int d = Math.max(2, 6 - n);
        final String fmt = "%,." + d + "f";
        return String.format(Locale.getDefault(), fmt, amount);
    }
}
