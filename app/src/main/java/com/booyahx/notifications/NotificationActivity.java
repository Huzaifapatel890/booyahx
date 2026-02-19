package com.booyahx.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;

import org.json.JSONObject;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ImageView backButton;
    private TextView emptyText;
    private NotificationManager notificationManager;
    private static final String TAG = "NotificationActivity";

    // ✅ FIX: BroadcastReceiver replaces direct socket.on() listeners in this Activity.
    // SocketManager already listens to the socket globally and broadcasts these actions.
    // Having direct socket.on() here AND in SocketManager caused duplicate/conflicting listeners.
    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String rawData = intent.getStringExtra("data");
            Log.d(TAG, "📨 Broadcast received: " + action);

            try {
                JSONObject data = (rawData != null) ? new JSONObject(rawData) : new JSONObject();

                // ✅ TOURNAMENT ROOM UPDATED — same logic as before
                if ("TOURNAMENT_ROOM_UPDATED".equals(action)) {
                    Log.d(TAG, "🔥 Room update notification received");

                    String tournamentName = data.optString("tournamentName", "Tournament");
                    String roomId = data.optString("roomId", "");
                    String roomPassword = data.optString("roomPassword", "");

                    NotificationItem notification = new NotificationItem(
                            "Room ID & Password Updated",
                            tournamentName + " credentials updated. ID: " + roomId + " Pass: " + roomPassword,
                            System.currentTimeMillis(),
                            NotificationType.ROOM_UPDATE
                    );

                    addNewNotification(notification);

                    // ✅ TOURNAMENT STATUS UPDATED — same logic as before
                } else if ("TOURNAMENT_STATUS_UPDATED".equals(action)) {
                    Log.d(TAG, "🔥 Status update notification received");

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
                        NotificationItem notification = new NotificationItem(
                                "Tournament Status Update",
                                message,
                                System.currentTimeMillis(),
                                type
                        );
                        addNewNotification(notification);
                    }

                    // ✅ WALLET UPDATED — covers wallet:balance-updated, wallet:transaction-updated,
                    //    wallet:history-updated (all broadcast as WALLET_UPDATED by SocketManager)
                } else if ("WALLET_UPDATED".equals(action)) {
                    double balanceGC = data.optDouble("balanceGC", -1);
                    String txAction = data.optString("action", "");

                    if (balanceGC >= 0) {
                        // wallet:balance-updated
                        Log.d(TAG, "🔥 Wallet balance update notification received");

                        double updatedAt = data.optDouble("updatedAt", 0);

                        NotificationItem notification = new NotificationItem(
                                "Wallet Balance Updated",
                                "Your new balance is " + (int) Math.round(balanceGC) + " GC",
                                System.currentTimeMillis(),
                                NotificationType.REWARD
                        );
                        addNewNotification(notification);

                    } else if (!txAction.isEmpty()) {
                        // wallet:transaction-updated
                        Log.d(TAG, "🔥 Wallet transaction update notification received");

                        String message = "";
                        NotificationType type = NotificationType.REWARD;

                        if ("approved".equalsIgnoreCase(txAction)) {
                            message = "Your transaction has been approved";
                        } else if ("rejected".equalsIgnoreCase(txAction)) {
                            message = "Your transaction has been rejected";
                            type = NotificationType.SYSTEM;
                        } else if ("updated".equalsIgnoreCase(txAction)) {
                            message = "Your transaction has been updated";
                        }

                        if (!message.isEmpty()) {
                            NotificationItem notification = new NotificationItem(
                                    "Transaction Update",
                                    message,
                                    System.currentTimeMillis(),
                                    type
                            );
                            addNewNotification(notification);
                        }

                    } else {
                        // wallet:history-updated
                        Log.d(TAG, "🔥 Wallet history update notification received");

                        NotificationItem notification = new NotificationItem(
                                "Transaction History Updated",
                                "New transaction added to your wallet",
                                System.currentTimeMillis(),
                                NotificationType.REWARD
                        );
                        addNewNotification(notification);
                    }

                    // ✅ PAYMENT QR UPDATED — same logic as before
                } else if ("PAYMENT_QR_UPDATED".equals(action)) {
                    Log.d(TAG, "🔥 QR payment status update notification received");

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
                        NotificationItem notification = new NotificationItem(
                                "Payment Status Update",
                                message,
                                System.currentTimeMillis(),
                                type
                        );
                        addNewNotification(notification);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error handling broadcast: " + action, e);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        emptyText = findViewById(R.id.emptyNotificationsText);

        backButton.setOnClickListener(v -> finish());

        notificationManager = NotificationManager.getInstance(this);

        setupRecyclerView();
        loadNotifications();
        // ✅ FIX: setupSocket() removed from here.
        // SocketManager handles all socket events globally and broadcasts them via LocalBroadcast.
        // This Activity just listens to those broadcasts via notificationReceiver (registered in onStart).
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(
                notificationManager.getAllNotifications(),
                this::removeNotification
        );
        recyclerView.setAdapter(adapter);
        updateEmptyState();
    }

    // ✅ FIX: Register BroadcastReceiver when Activity is visible (onStart/onStop lifecycle)
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("TOURNAMENT_ROOM_UPDATED");
        filter.addAction("TOURNAMENT_STATUS_UPDATED");
        filter.addAction("WALLET_UPDATED");
        filter.addAction("PAYMENT_QR_UPDATED");
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter);
        Log.d(TAG, "✅ BroadcastReceiver registered");
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        Log.d(TAG, "✅ BroadcastReceiver unregistered");
    }

    private void addNewNotification(NotificationItem notification) {
        notificationManager.addNotification(notification);
        adapter.updateNotifications(notificationManager.getAllNotifications());
        recyclerView.smoothScrollToPosition(0);
        updateEmptyState();
    }

    private void loadNotifications() {
        adapter.updateNotifications(notificationManager.getAllNotifications());
        updateEmptyState();
    }

    private void removeNotification(int position) {
        notificationManager.removeNotification(position);
        adapter.updateNotifications(notificationManager.getAllNotifications());
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (notificationManager.getCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ FIX: No socket cleanup needed here anymore.
        // SocketManager owns the socket lifecycle — unsubscribing here was also
        // accidentally killing the global socket listeners for the whole app.
        Log.d(TAG, "✅ NotificationActivity destroyed");
    }
}