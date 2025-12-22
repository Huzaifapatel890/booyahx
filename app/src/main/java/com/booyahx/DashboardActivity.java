package com.booyahx;

import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout navHome, navParticipated, navWallet, navSettings;
    private TextView tvNavHome, tvNavParticipated, tvNavWallet, tvNavProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        navHome = findViewById(R.id.navHome);
        navParticipated = findViewById(R.id.navParticipated);
        navWallet = findViewById(R.id.navWallet);
        navSettings = findViewById(R.id.NavSettings);

        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavParticipated = findViewById(R.id.tvNavParticipated);
        tvNavWallet = findViewById(R.id.tvNavWallet);
        tvNavProfile = findViewById(R.id.tvNavProfile);

        // Default → Home
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), 0, false);
            setActive(tvNavHome);
        }

        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment(), 0, true);
            setActive(tvNavHome);
        });

        navParticipated.setOnClickListener(v -> {
            loadFragment(new ParticipatedFragment(), 1, true);
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

    // ----------------------------------------------------
    // ✅ CUSTOM NEON TOAST (ADDED — NOTHING ELSE TOUCHED)
    // ----------------------------------------------------
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