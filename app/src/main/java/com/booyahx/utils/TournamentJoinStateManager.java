package com.booyahx.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages tournament join states per user account
 * Preserves data across account switches - User A's joins remain saved
 * even when User B logs in, so switching back to User A shows correct state
 */
public class TournamentJoinStateManager {

    private static final String PREF_NAME = "tournament_join_states";
    private static final String KEY_CURRENT_USER = "current_user_id";
    private static final String KEY_JOINED_PREFIX = "joined_tournaments_";

    /**
     * Save that user joined a tournament
     */
    public static void markAsJoined(Context context, String userId, String tournamentId) {
        if (userId == null || tournamentId == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> joined = prefs.getStringSet(KEY_JOINED_PREFIX + userId, new HashSet<>());

        // Create mutable copy (SharedPreferences returns immutable set)
        Set<String> updated = new HashSet<>(joined);
        updated.add(tournamentId);

        prefs.edit()
                .putStringSet(KEY_JOINED_PREFIX + userId, updated)
                .apply();
    }

    /**
     * Check if user has joined a tournament
     */
    public static boolean hasJoined(Context context, String userId, String tournamentId) {
        if (userId == null || tournamentId == null) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> joined = prefs.getStringSet(KEY_JOINED_PREFIX + userId, new HashSet<>());

        return joined.contains(tournamentId);
    }

    /**
     * Get all tournaments joined by user
     */
    public static Set<String> getJoinedTournaments(Context context, String userId) {
        if (userId == null) return new HashSet<>();

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> joined = prefs.getStringSet(KEY_JOINED_PREFIX + userId, new HashSet<>());

        return new HashSet<>(joined);
    }

    /**
     * Track current user - call this when user logs in or app starts
     * This does NOT clear previous user's data
     */
    public static void setCurrentUser(Context context, String userId) {
        if (userId == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CURRENT_USER, userId)
                .apply();
    }

    /**
     * Get current tracked user ID
     */
    public static String getCurrentUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENT_USER, null);
    }

    /**
     * Check if user switched accounts
     * Returns true if the userId is different from last tracked user
     */
    public static boolean hasUserSwitched(Context context, String currentUserId) {
        String trackedUserId = getCurrentUserId(context);
        return currentUserId != null && !currentUserId.equals(trackedUserId);
    }

    /**
     * ONLY USE THIS ON COMPLETE APP DATA CLEAR / UNINSTALL CLEANUP
     * This removes ALL users' data
     */
    public static void clearAllData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Optional: Remove specific user's data (e.g., user requests account deletion)
     * Normal logout should NOT call this - data should persist
     */
    public static void removeUserData(Context context, String userId) {
        if (userId == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_JOINED_PREFIX + userId)
                .apply();
    }
}