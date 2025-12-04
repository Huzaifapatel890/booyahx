package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // -------------------------------------------------------------------
        // SETTINGS BUTTON CLICK → OPEN SettingsActivity
        // -------------------------------------------------------------------
        findViewById(R.id.NavSettings).setOnClickListener(v -> {
            Intent i = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(i);
        });

        // (Later you can plug in more click listeners for tournament cards,
        // profile card, wallet section, etc. Not touching anything else now.)
    }
}