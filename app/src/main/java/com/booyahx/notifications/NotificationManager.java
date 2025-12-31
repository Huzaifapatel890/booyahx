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
    private static final String KEY_NOTIFICATIONS = "notifications_list";
    private static final int MAX_NOTIFICATIONS = 50; // Keep last 50 notifications

    private static NotificationManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<NotificationItem> notifications;

    private NotificationManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        notifications = loadNotifications();
    }

    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }

    /**
     * Add a new notification
     */
    public void addNotification(NotificationItem notification) {
        Log.d(TAG, "Adding notification: " + notification.getTitle());

        // Add to beginning of list
        notifications.add(0, notification);

        // Keep only last MAX_NOTIFICATIONS
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications = notifications.subList(0, MAX_NOTIFICATIONS);
        }

        saveNotifications();
    }

    /**
     * Get all notifications
     */
    public List<NotificationItem> getAllNotifications() {
        return new ArrayList<>(notifications);
    }

    /**
     * Remove notification at position
     */
    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            saveNotifications();
            Log.d(TAG, "Removed notification at position: " + position);
        }
    }

    /**
     * Clear all notifications
     */
    public void clearAll() {
        notifications.clear();
        saveNotifications();
        Log.d(TAG, "Cleared all notifications");
    }

    /**
     * Get notification count
     */
    public int getCount() {
        return notifications.size();
    }

    /**
     * Save notifications to SharedPreferences
     */
    private void saveNotifications() {
        String json = gson.toJson(notifications);
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
        Log.d(TAG, "Saved " + notifications.size() + " notifications");
    }

    /**
     * Load notifications from SharedPreferences
     */
    private List<NotificationItem> loadNotifications() {
        String json = prefs.getString(KEY_NOTIFICATIONS, null);

        if (json != null) {
            try {
                Type type = new TypeToken<List<NotificationItem>>(){}.getType();
                List<NotificationItem> loaded = gson.fromJson(json, type);
                Log.d(TAG, "Loaded " + loaded.size() + " notifications");
                return loaded;
            } catch (Exception e) {
                Log.e(TAG, "Error loading notifications", e);
            }
        }

        Log.d(TAG, "No saved notifications found");
        return new ArrayList<>();
    }
}
