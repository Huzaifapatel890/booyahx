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
import com.booyahx.socket.SocketManager;

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

        // Setup broadcast receivers for socket events
        setupSocketBroadcastReceivers();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister broadcast receivers
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ UNSUBSCRIBE WHEN LEAVING DASHBOARD
        SocketManager.unsubscribe();
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
    }

    /**
     * Handle wallet update event
     */
    private void handleWalletUpdate(Intent intent) {
        runOnUiThread(() -> {
            // Notify fragments to refresh wallet balance
            getSupportFragmentManager().setFragmentResult(
                    "balance_updated",
                    new Bundle()
            );
        });
    }

    /**
     * Handle tournament room update event
     */
    private void handleTournamentRoomUpdate(Intent intent) {
        runOnUiThread(() -> {
            // Notify fragments about room updates
            getSupportFragmentManager().setFragmentResult(
                    "joined_refresh",
                    new Bundle()
            );
        });
    }

    /**
     * Handle tournament status update event
     * ✅ Broadcasts to: HomeFragment, HostTournamentFragment, ParticipatedFragment
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
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing tournament status update", e);
            }
        });
    }

    /**
     * Handle payment QR status update event
     */
    private void handlePaymentQRUpdate(Intent intent) {
        String dataJson = intent.getStringExtra("data");

        runOnUiThread(() -> {
            Log.d(TAG, "Payment QR status updated: " + dataJson);
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