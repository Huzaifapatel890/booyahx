package com.booyahx.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationManager {

    private static final String TAG        = "NotificationManager";
    private static final String PREFS_NAME = "notifications_prefs";
    // USER ISOLATION: key namespaced per userId so User B never sees User A's history
    private static final String KEY_PREFIX         = "notifications_list_";
    private static final int    MAX_NOTIFICATIONS  = 50;

    // ─── Write-level dedup ───────────────────────────────────────────────────
    // Catches duplicates before they are ever saved to disk.
    // Key   : "title|message"  fingerprint
    // Value : timestamp (ms) of the last successful addNotification() call
    private final HashMap<String, Long> writeSeenMap  = new HashMap<>();
    private static final long DEDUP_WINDOW_MS         = 5_000L; // 5-second window
    private static final int  DEDUP_MAP_MAX_SIZE      = 200;
    // ─────────────────────────────────────────────────────────────────────────

    private static NotificationManager instance;
    private final  SharedPreferences    prefs;
    private final  Gson                 gson;
    private        List<NotificationItem> notifications;
    private        String               scopedUserId;

    private NotificationManager(Context context) {
        prefs         = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson          = new Gson();
        notifications = new ArrayList<>();
    }

    public static synchronized NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager(context);
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * USER ISOLATION: Call this in DashboardActivity.onResume() after SocketManager.subscribe().
     * Saves old user's data then loads the new user's stored history.
     */
    public synchronized void switchUser(String userId) {
        if (userId == null || userId.isEmpty()) return;
        if (userId.equals(scopedUserId)) return;

        Log.d(TAG, "Switching scope from [" + scopedUserId + "] → [" + userId + "]");
        if (scopedUserId != null) saveNotifications();

        scopedUserId  = userId;
        notifications = loadFromDisk();
        writeSeenMap.clear(); // clear dedup map so old user's fingerprints don't bleed over
        Log.d(TAG, "Loaded " + notifications.size() + " notifications for user: " + userId);
    }

    /**
     * Call during logout (before clearing tokens).
     * Saves in-memory notifications and resets scope.
     */
    public synchronized void onLogout() {
        Log.d(TAG, "onLogout() — saving and resetting scope for user: " + scopedUserId);
        saveNotifications();
        notifications = new ArrayList<>();
        scopedUserId  = null;
        writeSeenMap.clear();
    }

    // ─── Write-level dedup helper ─────────────────────────────────────────────

    private static String dedupKey(NotificationItem item) {
        return item.getTitle() + "|" + item.getMessage();
    }

    /**
     * Returns true if this notification is a duplicate of one added within
     * DEDUP_WINDOW_MS. Side-effect: stamps the map if NOT a duplicate.
     * Evicts oldest entry when the map hits DEDUP_MAP_MAX_SIZE.
     */
    private boolean isDuplicateWrite(NotificationItem item) {
        String key  = dedupKey(item);
        long   now  = System.currentTimeMillis();
        Long   last = writeSeenMap.get(key);

        if (last != null && (now - last) < DEDUP_WINDOW_MS) {
            Log.d(TAG, "⚠️ Write-level duplicate blocked: " + key);
            return true;
        }

        // Evict oldest if at cap
        if (writeSeenMap.size() >= DEDUP_MAP_MAX_SIZE) {
            String oldestKey = null;
            long   oldestTs  = Long.MAX_VALUE;
            for (HashMap.Entry<String, Long> e : writeSeenMap.entrySet()) {
                if (e.getValue() < oldestTs) {
                    oldestTs  = e.getValue();
                    oldestKey = e.getKey();
                }
            }
            if (oldestKey != null) writeSeenMap.remove(oldestKey);
        }

        writeSeenMap.put(key, now);
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a notification only if it is not a duplicate within the dedup window.
     * Duplicates are silently dropped here before reaching disk or the UI list.
     */
    public void addNotification(NotificationItem notification) {
        if (isDuplicateWrite(notification)) return; // ← dedup gate

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

    /** Clears all in-memory notifications and persists the empty list. */
    public void clearAllNotifications() {
        notifications.clear();
        writeSeenMap.clear(); // reset dedup map so fresh notifs always show after a clear
        saveNotifications();
        Log.d(TAG, "Cleared all notifications");
    }

    public int getCount() {
        return notifications.size();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void saveNotifications() {
        String key = KEY_PREFIX + (scopedUserId != null ? scopedUserId : "guest");
        prefs.edit().putString(key, gson.toJson(notifications)).apply();
        Log.d(TAG, "Saved " + notifications.size() + " notifications [key=" + key + "]");
    }

    private List<NotificationItem> loadFromDisk() {
        String key  = KEY_PREFIX + (scopedUserId != null ? scopedUserId : "guest");
        String json = prefs.getString(key, null);
        if (json != null) {
            try {
                Type type = new TypeToken<List<NotificationItem>>() {}.getType();
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