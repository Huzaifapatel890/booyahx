package com.booyahx;

import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.booyahx.socket.SocketManager;

import org.json.JSONObject;

import io.socket.client.Socket;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout navHome, navParticipated, navWallet, navSettings;
    private TextView tvNavHome, tvNavParticipated, tvNavWallet, tvNavProfile;

    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        Log.d("SOCKET_FLOW", "Dashboard onCreate");

        navHome = findViewById(R.id.navHome);
        navParticipated = findViewById(R.id.navParticipated);
        navWallet = findViewById(R.id.navWallet);
        navSettings = findViewById(R.id.NavSettings);

        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavParticipated = findViewById(R.id.tvNavParticipated);
        tvNavWallet = findViewById(R.id.tvNavWallet);
        tvNavProfile = findViewById(R.id.tvNavProfile);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), 0, false);
            setActive(tvNavHome);
        }

        navHome.setOnClickListener(v -> {
            Log.d("SOCKET_FLOW", "Home clicked");
            loadFragment(new HomeFragment(), 0, true);
            setActive(tvNavHome);
        });

        navParticipated.setOnClickListener(v -> {
            Log.d("SOCKET_FLOW", "Joined clicked");
            loadFragment(new ParticipatedFragment(), 1, true);
            setActive(tvNavParticipated);
        });

        navWallet.setOnClickListener(v -> {
            Log.d("SOCKET_FLOW", "Wallet clicked");
            loadFragment(new WalletFragment(), 2, true);
            setActive(tvNavWallet);
        });

        navSettings.setOnClickListener(v -> {
            Log.d("SOCKET_FLOW", "Settings clicked");
            loadFragment(new SettingsFragment(), 3, true);
            setActive(tvNavProfile);
        });

        String token = TokenManager.getAccessToken(this);
        Log.d("SOCKET_FLOW", "Requesting socket with token");

        socket = SocketManager.getSocket(token);
        SocketManager.connect();

        // 🔥 FIX: SUBSCRIBE IF SOCKET IS ALREADY CONNECTED
        String userId = TokenManager.getUserId(DashboardActivity.this);
        if (socket != null && socket.connected() && userId != null && !userId.isEmpty()) {
            Log.d("SOCKET_FLOW", "🔔 Socket already connected, subscribing immediately: " + userId);
            socket.emit("subscribe:user-tournaments", userId);
        }

        socket.on(Socket.EVENT_CONNECT, args -> {
            String uid = TokenManager.getUserId(DashboardActivity.this);
            Log.d("SOCKET_FLOW", "✅ Socket connected, subscribing user: " + uid);

            if (uid != null && !uid.isEmpty()) {
                socket.emit("subscribe:user-tournaments", uid);
            }
        });

        socket.on("tournament:room-updated", args -> {
            Log.d("SOCKET_FLOW", "🔥 tournament:room-updated EVENT RECEIVED");
            Log.d("SOCKET_FLOW", "Payload = " + (args.length > 0 ? args[0] : "null"));

            runOnUiThread(() ->
                    getSupportFragmentManager().setFragmentResult(
                            "joined_refresh",
                            new Bundle()
                    )
            );
        });

        socket.on("tournament:status-updated", args -> {
            Log.d("SOCKET_FLOW", "🔥 tournament:status-updated EVENT RECEIVED");

            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String tournamentId = data.optString("tournamentId", "");
                    String newStatus = data.optString("status", "");

                    Log.d("SOCKET_FLOW", "Tournament ID: " + tournamentId);
                    Log.d("SOCKET_FLOW", "New Status: " + newStatus);

                    runOnUiThread(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("tournament_id", tournamentId);
                        bundle.putString("new_status", newStatus);

                        getSupportFragmentManager().setFragmentResult(
                                "tournament_status_changed",
                                bundle
                        );

                        getSupportFragmentManager().setFragmentResult(
                                "joined_refresh",
                                new Bundle()
                        );
                    });

                } catch (Exception e) {
                    Log.e("SOCKET_FLOW", "Error parsing status update", e);
                }
            }
        });
    }

    private int currentIndex = 0;

    private void loadFragment(Fragment fragment, int newIndex, Boolean animate) {
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