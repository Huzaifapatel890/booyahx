package com.booyahx;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREF = "booyahx_user";

    // ----------------------------------------------------
    // SAVE TOKENS
    // ----------------------------------------------------
    public static void saveTokens(Context ctx, String access, String refresh) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        editor.putString("access", access);
        editor.putString("refresh", refresh);
        editor.apply();
    }

    // ----------------------------------------------------
    // GET ACCESS TOKEN
    // ----------------------------------------------------
    public static String getAccessToken(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("access", null);
    }

    // ----------------------------------------------------
    // GET REFRESH TOKEN
    // ----------------------------------------------------
    public static String getRefreshToken(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("refresh", null);
    }

    // ----------------------------------------------------
    // CLEAR TOKENS (used for password change, logout)
    // ----------------------------------------------------
    public static void clearTokens(Context ctx) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    // ----------------------------------------------------
    // OPTIONAL: LOGOUT ALIAS
    // ----------------------------------------------------
    public static void logout(Context ctx) {
        clearTokens(ctx);
    }
}