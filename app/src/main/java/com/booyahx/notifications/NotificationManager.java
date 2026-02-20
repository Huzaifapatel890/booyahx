package com.booyahx.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {

    private static final String TAG = "NotificationManager";
    private static final String PREFS_NAME = "notifications_prefs";
    // ✅ USER ISOLATION: key is namespaced per userId so User B never sees User A's history
    private static final String KEY_PREFIX = "notifications_list_";
    private static final int MAX_NOTIFICATIONS = 50;

    private static NotificationManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<NotificationItem> notifications;
    private String scopedUserId;

    private NotificationManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        notifications = new ArrayList<>();
    }

    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }

    /**
     * ✅ USER ISOLATION: Call this in DashboardActivity.onResume() after SocketManager.subscribe().
     * Saves old user's in-memory notifications, then loads the new user's stored history.
     * Guarantees User B never sees User A's notification history.
     */
    public synchronized void switchUser(String userId) {
        if (userId == null || userId.isEmpty()) return;
        if (userId.equals(scopedUserId)) return;

        Log.d(TAG, "Switching notification scope from [" + scopedUserId + "] to [" + userId + "]");
        if (scopedUserId != null) {
            saveNotifications(); // persist old user's data before switching
        }
        scopedUserId = userId;
        notifications = loadNotifications();
        Log.d(TAG, "Loaded " + notifications.size() + " notifications for user: " + userId);
    }

    /**
     * ✅ Call this during logout (before clearing tokens).
     * Saves the current user's notifications to disk, then resets the in-memory scope
     * so the next switchUser() call will correctly reload from SharedPreferences on login.
     */
    public synchronized void onLogout() {
        Log.d(TAG, "onLogout() — saving and resetting scope for user: " + scopedUserId);
        saveNotifications(); // persist anything still in memory
        notifications = new ArrayList<>(); // clear in-memory list
        scopedUserId = null; // reset scope so switchUser() reloads on next login
    }

    public void addNotification(NotificationItem notification) {
        Log.d(TAG, "Adding notification: " + notification.getTitle());
        notifications.add(0, notification);
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications = notifications.subList(0, MAX_NOTIFICATIONS);
        }
        saveNotifications();
    }

    public List<NotificationItem> getAllNotifications() {
        return new ArrayList<>(notifications);
    }

    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            saveNotifications();
            Log.d(TAG, "Removed notification at position: " + position);
        }
    }

    public void clearAll() {
        notifications.clear();
        saveNotifications();
        Log.d(TAG, "Cleared all notifications");
    }

    public int getCount() {
        return notifications.size();
    }

    private void saveNotifications() {
        String key = KEY_PREFIX + (scopedUserId != null ? scopedUserId : "guest");
        prefs.edit().putString(key, gson.toJson(notifications)).apply();
        Log.d(TAG, "Saved " + notifications.size() + " notifications [key=" + key + "]");
    }

    private List<NotificationItem> loadNotifications() {
        String key = KEY_PREFIX + (scopedUserId != null ? scopedUserId : "guest");
        String json = prefs.getString(key, null);
        if (json != null) {
            try {
                Type type = new TypeToken<List<NotificationItem>>(){}.getType();
                List<NotificationItem> loaded = gson.fromJson(json, type);
                Log.d(TAG, "Loaded " + loaded.size() + " notifications [key=" + key + "]");
                return loaded;
            } catch (Exception e) {
                Log.e(TAG, "Error loading notifications", e);
            }
        }
        Log.d(TAG, "No saved notifications [key=" + key + "]");
        return new ArrayList<>();
    }
}