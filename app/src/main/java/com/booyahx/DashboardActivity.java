package com.booyahx;

import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.content.Intent;
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
    private int currentIndex = 0;

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

        // 🔥 APPLY ROLE-BASED LABEL (FIRST TIME)
        applyRoleLabel();

        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment(), 0, true);
            setActive(tvNavHome);
        });

        // 🔥 ROLE-BASED FRAGMENT LOADING
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

        String token = TokenManager.getAccessToken(this);
        socket = SocketManager.getSocket(token);
        SocketManager.connect();

        String userId = TokenManager.getUserId(this);
        if (socket != null && socket.connected() && userId != null) {
            socket.emit("subscribe:user-tournaments", userId);
        }

        socket.on(Socket.EVENT_CONNECT, args -> {
            String uid = TokenManager.getUserId(this);
            if (uid != null) socket.emit("subscribe:user-tournaments", uid);
        });

        socket.on("tournament:room-updated", args ->
                runOnUiThread(() ->
                        getSupportFragmentManager().setFragmentResult(
                                "joined_refresh",
                                new Bundle()
                        )
                )
        );

        getSupportFragmentManager().setFragmentResultListener(
                "role_updated",
                this,
                (key, bundle) -> applyRoleLabel()
        );

        socket.on("tournament:status-updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Bundle bundle = new Bundle();
                    bundle.putString("tournament_id", data.optString("tournamentId"));
                    bundle.putString("new_status", data.optString("status"));

                    runOnUiThread(() -> {
                        getSupportFragmentManager().setFragmentResult(
                                "tournament_status_changed",
                                bundle
                        );
                        getSupportFragmentManager().setFragmentResult(
                                "joined_refresh",
                                new Bundle()
                        );
                    });
                } catch (Exception ignored) {}
            }
        });
    }

    // 🔥 THIS IS THE FIX
    @Override
    protected void onResume() {
        super.onResume();
        applyRoleLabel();
    }

    // 🔥 SINGLE SOURCE OF TRUTH FOR LABEL
    private void applyRoleLabel() {
        String role = TokenManager.getRole(this);
        if ("host".equalsIgnoreCase(role)) {
            tvNavParticipated.setText("Host Panel");
        } else {
            tvNavParticipated.setText("Joined");
        }
    }

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