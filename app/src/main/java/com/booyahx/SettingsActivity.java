package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    LinearLayout btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> performLogout());
    }

    // -----------------------------------------
    // 🔥 SAFE LOGOUT FUNCTION
    // -----------------------------------------
    private void performLogout() {

        // 1. Clear token storage
        TokenManager.logout(this);

        // 2. Completely clear activity stack & redirect to login
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }
}