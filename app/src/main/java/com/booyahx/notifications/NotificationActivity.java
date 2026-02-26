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
import com.booyahx.utils.NotificationPref;

import java.util.HashMap;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ImageView backButton;
    private TextView emptyText;
    private TextView clearAllButton;
    private NotificationManager notificationManager;
    private static final String TAG = "NotificationActivity";

    // ─── Duplicate-notification filter ───────────────────────────────────────
    // Key   : "title|message"  (content fingerprint)
    // Value : timestamp (ms) of the last time that key was displayed
    //
    // Capped manually at DEDUP_MAP_MAX_SIZE inside isDuplicate() — avoids the
    // anonymous-LinkedHashMap pattern that caused compile errors on Android.
    // ─────────────────────────────────────────────────────────────────────────
    private final HashMap<String, Long> lastSeenNotifMap = new HashMap<>();
    private static final long DEDUP_WINDOW_MS    = 5_000L; // 5-second dedup window
    private static final int  DEDUP_MAP_MAX_SIZE = 200;    // max fingerprints kept

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // USER ISOLATION: drop broadcasts not meant for the current user
            String targetUserId  = intent.getStringExtra("targetUserId");
            String currentUserId = com.booyahx.TokenManager.getUserId(context);
            if (targetUserId != null && !targetUserId.equals(currentUserId)) {
                return;
            }
            Log.d(TAG, "📨 Broadcast received: " + intent.getAction());
            // DashboardActivity already saved to NotificationManager — only refresh here.
            refreshDisplay();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        backButton     = findViewById(R.id.backButton);
        recyclerView   = findViewById(R.id.recyclerViewNotifications);
        emptyText      = findViewById(R.id.emptyNotificationsText);
        clearAllButton = findViewById(R.id.clearAllButton);

        backButton.setOnClickListener(v -> finish());
        clearAllButton.setOnClickListener(v -> clearAllNotifications());

        notificationManager = NotificationManager.getInstance(this);

        // USER ISOLATION: enforce correct scope before reading any data
        String currentUserId = com.booyahx.TokenManager.getUserId(this);
        if (currentUserId != null) {
            notificationManager.switchUser(currentUserId);
        }

        NotificationPref.setUnread(this, false);

        setupRecyclerView();
        loadNotifications();
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

    // ─── Dedup helpers ────────────────────────────────────────────────────────

    /** Stable content fingerprint: title + "|" + message. */
    private static String dedupKey(NotificationItem item) {
        return item.getTitle() + "|" + item.getMessage();
    }

    /**
     * Returns true if {@code item} is a duplicate (same fingerprint seen within
     * DEDUP_WINDOW_MS). Side-effect: stamps/updates the map entry when not a
     * duplicate. Evicts the oldest entry if map exceeds DEDUP_MAP_MAX_SIZE.
     */
    private boolean isDuplicate(NotificationItem item) {
        String key  = dedupKey(item);
        long   now  = System.currentTimeMillis();
        Long   last = lastSeenNotifMap.get(key);

        if (last != null && (now - last) < DEDUP_WINDOW_MS) {
            Log.d(TAG, "⚠️ Duplicate suppressed: " + key);
            return true;
        }

        // Evict the oldest entry if we're at the cap
        if (lastSeenNotifMap.size() >= DEDUP_MAP_MAX_SIZE) {
            String oldestKey = null;
            long   oldestTs  = Long.MAX_VALUE;
            for (HashMap.Entry<String, Long> e : lastSeenNotifMap.entrySet()) {
                if (e.getValue() < oldestTs) {
                    oldestTs  = e.getValue();
                    oldestKey = e.getKey();
                }
            }
            if (oldestKey != null) lastSeenNotifMap.remove(oldestKey);
        }

        lastSeenNotifMap.put(key, now);
        return false;
    }

    /**
     * Walks the full notification list and removes every duplicate entry.
     * Iterates from the end so index-based removal is safe.
     */
    private void removeDuplicatesFromManager() {
        List<NotificationItem> all = notificationManager.getAllNotifications();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (isDuplicate(all.get(i))) {
                notificationManager.removeNotification(i);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when a live broadcast arrives. NotificationManager already has the
     * new entry saved (via DashboardActivity). Dedup then re-render.
     */
    private void refreshDisplay() {
        runOnUiThread(() -> {
            removeDuplicatesFromManager();
            adapter.updateNotifications(notificationManager.getAllNotifications());
            recyclerView.smoothScrollToPosition(0);
            updateEmptyState();
        });
    }

    /**
     * Initial load on Activity open. Cleans stale entries then runs a full dedup
     * pass so duplicates saved to disk are removed before first render.
     */
    private void loadNotifications() {
        List<NotificationItem> all = notificationManager.getAllNotifications();
        for (int i = all.size() - 1; i >= 0; i--) {
            if ("Transaction History Updated".equals(all.get(i).getTitle())) {
                notificationManager.removeNotification(i);
            }
        }
        removeDuplicatesFromManager();
        adapter.updateNotifications(notificationManager.getAllNotifications());
        updateEmptyState();
    }

    private void removeNotification(int position) {
        RecyclerView.ViewHolder viewHolder =
                recyclerView.findViewHolderForAdapterPosition(position);
        if (viewHolder != null) {
            viewHolder.itemView.animate()
                    .translationX(-viewHolder.itemView.getWidth())
                    .alpha(0f)
                    .setDuration(280)
                    .withEndAction(() -> {
                        viewHolder.itemView.setAlpha(1f);
                        viewHolder.itemView.setTranslationX(0f);
                        notificationManager.removeNotification(position);
                        adapter.updateNotifications(notificationManager.getAllNotifications());
                        updateEmptyState();
                    })
                    .start();
        } else {
            notificationManager.removeNotification(position);
            adapter.updateNotifications(notificationManager.getAllNotifications());
            updateEmptyState();
        }
    }

    private void clearAllNotifications() {
        if (notificationManager.getCount() == 0) return;

        recyclerView.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(250)
                .withEndAction(() -> {
                    notificationManager.clearAllNotifications();
                    lastSeenNotifMap.clear(); // reset so fresh notifs always show
                    adapter.updateNotifications(notificationManager.getAllNotifications());
                    recyclerView.setTranslationY(0f);
                    recyclerView.setAlpha(1f);
                    updateEmptyState();
                })
                .start();
    }

    private void updateEmptyState() {
        boolean isEmpty = notificationManager.getCount() == 0;
        recyclerView.setVisibility(isEmpty   ? View.GONE    : View.VISIBLE);
        emptyText.setVisibility(isEmpty      ? View.VISIBLE : View.GONE);
        clearAllButton.setVisibility(isEmpty ? View.GONE    : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "✅ NotificationActivity destroyed");
    }
}