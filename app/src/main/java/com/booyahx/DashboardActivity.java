package com.booyahx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.WalletBalanceResponse;
import com.booyahx.notifications.NotificationItem;
import com.booyahx.notifications.NotificationManager;
import com.booyahx.notifications.NotificationType;
import com.booyahx.socket.SocketManager;
import com.booyahx.utils.NotificationPref;
import com.booyahx.utils.InAppNotificationBanner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private LinearLayout navHome, navParticipated, navWallet, navSettings;
    private TextView tvNavHome, tvNavParticipated, tvNavWallet, tvNavProfile;

    private Socket socket;
    private BroadcastReceiver broadcastReceiver;
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        Log.d(TAG, "Dashboard onCreate");

        // Initialize global API loader
        ApiClient.updateActivity(this);

        // Initialize views
        navHome = findViewById(R.id.navHome);
        navParticipated = findViewById(R.id.navParticipated);
        navWallet = findViewById(R.id.navWallet);
        navSettings = findViewById(R.id.NavSettings);

        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavParticipated = findViewById(R.id.tvNavParticipated);
        tvNavWallet = findViewById(R.id.tvNavWallet);
        tvNavProfile = findViewById(R.id.tvNavProfile);

        // ✅ FIX 3: Initialize socket with token (only once)
        String token = TokenManager.getAccessToken(this);
        if (token != null && SocketManager.getSocket() == null) {
            socket = SocketManager.getSocket(token);
            // Connect immediately after initialization
            SocketManager.connect();
            Log.d(TAG, "Socket initialized and connected");
        } else if (SocketManager.getSocket() != null) {
            socket = SocketManager.getSocket();
            Log.d(TAG, "Socket already initialized");
        }

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), 0, false);
            setActive(tvNavHome);
        }

        applyRoleLabel();

        // Setup navigation
        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment(), 0, true);
            setActive(tvNavHome);
        });

        navParticipated.setOnClickListener(v -> {
            String role = TokenManager.getRole(this);
            Fragment fragment;

            if ("host".equalsIgnoreCase(role)) {
                fragment = new HostTournamentFragment();
            } else {
                fragment = new ParticipatedFragment();
            }

            loadFragment(fragment, 1, true);
            setActive(tvNavParticipated);
        });

        navWallet.setOnClickListener(v -> {
            loadFragment(new WalletFragment(), 2, true);
            setActive(tvNavWallet);
        });

        navSettings.setOnClickListener(v -> {
            loadFragment(new SettingsFragment(), 3, true);
            setActive(tvNavProfile);
        });

        // Setup fragment result listener for role updates
        getSupportFragmentManager().setFragmentResultListener(
                "role_updated",
                this,
                (key, bundle) -> applyRoleLabel()
        );

        // ✅ FIX: Register receiver in onCreate so it stays alive even when user navigates
        // to NotificationActivity, EditProfile, etc. Previously in onResume/onPause which
        // caused receiver to die the moment any other screen opened.
        setupSocketBroadcastReceivers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        applyRoleLabel();
        ApiClient.updateActivity(this);

        // ✅ SUBSCRIBE TO SOCKET EVENTS
        String userId = TokenManager.getUserId(this);
        if (userId != null) {
            SocketManager.subscribe(userId);
            // ✅ USER ISOLATION: scope stored notifications to this user so User B
            // never sees User A's notification history from SharedPreferences
            NotificationManager.getInstance(this).switchUser(userId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ✅ FIX: Receiver no longer unregistered here — moved to onDestroy.
        // Unregistering in onPause killed the receiver every time any screen opened.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ FIX: Unregister only when Activity is fully destroyed
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
            Log.d(TAG, "✅ BroadcastReceiver unregistered in onDestroy");
        }
    }

    /**
     * Setup broadcast receivers for socket events
     */
    private void setupSocketBroadcastReceivers() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String targetUserId = intent.getStringExtra("targetUserId");
                String currentUserId = TokenManager.getUserId(context);
                if (targetUserId != null && !targetUserId.equals(currentUserId)) {
                    return; // drop — not for this user
                }

                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case "WALLET_UPDATED":
                        handleWalletUpdate(intent);
                        break;

                    case "TOURNAMENT_ROOM_UPDATED":
                        handleTournamentRoomUpdate(intent);
                        break;

                    case "TOURNAMENT_STATUS_UPDATED":
                        handleTournamentStatusUpdate(intent);
                        break;

                    case "PAYMENT_QR_UPDATED":
                        handlePaymentQRUpdate(intent);
                        break;

                    case "NOTIFICATION_PUSH": // ✅ GAP 3 FIX
                        handleNotificationPush(intent);
                        break;
                }
            }
        };

        // Register for all socket events
        IntentFilter filter = new IntentFilter();
        filter.addAction("WALLET_UPDATED");
        filter.addAction("TOURNAMENT_ROOM_UPDATED");
        filter.addAction("TOURNAMENT_STATUS_UPDATED");
        filter.addAction("PAYMENT_QR_UPDATED");
        filter.addAction("NOTIFICATION_PUSH"); // ✅ GAP 3 FIX

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
        Log.d(TAG, "✅ BroadcastReceiver registered in onCreate");
    }

    /**
     * Handle wallet update event
     * ✅ FIX: Now saves notification to NotificationManager so it persists for NotificationActivity
     */
    private void handleWalletUpdate(Intent intent) {
        String dataJson = intent.getStringExtra("data");

        runOnUiThread(() -> {
            // Notify fragments to refresh wallet balance
            getSupportFragmentManager().setFragmentResult(
                    "balance_updated",
                    new Bundle()
            );

            // ✅ FIX: Save notification to persistent store so NotificationActivity can show it
            try {
                NotificationManager nm = NotificationManager.getInstance(this);
                NotificationItem notification = null;

                if (dataJson != null) {
                    JSONObject data = new JSONObject(dataJson);

                    // ✅ FIX: balanceGC is nested inside "wallet" object, NOT at root level
                    double balanceGC = -1;
                    if (data.has("wallet") && !data.isNull("wallet")) {
                        balanceGC = data.getJSONObject("wallet").optDouble("balanceGC", -1);
                    }

                    // ✅ FIX: amountGC, status, type are nested inside "transaction" object
                    double txAmountGC = -1;
                    String txStatus   = "";
                    String txType     = "";
                    if (data.has("transaction") && !data.isNull("transaction")) {
                        JSONObject tx = data.getJSONObject("transaction");
                        txAmountGC = tx.optDouble("amountGC", -1);
                        txStatus   = tx.optString("status", "");
                        txType     = tx.optString("type", "");
                    }

                    if (txAmountGC >= 0 && !txStatus.isEmpty()) {
                        // ✅ Transaction available — build amount-aware message
                        String message = "";
                        NotificationType type = NotificationType.REWARD;

                        if ("success".equalsIgnoreCase(txStatus) || "approved".equalsIgnoreCase(txStatus)) {
                            if ("withdrawal".equalsIgnoreCase(txType)) {
                                message = (int) Math.round(txAmountGC) + " GC withdrawn from your wallet";
                            } else {
                                message = (int) Math.round(txAmountGC) + " GC added to your wallet";
                            }
                            type = NotificationType.REWARD;
                        } else if ("rejected".equalsIgnoreCase(txStatus) || "failed".equalsIgnoreCase(txStatus)) {
                            message = "Your transaction of " + (int) Math.round(txAmountGC) + " GC was rejected";
                            type = NotificationType.SYSTEM;
                        } else if ("pending".equalsIgnoreCase(txStatus)) {
                            message = "Your transaction of " + (int) Math.round(txAmountGC) + " GC is pending";
                            type = NotificationType.SYSTEM;
                        }

                        if (!message.isEmpty()) {
                            notification = new NotificationItem(
                                    "Transaction Update", message,
                                    System.currentTimeMillis(), type);
                        }

                    } else if (balanceGC >= 0) {
                        // ✅ Fallback: no transaction object, use balance
                        notification = new NotificationItem(
                                "Wallet Balance Updated",
                                "New balance: " + (int) Math.round(balanceGC) + " GC",
                                System.currentTimeMillis(),
                                NotificationType.REWARD
                        );
                    } else {
                        notification = new NotificationItem(
                                "Transaction History Updated",
                                "New transaction added to your wallet",
                                System.currentTimeMillis(),
                                NotificationType.REWARD
                        );
                    }
                }

                if (notification != null) {
                    nm.addNotification(notification);
                    NotificationPref.setUnread(this, true);
                    Log.d(TAG, "✅ Wallet notification saved to NotificationManager");
                    InAppNotificationBanner.show(DashboardActivity.this, notification.getTitle(), notification.getMessage());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error saving wallet notification", e);
            }

            // ✅ Silently refresh wallet balance from API and overwrite WalletCacheManager
            refreshWalletCache();
        });
    }
    /**
     * Handle tournament room update event.
     *
     * 🔥 NEW IMPLEMENTATION:
     *   - Old approach: sent a bare "joined_refresh" fragment result → triggered
     *     fetchJoinedTournaments() API call → room data came back from the server.
     *   - New approach: backend NOW sends room: { roomId, password } directly inside
     *     the "tournament:room-updated" WebSocket payload (no API round-trip needed).
     *     We parse the nested "room" object and push its data straight into a
     *     "tournament_room_updated" bundle → ParticipatedFragment patches only the
     *     matching tournament in its local list. Users who did not join that
     *     tournament are completely unaffected.
     *
     * Payload shape from backend:
     *   { tournamentId, type: 'room-updated', room: { roomId, password }, timestamp }
     */
    private void handleTournamentRoomUpdate(Intent intent) {
        String dataJson = intent.getStringExtra("data");

        runOnUiThread(() -> {
            try {
                String tournamentId   = "";
                String roomId         = "";
                String roomPassword   = "";
                String tournamentName = "Tournament";

                if (dataJson != null) {
                    JSONObject data = new JSONObject(dataJson);

                    // Top-level fields
                    tournamentId   = data.optString("tournamentId", "");
                    tournamentName = data.optString("tournamentName", "Tournament");

                    // 🔥 NEW: roomId & password now live inside the nested "room" object
                    //    per the updated WebSocket payload spec.
                    if (data.has("room") && !data.isNull("room")) {
                        JSONObject room = data.getJSONObject("room");
                        roomId       = room.optString("roomId",   "");
                        roomPassword = room.optString("password", "");
                        Log.d(TAG, "🔥 Parsed room from nested object → roomId=" + roomId
                                + " password=" + roomPassword);
                    } else {
                        // ⚠️ Fallback for any legacy payload shape — keeps compatibility
                        roomId       = data.optString("roomId",       "");
                        roomPassword = data.optString("roomPassword", "");
                        Log.w(TAG, "⚠️ Fallback: room object not found in payload, reading flat keys");
                    }

                    Log.d(TAG, "📨 handleTournamentRoomUpdate:"
                            + " tournamentId=" + tournamentId
                            + " roomId=" + roomId
                            + " password=" + roomPassword);
                }

                // 🔥 NEW: Push roomId + password directly to ParticipatedFragment via bundle.
                // The fragment will patch ONLY the matching tournament in allTournaments
                // (which already contains only tournaments the current user has joined),
                // so no other user's data is affected. No API call is made.
                Bundle roomBundle = new Bundle();
                roomBundle.putString("tournament_id",   tournamentId);
                roomBundle.putString("room_id",         roomId);
                roomBundle.putString("room_password",   roomPassword);

                getSupportFragmentManager().setFragmentResult(
                        "tournament_room_updated",
                        roomBundle
                );

                Log.d(TAG, "✅ 'tournament_room_updated' fragment result sent"
                        + " | tournamentId=" + tournamentId
                        + " | roomId=" + roomId);

                // ✅ FIX: Save notification to persistent store so NotificationActivity can show it
                NotificationManager nm = NotificationManager.getInstance(this);

                NotificationItem notification = new NotificationItem(
                        "Room ID & Password Updated",
                        tournamentName + " credentials updated. ID: " + roomId + " Pass: " + roomPassword,
                        System.currentTimeMillis(),
                        NotificationType.ROOM_UPDATE
                );

                nm.addNotification(notification);
                NotificationPref.setUnread(this, true);
                Log.d(TAG, "✅ Room update notification saved to NotificationManager");
                InAppNotificationBanner.show(DashboardActivity.this, notification.getTitle(), notification.getMessage());

            } catch (Exception e) {
                Log.e(TAG, "Error handling tournament room update", e);
            }
        });
    }

    /**
     * Handle tournament status update event
     * ✅ Broadcasts to: HomeFragment, HostTournamentFragment, ParticipatedFragment
     * ✅ FIX: Now saves notification to NotificationManager so it persists for NotificationActivity
     */
    private void handleTournamentStatusUpdate(Intent intent) {
        String dataJson = intent.getStringExtra("data");

        runOnUiThread(() -> {
            try {
                Bundle bundle = new Bundle();

                if (dataJson != null) {
                    JSONObject data = new JSONObject(dataJson);
                    bundle.putString("tournament_id", data.optString("tournamentId"));
                    bundle.putString("new_status", data.optString("status"));

                    // ✅ GAP 2 FIX: backend dev said if tournament:status-updated payload
                    // contains a "room" object, treat it the same as tournament:room-updated
                    // and push the room credentials to ParticipatedFragment immediately.
                    // Previously this data was silently dropped.
                    if (data.has("room") && !data.isNull("room")) {
                        try {
                            JSONObject room = data.getJSONObject("room");
                            String roomId       = room.optString("roomId",   "");
                            String roomPassword = room.optString("password", "");
                            String tId          = data.optString("tournamentId", "");
                            if (!tId.isEmpty()) {
                                Bundle roomBundle = new Bundle();
                                roomBundle.putString("tournament_id",   tId);
                                roomBundle.putString("room_id",         roomId);
                                roomBundle.putString("room_password",   roomPassword);
                                getSupportFragmentManager().setFragmentResult(
                                        "tournament_room_updated", roomBundle);
                                Log.d(TAG, "✅ GAP 2: room data inside status payload forwarded"
                                        + " → tournamentId=" + tId
                                        + " roomId=" + roomId);
                            }
                        } catch (Exception roomEx) {
                            Log.e(TAG, "Error extracting room from status payload", roomEx);
                        }
                    }
                }

                // ✅ Broadcast to HomeFragment
                getSupportFragmentManager().setFragmentResult(
                        "tournament_status_changed",
                        bundle
                );

                // ✅ Broadcast to HostTournamentFragment
                getSupportFragmentManager().setFragmentResult(
                        "host_tournament_status_changed",
                        bundle
                );

                // NOTE: "joined_refresh" is intentionally NOT sent here.
                // ParticipatedFragment already listens to "tournament_status_changed" above
                // and calls fetchJoinedTournaments() from that listener.
                // Sending "joined_refresh" here too was causing a duplicate API call
                // (confirmed via logs: two back-to-back hits to /api/tournament/joined).

                Log.d(TAG, "Tournament status update broadcasted to all fragments");

                // ✅ FIX: Save notification to persistent store so NotificationActivity can show it
                if (dataJson != null) {
                    JSONObject data = new JSONObject(dataJson);
                    String tournamentName = data.optString("tournamentName", "Tournament");
                    String status = data.optString("status", "");

                    String message = "";
                    NotificationType type = NotificationType.SYSTEM;

                    if ("live".equalsIgnoreCase(status)) {
                        message = tournamentName + " is now live!";
                        type = NotificationType.TOURNAMENT;
                    } else if ("completed".equalsIgnoreCase(status)) {
                        message = tournamentName + " has ended.";
                        type = NotificationType.VICTORY;
                    } else if ("cancelled".equalsIgnoreCase(status)) {
                        message = tournamentName + " has been cancelled.";
                        type = NotificationType.SYSTEM;
                    } else if ("pendingResult".equalsIgnoreCase(status)) {
                        message = tournamentName + " is awaiting results.";
                        type = NotificationType.TOURNAMENT;
                    }

                    if (!message.isEmpty()) {
                        NotificationManager nm = NotificationManager.getInstance(this);
                        nm.addNotification(new NotificationItem(
                                "Tournament Status Update",
                                message,
                                System.currentTimeMillis(),
                                type
                        ));
                        NotificationPref.setUnread(this, true);
                        Log.d(TAG, "✅ Tournament status notification saved to NotificationManager");
                        InAppNotificationBanner.show(DashboardActivity.this, "Tournament Status Update", message);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing tournament status update", e);
            }
        });
    }

    /**
     * ✅ GAP 3 FIX: Handle notification:push events from the server.
     *
     * Backend payload shape:
     *   { type, title, message, body?, timestamp, tournamentId? }
     *
     * Types include: "room-updated", "new-lobby-created", "lobby-filling",
     *                "admin-notification", etc.
     *
     * Shows an in-app banner and persists to NotificationManager so the
     * NotificationActivity bell list also shows these server-pushed alerts.
     */
    private void handleNotificationPush(Intent intent) {
        String dataJson = intent.getStringExtra("data");
        runOnUiThread(() -> {
            try {
                if (dataJson == null) {
                    Log.w(TAG, "handleNotificationPush: dataJson is null, skipping");
                    return;
                }

                JSONObject data   = new JSONObject(dataJson);
                String type       = data.optString("type",    "");
                String title      = data.optString("title",   "Notification");
                String message    = data.optString("message", "");

                // Fallback: some push shapes use "body" instead of "message"
                if (message.isEmpty()) {
                    message = data.optString("body", "");
                }
                if (message.isEmpty()) {
                    Log.w(TAG, "handleNotificationPush: empty message, skipping");
                    return;
                }

                // Map push type → notification icon/colour in NotificationActivity
                NotificationType notifType = NotificationType.SYSTEM;
                if ("room-updated".equalsIgnoreCase(type)) {
                    notifType = NotificationType.ROOM_UPDATE;
                } else if ("new-lobby-created".equalsIgnoreCase(type)
                        || "lobby-filling".equalsIgnoreCase(type)) {
                    notifType = NotificationType.TOURNAMENT;
                }

                NotificationManager nm = NotificationManager.getInstance(this);
                nm.addNotification(new NotificationItem(
                        title,
                        message,
                        System.currentTimeMillis(),
                        notifType
                ));
                NotificationPref.setUnread(this, true);
                InAppNotificationBanner.show(DashboardActivity.this, title, message);
                Log.d(TAG, "✅ notification:push handled — type=" + type + " title=" + title);

            } catch (Exception e) {
                Log.e(TAG, "Error handling notification:push", e);
            }
        });
    }

    /**
     * Handle payment QR status update event
     * ✅ FIX: Now saves notification to NotificationManager so it persists for NotificationActivity
     */
    private void handlePaymentQRUpdate(Intent intent) {
        String dataJson = intent.getStringExtra("data");

        runOnUiThread(() -> {
            Log.d(TAG, "Payment QR status updated: " + dataJson);

            // ✅ FIX: Save notification to persistent store so NotificationActivity can show it
            try {
                if (dataJson != null) {
                    JSONObject data = new JSONObject(dataJson);
                    String status = data.optString("status", "");
                    String qrAction = data.optString("action", "");
                    double amountGC = data.optDouble("amountGC", 0);

                    String message = "";
                    NotificationType type = NotificationType.REWARD;

                    if ("success".equalsIgnoreCase(status) || "approved".equalsIgnoreCase(qrAction)) {
                        message = "Payment successful! " + (int) Math.round(amountGC) + " GC credited to your wallet";
                        type = NotificationType.REWARD;
                    } else if ("pending".equalsIgnoreCase(status)) {
                        message = "Payment is being processed";
                        type = NotificationType.SYSTEM;
                    } else if ("rejected".equalsIgnoreCase(qrAction)) {
                        message = "Payment was rejected";
                        type = NotificationType.SYSTEM;
                    } else if ("failed".equalsIgnoreCase(status)) {
                        message = "Payment failed. Please try again";
                        type = NotificationType.SYSTEM;
                    }

                    if (!message.isEmpty()) {
                        NotificationManager nm = NotificationManager.getInstance(this);
                        nm.addNotification(new NotificationItem(
                                "Payment Status Update",
                                message,
                                System.currentTimeMillis(),
                                type
                        ));
                        NotificationPref.setUnread(this, true);
                        Log.d(TAG, "✅ Payment notification saved to NotificationManager");
                        InAppNotificationBanner.show(DashboardActivity.this, "Payment Status Update", message);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving payment notification", e);
            }

            // ✅ Silently refresh wallet balance from API and overwrite WalletCacheManager
            refreshWalletCache();
        });
    }

    private void applyRoleLabel() {
        String role = TokenManager.getRole(this);
        if ("host".equalsIgnoreCase(role)) {
            tvNavParticipated.setText("Host Panel");
        } else {
            tvNavParticipated.setText("Joined");
        }
    }

    private void loadFragment(Fragment fragment, int newIndex, boolean animate) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (animate) {
            if (newIndex > currentIndex) {
                ft.setCustomAnimations(R.anim.slide_in_right, 0);
            } else if (newIndex < currentIndex) {
                ft.setCustomAnimations(R.anim.slide_in_left, 0);
            }
        }

        currentIndex = newIndex;
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }

    private void setActive(TextView active) {
        reset(tvNavHome);
        reset(tvNavParticipated);
        reset(tvNavWallet);
        reset(tvNavProfile);
        active.setTextColor(0xFF00C3FF);
    }

    private void reset(TextView tv) {
        tv.setTextColor(0xFFAAAAAA);
    }

    /**
     * Silently calls the wallet balance API in the background and overwrites WalletCacheManager.
     * Called on any wallet-related socket emit (WALLET_UPDATED, PAYMENT_QR_UPDATED).
     * No UI shown — purely a background cache refresh.
     */
    private void refreshWalletCache() {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(Call<WalletBalanceResponse> call, Response<WalletBalanceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {
                    double newBalance = response.body().data.balanceGC;
                    WalletCacheManager.saveBalance(DashboardActivity.this, newBalance);
                    Log.d(TAG, "✅ WalletCacheManager updated silently: " + newBalance + " GC");
                } else {
                    Log.w(TAG, "⚠️ refreshWalletCache: unsuccessful response");
                }
            }

            @Override
            public void onFailure(Call<WalletBalanceResponse> call, Throwable t) {
                Log.e(TAG, "❌ refreshWalletCache failed: " + t.getMessage());
            }
        });
    }

    public void showTopRightToast(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(40, 25, 40, 25);
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackgroundResource(R.drawable.toast_bg);
        tv.setTextSize(14);

        Toast toast = new Toast(getApplicationContext());
        toast.setView(tv);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.END, 40, 120);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(350);
        tv.startAnimation(fade);
        toast.show();
    }
}