package com.booyahx;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    LinearLayout btnGoogle, btnMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btnGoogle);
        btnMobile = findViewById(R.id.btnMobile);

        // REMOVE ACTION BAR
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // GOOGLE SIGN IN (UI ONLY)
        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Login Coming Soon", Toast.LENGTH_SHORT).show();
        });

        // MOBILE LOGIN → GO TO MOBILE NUMBER SCREEN FIRST
        btnMobile.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginUsernameActivity.class);
            startActivity(intent);
        });
    }
}