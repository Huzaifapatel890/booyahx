package com.booyahx;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREF = "booyahx_user";

    public static void saveTokens(Context ctx, String access, String refresh) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        editor.putString("access", access);
        editor.putString("refresh", refresh);
        editor.apply();
    }

    public static String getAccessToken(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("access", null);
    }

    public static String getRefreshToken(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("refresh", null);
    }

    public static void clearTokens(Context ctx) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static void logout(Context ctx) {
        clearTokens(ctx);
    }
}