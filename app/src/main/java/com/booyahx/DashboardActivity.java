package com.booyahx;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Later we’ll wire:
        // - username from login response
        // - wallet balance from API
        // - click listeners for cards & bottom nav
    }
}