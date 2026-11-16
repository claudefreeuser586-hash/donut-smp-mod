package com.example.donutsmp.economy;

/** يحوّل السنتات إلى نص مختصر مثل $1.5K و $2.3M */
public final class MoneyFormatter {
    private MoneyFormatter() {}

    public static String format(long cents) {
        double dollars = cents / 100.0;
        String sign = dollars < 0 ? "-" : "";
        double abs = Math.abs(dollars);

        if (abs >= 1_000_000_000_000.0) return sign + "$" + trim(abs / 1_000_000_000_000.0) + "T";
        if (abs >= 1_000_000_000.0) return sign + "$" + trim(abs / 1_000_000_000.0) + "B";
        if (abs >= 1_000_000.0) return sign + "$" + trim(abs / 1_000_000.0) + "M";
        if (abs >= 1_000.0) return sign + "$" + trim(abs / 1_000.0) + "K";
        return sign + "$" + String.format("%.2f", abs);
    }

    private static String trim(double value) {
        String s = String.format("%.2f", value);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
