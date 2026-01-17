package com.booyahx;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.booyahx.network.models.ProfileResponse;

/**
 * Manages local cache for user profile data
 * Only hits API once, then uses cached data until update/logout
 */
public class ProfileCacheManager {

    private static final String PREF_NAME = "profile_cache";
    private static final String KEY_PROFILE_DATA = "profile_data";
    private static final String KEY_LAST_UPDATED = "last_updated";

    private static final Gson gson = new Gson();

    // ✅ SAVE PROFILE TO LOCAL STORAGE
    public static void saveProfile(Context context, ProfileResponse.Data profile) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(profile);

        prefs.edit()
                .putString(KEY_PROFILE_DATA, json)
                .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                .apply();
    }

    // ✅ GET PROFILE FROM LOCAL STORAGE
    public static ProfileResponse.Data getProfile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PROFILE_DATA, null);

        if (json == null) return null;

        try {
            return gson.fromJson(json, ProfileResponse.Data.class);
        } catch (Exception e) {
            return null;
        }
    }

    // ✅ CHECK IF PROFILE EXISTS IN CACHE
    public static boolean hasProfile(Context context) {
        return getProfile(context) != null;
    }

    // ✅ GET SPECIFIC FIELDS (CONVENIENCE METHODS)
    public static String getName(Context context) {
        ProfileResponse.Data profile = getProfile(context);
        return profile != null ? profile.name : null;
    }

    public static String getEmail(Context context) {
        ProfileResponse.Data profile = getProfile(context);
        return profile != null ? profile.email : null;
    }

    public static String getIgn(Context context) {
        ProfileResponse.Data profile = getProfile(context);
        return profile != null ? profile.ign : null;
    }

    public static String getRole(Context context) {
        ProfileResponse.Data profile = getProfile(context);
        return profile != null ? profile.role : "user";
    }

    public static String getUserId(Context context) {
        ProfileResponse.Data profile = getProfile(context);
        return profile != null ? profile.userId : null;
    }

    // ✅ UPDATE SPECIFIC FIELDS (AFTER UPDATE PROFILE API)
    public static void updateProfile(Context context,
                                     String name,
                                     String ign,
                                     String gender,
                                     int age,
                                     String phoneNumber,
                                     String paymentUPI,
                                     String paymentMethod) {

        ProfileResponse.Data profile = getProfile(context);
        if (profile == null) return;

        profile.name = name;
        profile.ign = ign;
        profile.gender = gender;
        profile.age = age;
        profile.phoneNumber = phoneNumber;
        profile.paymentUPI = paymentUPI;
        profile.paymentMethod = paymentMethod;

        saveProfile(context, profile);
    }

    // ✅ CLEAR PROFILE (ON LOGOUT)
    public static void clear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    // ✅ GET LAST UPDATE TIME
    public static long getLastUpdated(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_UPDATED, 0);
    }
}