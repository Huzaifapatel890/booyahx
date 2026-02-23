package com.booyahx;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.FcmTokenRequest;
import com.booyahx.network.models.SimpleResponse;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles all incoming FCM push notifications for BooyahX.
 *
 * Supported types (read from data payload "type" field):
 *   - room-updated          → Show room ID + password to joined users
 *   - tournament-cancelled  → Tournament cancelled alert
 *   - tournament-started    → Tournament started alert
 *   - new-lobby-created     → New lobby available
 *   - lobby-filling         → Lobby filling up warning
 *   - admin-notification    → Custom admin message
 *   - special_tournament    → Special tournament alert
 *
 * Also handles onNewToken: re-saves refreshed FCM token to backend.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_DEBUG";

    // Notification channels
    private static final String CHANNEL_ROOM     = "room_updates";
    private static final String CHANNEL_TOURNAMENT = "tournament_alerts";
    private static final String CHANNEL_GENERAL  = "general";

    // Auto-incrementing ID so notifications don't overwrite each other
    private static final AtomicInteger notifIdCounter = new AtomicInteger(1000);

    // =========================================================
    // INCOMING MESSAGE
    // =========================================================

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "============================================");
        Log.d(TAG, "📨 FCM message received");
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Data payload: " + data.toString());

        // Always prefer data payload for type-based routing
        if (!data.isEmpty()) {
            String type = data.get("type");
            Log.d(TAG, "Type: " + type);
            handleByType(type, data, remoteMessage);
        } else if (remoteMessage.getNotification() != null) {
            // Fallback: plain notification message with no data
            RemoteMessage.Notification notif = remoteMessage.getNotification();
            String title = notif.getTitle() != null ? notif.getTitle() : "BooyahX";
            String body  = notif.getBody()  != null ? notif.getBody()  : "";
            Log.d(TAG, "Plain notification → title=" + title + " body=" + body);
            showNotification(CHANNEL_GENERAL, title, body, null);
        }

        Log.d(TAG, "============================================");
    }

    // =========================================================
    // ROUTE BY TYPE
    // =========================================================

    private void handleByType(String type, Map<String, String> data, RemoteMessage remoteMessage) {
        if (type == null) {
            // No type field — fall back to notification payload if present
            if (remoteMessage.getNotification() != null) {
                RemoteMessage.Notification n = remoteMessage.getNotification();
                showNotification(CHANNEL_GENERAL,
                        n.getTitle() != null ? n.getTitle() : "BooyahX",
                        n.getBody()  != null ? n.getBody()  : "",
                        null);
            }
            return;
        }

        switch (type) {

            // --------------------------------------------------
            // ROOM UPDATED — show roomId + password
            // Sent to joined users only (backend filters by topic)
            // --------------------------------------------------
            case "room-updated": {
                String slotName   = getOrDefault(data, "tournamentName", "Your Slot");
                String roomId     = getOrDefault(data, "roomId",         "");
                String password   = getOrDefault(data, "password",       "");

                // Also try nested-key fallbacks backend might use
                if (roomId.isEmpty())   roomId   = getOrDefault(data, "room_id",       "");
                if (password.isEmpty()) password = getOrDefault(data, "roomPassword",  "");

                String title = "🔑 Your Room is Live!";
                String body  = slotName + "\nRoom ID: " + roomId + "  •  Pass: " + password;

                Log.d(TAG, "room-updated → roomId=" + roomId + " password=" + password);
                showNotification(CHANNEL_ROOM, title, body, openDashboard("participated"));
                break;
            }

            // --------------------------------------------------
            // TOURNAMENT CANCELLED
            // --------------------------------------------------
            case "tournament-cancelled": {
                String slotName = getOrDefault(data, "tournamentName", "your slot");
                String reason   = getOrDefault(data, "reason", "");

                String title = "❌ Slot Cancelled";
                String body  = "Unfortunately your slot in " + slotName + " has been cancelled." +
                        (reason.isEmpty() ? "" : "\nReason: " + reason);

                Log.d(TAG, "tournament-cancelled → " + slotName);
                showNotification(CHANNEL_TOURNAMENT, title, body, openDashboard("participated"));
                break;
            }

            // --------------------------------------------------
            // TOURNAMENT STARTED
            // --------------------------------------------------
            case "tournament-started": {
                String slotName  = getOrDefault(data, "tournamentName", "Your slot");
                String startTime = getOrDefault(data, "startTime", "");

                String title = "🔥 Your Slot is Live — Let's Go!";
                String body  = slotName + " has kicked off! Get in the room NOW!" +
                        (startTime.isEmpty() ? "" : " Started at " + startTime);

                Log.d(TAG, "tournament-started → " + slotName);
                showNotification(CHANNEL_TOURNAMENT, title, body, openDashboard("participated"));
                break;
            }

            // --------------------------------------------------
            // NEW LOBBY CREATED
            // --------------------------------------------------
            case "new-lobby-created": {
                String lobbyName  = getOrDefault(data, "lobbyName",  "New Slot");
                String mode       = getOrDefault(data, "mode",       "");
                String subMode    = getOrDefault(data, "subMode",    "");
                String entryFee   = getOrDefault(data, "entryFee",   "");
                String startTime  = getOrDefault(data, "startTime",  "");

                String title = "🆕 New Slots Published!";
                StringBuilder body = new StringBuilder("Grab your slot now — " + lobbyName);
                if (!mode.isEmpty())      body.append(" • ").append(mode);
                if (!subMode.isEmpty())   body.append(" ").append(subMode);
                if (!entryFee.isEmpty())  body.append("\nEntry: ").append(entryFee).append(" GC");
                if (!startTime.isEmpty()) body.append(" • ").append(startTime);

                Log.d(TAG, "new-lobby-created → " + lobbyName);
                showNotification(CHANNEL_GENERAL, title, body.toString(), openDashboard("home"));
                break;
            }

            // --------------------------------------------------
            // LOBBY FILLING
            // --------------------------------------------------
            case "lobby-filling": {
                String lobbyName  = getOrDefault(data, "lobbyName",  "A slot");
                String spotsLeft  = getOrDefault(data, "spotsLeft",  "");
                String totalSpots = getOrDefault(data, "totalSpots", "");

                String title = "⚡ Slots Filling Fast!";
                String body  = lobbyName + " is almost full — don't miss your shot!" +
                        (spotsLeft.isEmpty() ? "" : " Only " + spotsLeft + " slots left!");

                Log.d(TAG, "lobby-filling → " + lobbyName + " spotsLeft=" + spotsLeft);
                showNotification(CHANNEL_GENERAL, title, body, openDashboard("home"));
                break;
            }

            // --------------------------------------------------
            // ADMIN CUSTOM NOTIFICATION
            // --------------------------------------------------
            case "admin-notification": {
                String title   = getOrDefault(data, "title",   "BooyahX Admin");
                String message = getOrDefault(data, "message", "You have a new notification");

                Log.d(TAG, "admin-notification → " + title);
                showNotification(CHANNEL_GENERAL, title, message, openDashboard("home"));
                break;
            }

            // --------------------------------------------------
            // SPECIAL TOURNAMENT
            // --------------------------------------------------
            case "special_tournament": {
                String slotName  = getOrDefault(data, "tournamentName", "Special Slot");
                String details   = getOrDefault(data, "details",        "");
                String prizePool = getOrDefault(data, "prizePool",      "");

                String title = "🏆 Special Slots Drop!";
                StringBuilder body = new StringBuilder("Don't miss it — " + slotName);
                if (!details.isEmpty())   body.append("\n").append(details);
                if (!prizePool.isEmpty()) body.append("\nPrize Pool: ").append(prizePool).append(" GC");

                Log.d(TAG, "special_tournament → " + slotName);
                showNotification(CHANNEL_GENERAL, title, body.toString(), openDashboard("home"));
                break;
            }

            default:
                Log.w(TAG, "⚠️ Unknown FCM type: " + type + " — showing generic notification");
                // Try notification payload as fallback
                if (remoteMessage.getNotification() != null) {
                    RemoteMessage.Notification n = remoteMessage.getNotification();
                    showNotification(CHANNEL_GENERAL,
                            n.getTitle() != null ? n.getTitle() : "BooyahX",
                            n.getBody()  != null ? n.getBody()  : "",
                            null);
                }
                break;
        }
    }

    // =========================================================
    // TOKEN REFRESH — re-save to backend when Firebase rotates token
    // =========================================================

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "============================================");
        Log.d(TAG, "🔄 FCM token refreshed");
        Log.d(TAG, "New token (first 20): " + token.substring(0, Math.min(20, token.length())) + "...");
        Log.d(TAG, "============================================");

        // Only re-save if user is logged in (has access token)
        String accessToken = TokenManager.getAccessToken(getApplicationContext());
        if (accessToken == null || accessToken.isEmpty()) {
            Log.d(TAG, "User not logged in — skipping token re-save");
            return;
        }

        ApiService api = ApiClient.getClient(getApplicationContext()).create(ApiService.class);
        api.saveFcmToken(new FcmTokenRequest(token))
                .enqueue(new Callback<SimpleResponse>() {
                    @Override
                    public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ Refreshed FCM token saved to backend");
                        } else {
                            Log.w(TAG, "⚠️ Refreshed token save failed: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SimpleResponse> call, Throwable t) {
                        Log.e(TAG, "❌ Refreshed token save network error: " + t.getMessage());
                    }
                });
    }

    // =========================================================
    // SHOW NOTIFICATION
    // =========================================================

    private void showNotification(String channelId, String title, String body, PendingIntent pendingIntent) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        // Create channel (required for Android O+, safe to call repeatedly)
        createChannelIfNeeded(manager, channelId);

        if (pendingIntent == null) {
            pendingIntent = openDashboard("home");
        }

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification_image)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        int notifId = notifIdCounter.getAndIncrement();
        manager.notify(notifId, builder.build());

        Log.d(TAG, "✅ Notification shown → id=" + notifId + " title=" + title);
    }

    // =========================================================
    // CREATE NOTIFICATION CHANNELS
    // =========================================================

    private void createChannelIfNeeded(NotificationManager manager, String channelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        if (manager.getNotificationChannel(channelId) != null) return;

        String name;
        String description;
        int importance = NotificationManager.IMPORTANCE_HIGH;

        switch (channelId) {
            case CHANNEL_ROOM:
                name = "Room Updates";
                description = "Room ID and password notifications for joined tournaments";
                break;
            case CHANNEL_TOURNAMENT:
                name = "Tournament Alerts";
                description = "Tournament started, cancelled and status notifications";
                break;
            default:
                name = "General";
                description = "General app notifications";
                break;
        }

        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setDescription(description);
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);

        Log.d(TAG, "✅ Notification channel created: " + channelId);
    }

    // =========================================================
    // PENDING INTENT — tap notification → open DashboardActivity
    // =========================================================

    private PendingIntent openDashboard(String tab) {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("nav_tab", tab); // DashboardActivity can read this to switch tab if needed

        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(this, notifIdCounter.get(), intent, flags);
    }

    // =========================================================
    // UTILS
    // =========================================================

    private String getOrDefault(Map<String, String> data, String key, String defaultValue) {
        String val = data.get(key);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }
}