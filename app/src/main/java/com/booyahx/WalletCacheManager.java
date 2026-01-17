package com.booyahx;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages local cache for wallet balance
 * Only hits API once, then uses cached data until balance update
 */
public class WalletCacheManager {

    private static final String PREF_NAME = "wallet_cache";
    private static final String KEY_BALANCE = "balance_gc";
    private static final String KEY_LAST_UPDATED = "last_updated";

    // ✅ SAVE BALANCE TO LOCAL STORAGE
    public static void saveBalance(Context context, double balance) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putFloat(KEY_BALANCE, (float) balance)
                .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                .apply();
    }

    // ✅ GET BALANCE FROM LOCAL STORAGE
    public static double getBalance(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_BALANCE, 0f);
    }

    // ✅ CHECK IF BALANCE EXISTS IN CACHE
    public static boolean hasBalance(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_BALANCE);
    }

    // ✅ UPDATE BALANCE (AFTER TOP-UP, WITHDRAW, JOIN TOURNAMENT)
    public static void updateBalance(Context context, double newBalance) {
        saveBalance(context, newBalance);
    }

    // ✅ DEDUCT BALANCE (AFTER JOINING TOURNAMENT)
    public static void deductBalance(Context context, double amount) {
        double currentBalance = getBalance(context);
        saveBalance(context, currentBalance - amount);
    }

    // ✅ ADD BALANCE (AFTER TOP-UP OR WINNING)
    public static void addBalance(Context context, double amount) {
        double currentBalance = getBalance(context);
        saveBalance(context, currentBalance + amount);
    }

    // ✅ CLEAR BALANCE (ON LOGOUT)
    public static void clear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    // ✅ GET LAST UPDATE TIME
    public static long getLastUpdated(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_UPDATED, 0);
    }

    // ✅ GET FORMATTED BALANCE STRING
    public static String getFormattedBalance(Context context) {
        double balance = getBalance(context);
        return String.format("%.2f GC", balance);
    }

    // ✅ GET BALANCE AS INTEGER (NO DECIMALS)
    public static int getBalanceAsInt(Context context) {
        return (int) Math.round(getBalance(context));
    }
}
