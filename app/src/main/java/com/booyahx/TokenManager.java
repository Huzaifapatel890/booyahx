package com.booyahx;

import android.content.Context;
import android.content.SharedPreferences;

public final class TokenManager {

    private static final String PREF = "booyahx_user";

    private TokenManager() {}

    /* ================= TOKENS ================= */

    public static void saveTokens(Context ctx, String access, String refresh) {
        SharedPreferences.Editor e =
                ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        e.putString("access", access);
        e.putString("refresh", refresh);
        e.apply();
    }

    public static String getAccessToken(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("access", null);
    }

    public static String getRefreshToken(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("refresh", null);
    }

    /* ================= CSRF ================= */

    public static void saveCsrf(Context ctx, String csrf) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString("csrf", csrf)
                .apply();
    }

    public static String getCsrf(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("csrf", null);
    }

    /* ================= USER ================= */

    public static void saveRole(Context ctx, String role) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString("role", role)
                .apply();
    }

    public static String getRole(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("role", null);
    }

    public static void saveUserId(Context ctx, String id) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString("user_id", id)
                .apply();
    }

    public static String getUserId(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("user_id", null);
    }

    /* ================= CLEAR ================= */

    public static void logout(Context ctx) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}