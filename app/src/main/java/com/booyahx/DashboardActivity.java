package com.booyahx;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout navHome, navParticipated, navWallet, navSettings;
    private ImageView icNavHome, icNavParticipated, icNavWallet, icNavSettings;
    private TextView tvNavHome, tvNavParticipated, tvNavWallet, tvNavProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize bottom nav views
        navHome = findViewById(R.id.navHome);
        navParticipated = findViewById(R.id.navParticipated);
        navWallet = findViewById(R.id.navWallet);
        navSettings = findViewById(R.id.NavSettings);

        icNavHome = findViewById(R.id.icNavHome);
        icNavParticipated = findViewById(R.id.icNavParticipated);
        icNavWallet = findViewById(R.id.icNavWallet);
        icNavSettings = findViewById(R.id.icNavsettings);

        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavParticipated = findViewById(R.id.tvNavParticipated);
        tvNavWallet = findViewById(R.id.tvNavWallet);
        tvNavProfile = findViewById(R.id.tvNavProfile);

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            setActiveNav(navHome, icNavHome, tvNavHome);
        }

        // Bottom Navigation Click Listeners
        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            setActiveNav(navHome, icNavHome, tvNavHome);
        });

        navParticipated.setOnClickListener(v -> {
            loadFragment(new ParticipatedFragment());
            setActiveNav(navParticipated, icNavParticipated, tvNavParticipated);
        });

        navWallet.setOnClickListener(v -> {
            loadFragment(new WalletFragment());
            setActiveNav(navWallet, icNavWallet, tvNavWallet);
        });

        navSettings.setOnClickListener(v -> {
            loadFragment(new SettingsFragment());
            setActiveNav(navSettings, icNavSettings, tvNavProfile);
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void setActiveNav(LinearLayout activeNav, ImageView activeIcon, TextView activeText) {
        // Reset all
        resetNavItem(navHome, tvNavHome);
        resetNavItem(navParticipated, tvNavParticipated);
        resetNavItem(navWallet, tvNavWallet);
        resetNavItem(navSettings, tvNavProfile);

        // Highlight active
        activeText.setTextColor(0xFF00C3FF); // Cyan color
    }

    private void resetNavItem(LinearLayout nav, TextView text) {
        text.setTextColor(0xFFAAAAAA); // Gray color
    }
}