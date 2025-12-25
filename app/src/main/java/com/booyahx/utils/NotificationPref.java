package com.booyahx.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPref {

    private static final String PREF = "notification_pref";
    private static final String KEY_UNREAD = "has_unread";

    public static void setUnread(Context context, boolean unread) {
        SharedPreferences sp =
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_UNREAD, unread).apply();
    }

    public static boolean hasUnread(Context context) {
        SharedPreferences sp =
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_UNREAD, false);
    }
}