package com.booyahx;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREF = "booyahx_user";

    /* ================= TOKENS ================= */

    public static void saveTokens(Context ctx, String access, String refresh) {
        SharedPreferences.Editor editor =
                ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
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

    /* ================= ROLE ================= */

    public static void saveRole(Context ctx, String role) {
        SharedPreferences.Editor editor =
                ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        editor.putString("role", role);
        editor.apply();
    }

    public static String getRole(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("role", null);
    }

    /* ================= USER ID ================= */

    public static void saveUserId(Context ctx, String userId) {
        SharedPreferences.Editor editor =
                ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        editor.putString("user_id", userId);
        editor.apply();
    }

    public static String getUserId(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("user_id", null);
    }

    /* ================= CLEAR ================= */

    public static void clearTokens(Context ctx) {
        SharedPreferences.Editor editor =
                ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();

        // Explicit clearing (safe & readable)
        editor.remove("access");
        editor.remove("refresh");
        editor.remove("role");
        editor.remove("user_id");

        editor.apply();
    }

    public static void logout(Context ctx) {
        clearTokens(ctx);
    }
}