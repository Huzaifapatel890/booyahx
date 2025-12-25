package com.booyahx.notifications;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.recyclerViewNotifications);

        backButton.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadNotifications();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, this::removeNotification);
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {

        notificationList.add(new NotificationItem(
                "Room ID & Password Updated",
                "Tournament room credentials have been updated. Check lobby details.",
                "Just now",
                NotificationType.ROOM_UPDATE
        ));

        notificationList.add(new NotificationItem(
                "Tournament Starting Soon!",
                "Lobby 1 300 6PM is about to begin.",
                "5 minutes ago",
                NotificationType.TOURNAMENT
        ));

        notificationList.add(new NotificationItem(
                "Reward Received!",
                "You've earned 500 GC.",
                "1 hour ago",
                NotificationType.REWARD
        ));

        notificationList.add(new NotificationItem(
                "Victory!",
                "You ranked #3 and won 1200 GC!",
                "3 hours ago",
                NotificationType.VICTORY
        ));

        notificationList.add(new NotificationItem(
                "New Squad Invite",
                "PlayerX999 invited you to a squad.",
                "5 hours ago",
                NotificationType.SQUAD
        ));

        notificationList.add(new NotificationItem(
                "System Update",
                "New features added in Special mode.",
                "1 day ago",
                NotificationType.SYSTEM
        ));

        adapter.notifyDataSetChanged();
    }

    private void removeNotification(int position) {
        notificationList.remove(position);
        adapter.notifyItemRemoved(position);
    }
}