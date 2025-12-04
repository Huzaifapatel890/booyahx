package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.booyahx.settings.ChangePasswordActivity;
import com.booyahx.settings.AboutActivity;
import com.booyahx.settings.EditProfileActivity;
import com.booyahx.settings.ThemeActivity;
import com.booyahx.settings.WinningHistoryActivity;
import com.booyahx.settings.SupportActivity;
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Click listeners
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, EditProfileActivity.class));
        });

        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class));
        });

        findViewById(R.id.btnTheme).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ThemeActivity.class));
        });

        findViewById(R.id.btnSupport).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, SupportActivity.class));
        });

        findViewById(R.id.btnAbout).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
        });

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, WinningHistoryActivity.class));
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            getSharedPreferences("USER", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(SettingsActivity.this, LoginUsernameActivity.class));
            finish();
        });
    }
}