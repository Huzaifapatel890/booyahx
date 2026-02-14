package com.booyahx;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cache manager for withdrawal limit data.
 * Now populated from merged /api/wallet/balance endpoint.
 *
 * Stores:
 * - maxWithdrawableGC: Calculated by backend (50% of deposits minus withdrawn)
 * - balanceGC: Current wallet balance
 * - totalDepositedGC: Total amount deposited
 * - withdrawnGC: Total amount already withdrawn
 */
public class WalletLimitCache {

    private static final String PREF_NAME = "wallet_limit_cache";
    private static final String KEY_MAX_WITHDRAWABLE_GC = "max_withdrawable_gc";
    private static final String KEY_BALANCE_GC = "balance_gc";
    private static final String KEY_TOTAL_DEPOSITED_GC = "total_deposited_gc";
    private static final String KEY_WITHDRAWN_GC = "withdrawn_gc";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_IS_AVAILABLE = "is_available";

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save withdrawal limit data to cache.
     * Called from WalletFragment when /api/wallet/balance response is received.
     */
    public static void saveLimit(Context context, int maxWithdrawableGC, int balanceGC,
                                 int totalDepositedGC, int withdrawnGC) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(KEY_MAX_WITHDRAWABLE_GC, maxWithdrawableGC);
        editor.putInt(KEY_BALANCE_GC, balanceGC);
        editor.putInt(KEY_TOTAL_DEPOSITED_GC, totalDepositedGC);
        editor.putInt(KEY_WITHDRAWN_GC, withdrawnGC);
        editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
        editor.putBoolean(KEY_IS_AVAILABLE, true);
        editor.apply();
    }

    /**
     * Get max withdrawable amount from cache
     */
    public static int getMaxWithdrawableGC(Context context) {
        return getPreferences(context).getInt(KEY_MAX_WITHDRAWABLE_GC, 0);
    }

    /**
     * Get balance from cache
     */
    public static int getBalanceGC(Context context) {
        return getPreferences(context).getInt(KEY_BALANCE_GC, 0);
    }

    /**
     * Get total deposited amount from cache
     */
    public static int getTotalDepositedGC(Context context) {
        return getPreferences(context).getInt(KEY_TOTAL_DEPOSITED_GC, 0);
    }

    /**
     * Get withdrawn amount from cache
     */
    public static int getWithdrawnGC(Context context) {
        return getPreferences(context).getInt(KEY_WITHDRAWN_GC, 0);
    }

    /**
     * Check if cached data is available
     */
    public static boolean isLimitAvailable(Context context) {
        return getPreferences(context).getBoolean(KEY_IS_AVAILABLE, false);
    }

    /**
     * Get last update timestamp
     */
    public static long getLastUpdateTime(Context context) {
        return getPreferences(context).getLong(KEY_LAST_UPDATE, 0);
    }

    /**
     * Clear all cached data
     */
    public static void clearCache(Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Mark cache as unavailable (when API call fails)
     */
    public static void markUnavailable(Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(KEY_IS_AVAILABLE, false);
        editor.apply();
    }
}