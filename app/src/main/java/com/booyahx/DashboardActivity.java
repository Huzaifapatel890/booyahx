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
import com.booyahx.notifications.NotificationItem;
import com.booyahx.notifications.NotificationManager;
import com.booyahx.notifications.NotificationType;
import com.booyahx.socket.SocketManager;
import com.booyahx.utils.NotificationPref;

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
                }
            }
        };

        // Register for all socket events
        IntentFilter filter = new IntentFilter();
        filter.addAction("WALLET_UPDATED");
        filter.addAction("TOURNAMENT_ROOM_UPDATED");
        filter.addAction("TOURNAMENT_STATUS_UPDATED");
        filter.addAction("PAYMENT_QR_UPDATED");

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
                    double balanceGC = data.optDouble("balanceGC", -1);
                    String txAction = data.optString("action", "");

                    if (balanceGC >= 0) {
                        notification = new NotificationItem(
                                "Wallet Balance Updated",
                                "Your new balance is " + (int) Math.round(balanceGC) + " GC",
                                System.currentTimeMillis(),
                                NotificationType.REWARD
                        );
                    } else if (!txAction.isEmpty()) {
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
                            notification = new NotificationItem(
                                    "Transaction Update", message,
                                    System.currentTimeMillis(), type);
                        }
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
                }

            } catch (Exception e) {
                Log.e(TAG, "Error saving wallet notification", e);
            }
        });
    }

    /**
     * Handle tournament room update event
     * ✅ FIX: Now saves notification to NotificationManager so it persists for NotificationActivity
     */
    private void handleTournamentRoomUpdate(Intent intent) {
        String dataJson = intent.getStringExtra("data");

        runOnUiThread(() -> {
            // Notify fragments about room updates
            getSupportFragmentManager().setFragmentResult(
                    "joined_refresh",
                    new Bundle()
            );

            // ✅ FIX: Save notification to persistent store so NotificationActivity can show it
            try {
                NotificationManager nm = NotificationManager.getInstance(this);
                String tournamentName = "Tournament";
                String roomId = "";
                String roomPassword = "";

                if (dataJson != null) {
                    JSONObject data = new JSONObject(dataJson);
                    tournamentName = data.optString("tournamentName", "Tournament");
                    roomId = data.optString("roomId", "");
                    roomPassword = data.optString("roomPassword", "");
                }

                NotificationItem notification = new NotificationItem(
                        "Room ID & Password Updated",
                        tournamentName + " credentials updated. ID: " + roomId + " Pass: " + roomPassword,
                        System.currentTimeMillis(),
                        NotificationType.ROOM_UPDATE
                );

                nm.addNotification(notification);
                NotificationPref.setUnread(this, true);
                Log.d(TAG, "✅ Room update notification saved to NotificationManager");

            } catch (Exception e) {
                Log.e(TAG, "Error saving room update notification", e);
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

                // ✅ Broadcast to ParticipatedFragment (through joined_refresh)
                getSupportFragmentManager().setFragmentResult(
                        "joined_refresh",
                        new Bundle()
                );

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
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing tournament status update", e);
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
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving payment notification", e);
            }
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