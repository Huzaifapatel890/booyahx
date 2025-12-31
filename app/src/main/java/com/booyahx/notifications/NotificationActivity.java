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

        // Listen for room updates
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

        // Listen for status updates
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
            socket.off("tournament:room-updated");
            socket.off("tournament:status-updated");
        }
    }
}