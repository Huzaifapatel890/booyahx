package com.booyahx.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;
import com.booyahx.TokenManager;
import com.booyahx.socket.SocketManager;

import org.json.JSONObject;

import io.socket.client.Socket;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ImageView backButton;
    private TextView emptyText;
    private Socket socket;
    private NotificationManager notificationManager;
    private static final String TAG = "NotificationActivity";

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
        setupSocket();
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

    private void setupSocket() {
        String token = TokenManager.getAccessToken(this);
        socket = SocketManager.getSocket(token);

        if (!socket.connected()) {
            SocketManager.connect();
        }

        String userId = TokenManager.getUserId(this);

        // 🔥 SUBSCRIBE TO USER TOURNAMENTS ON CONNECT
        if (socket.connected() && userId != null) {
            socket.emit("subscribe:user-tournaments", userId);
            socket.emit("subscribe:wallet", userId);
            Log.d(TAG, "✅ Subscribed to user tournaments and wallet");
        }

        // 🔥 RE-SUBSCRIBE ON RECONNECT
        socket.on(Socket.EVENT_CONNECT, args -> {
            String uid = TokenManager.getUserId(this);
            if (uid != null) {
                socket.emit("subscribe:user-tournaments", uid);
                socket.emit("subscribe:wallet", uid);
                Log.d(TAG, "✅ Re-subscribed on reconnect");
            }
        });

        // 🔥 LISTEN FOR TOURNAMENT ROOM UPDATES
        socket.on("tournament:room-updated", args -> {
            Log.d(TAG, "🔥 Room update notification received");

            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String tournamentName = data.optString("tournamentName", "Tournament");
                    String roomId = data.optString("roomId", "");
                    String roomPassword = data.optString("roomPassword", "");

                    runOnUiThread(() -> {
                        NotificationItem notification = new NotificationItem(
                                "Room ID & Password Updated",
                                tournamentName + " credentials updated. ID: " + roomId + " Pass: " + roomPassword,
                                System.currentTimeMillis(),
                                NotificationType.ROOM_UPDATE
                        );

                        addNewNotification(notification);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing room update", e);
                }
            }
        });

        // 🔥 LISTEN FOR TOURNAMENT STATUS UPDATES
        socket.on("tournament:status-updated", args -> {
            Log.d(TAG, "🔥 Status update notification received");

            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String tournamentName = data.optString("tournamentName", "Tournament");
                    String status = data.optString("status", "");

                    runOnUiThread(() -> {
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
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing status update", e);
                }
            }
        });

        // 🔥 LISTEN FOR WALLET BALANCE UPDATES
        socket.on("wallet:balance-updated", args -> {
            Log.d(TAG, "🔥 Wallet balance update notification received");

            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    double balanceGC = data.optDouble("balanceGC", 0);
                    double updatedAt = data.optDouble("updatedAt", 0);

                    runOnUiThread(() -> {
                        NotificationItem notification = new NotificationItem(
                                "Wallet Balance Updated",
                                "Your new balance is " + (int) Math.round(balanceGC) + " GC",
                                System.currentTimeMillis(),
                                NotificationType.REWARD
                        );

                        addNewNotification(notification);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing balance update", e);
                }
            }
        });

        // 🔥 LISTEN FOR WALLET TRANSACTION UPDATES
        socket.on("wallet:transaction-updated", args -> {
            Log.d(TAG, "🔥 Wallet transaction update notification received");

            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String action = data.optString("action", "");

                    runOnUiThread(() -> {
                        String message = "";
                        NotificationType type = NotificationType.REWARD;

                        if ("approved".equalsIgnoreCase(action)) {
                            message = "Your transaction has been approved";
                        } else if ("rejected".equalsIgnoreCase(action)) {
                            message = "Your transaction has been rejected";
                            type = NotificationType.SYSTEM;
                        } else if ("updated".equalsIgnoreCase(action)) {
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
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing transaction update", e);
                }
            }
        });

        // 🔥 LISTEN FOR WALLET HISTORY UPDATES
        socket.on("wallet:history-updated", args -> {
            Log.d(TAG, "🔥 Wallet history update notification received");

            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];

                    runOnUiThread(() -> {
                        NotificationItem notification = new NotificationItem(
                                "Transaction History Updated",
                                "New transaction added to your wallet",
                                System.currentTimeMillis(),
                                NotificationType.REWARD
                        );

                        addNewNotification(notification);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing history update", e);
                }
            }
        });

        // 🔥 LISTEN FOR QR PAYMENT STATUS UPDATES
        socket.on("payment:qr-status-updated", args -> {
            Log.d(TAG, "🔥 QR payment status update notification received");

            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String status = data.optString("status", "");
                    String action = data.optString("action", "");
                    double amountGC = data.optDouble("amountGC", 0);

                    runOnUiThread(() -> {
                        String message = "";
                        NotificationType type = NotificationType.REWARD;

                        if ("success".equalsIgnoreCase(status) || "approved".equalsIgnoreCase(action)) {
                            message = "Payment successful! " + (int) Math.round(amountGC) + " GC credited to your wallet";
                            type = NotificationType.REWARD;
                        } else if ("pending".equalsIgnoreCase(status)) {
                            message = "Payment is being processed";
                            type = NotificationType.SYSTEM;
                        } else if ("rejected".equalsIgnoreCase(action)) {
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
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing QR payment status", e);
                }
            }
        });

        Log.d(TAG, "✅ All socket listeners setup complete");
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
        if (socket != null) {
            String userId = TokenManager.getUserId(this);
            if (userId != null) {
                socket.emit("unsubscribe:wallet", userId);
            }

            socket.off("tournament:room-updated");
            socket.off("tournament:status-updated");
            socket.off("wallet:balance-updated");
            socket.off("wallet:transaction-updated");
            socket.off("wallet:history-updated");
            socket.off("payment:qr-status-updated");

            Log.d(TAG, "✅ All socket listeners removed");
        }
    }
}