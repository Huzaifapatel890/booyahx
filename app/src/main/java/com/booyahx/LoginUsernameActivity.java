package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginUsernameActivity extends AppCompatActivity {

    EditText etLoginUsername, etLoginPassword;
    LinearLayout btnLogin;
    TextView txtCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_username);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        etLoginUsername = findViewById(R.id.etLoginUsername);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtCreateAccount = findViewById(R.id.txtCreateAccount);

        btnLogin.setOnClickListener(v -> {
            String user = etLoginUsername.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (user.isEmpty()) { etLoginUsername.setError("Enter username"); return; }
            if (pass.isEmpty()) { etLoginPassword.setError("Enter password"); return; }

            Toast.makeText(this, "Login Success (UI only)", Toast.LENGTH_SHORT).show();
        });
        TextView txtForgotPassword = findViewById(R.id.txtForgotPassword);

        txtForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginUsernameActivity.this, ForgotPasswordActivity.class));
        });

        txtCreateAccount.setOnClickListener(v -> {
            Intent i = new Intent(LoginUsernameActivity.this, RegisterActivity.class);
            startActivity(i);
        });
    }
}