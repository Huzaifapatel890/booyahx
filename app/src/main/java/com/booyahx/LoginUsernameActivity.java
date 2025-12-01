package com.booyahx;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.LoginRequest;
import com.booyahx.network.models.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginUsernameActivity extends AppCompatActivity {

    EditText etLoginUsername, etLoginPassword;
    LinearLayout btnLogin;
    TextView txtCreateAccount, txtForgotPassword, txtLoginBtnText;
    ImageView eyeLogin;
    ProgressBar loginLoader;

    boolean showPass = false;

    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_username);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        api = ApiClient.getClient().create(ApiService.class);

        etLoginUsername = findViewById(R.id.etLoginUsername);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtCreateAccount = findViewById(R.id.txtCreateAccount);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        eyeLogin = findViewById(R.id.eyeLogin);

        // NEW
        loginLoader = findViewById(R.id.loginLoader);
        txtLoginBtnText = findViewById(R.id.txtLoginBtnText);

        // 👁 Toggle Password + Change Icon
        eyeLogin.setOnClickListener(v -> {
            showPass = !showPass;

            if (showPass) {
                etLoginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                eyeLogin.setImageResource(R.drawable.ic_eye_on);
            } else {
                etLoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                eyeLogin.setImageResource(R.drawable.ic_eye_off);
            }
            etLoginPassword.setSelection(etLoginPassword.getText().length());
        });

        // Login button
        btnLogin.setOnClickListener(v -> {
            String email = etLoginUsername.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (email.isEmpty()) { etLoginUsername.setError("Enter email"); return; }
            if (pass.isEmpty()) { etLoginPassword.setError("Enter password"); return; }

            // SHOW LOADER
            btnLogin.setEnabled(false);
            loginLoader.setVisibility(View.VISIBLE);
            txtLoginBtnText.setText("Please wait...");

            loginUser(email, pass);
        });

        txtForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginUsernameActivity.this, ForgotPasswordActivity.class));
        });

        txtCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(LoginUsernameActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser(String email, String password) {
        Call<AuthResponse> call = api.loginUser(new LoginRequest(email, password));

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {

                // HIDE LOADER
                btnLogin.setEnabled(true);
                loginLoader.setVisibility(View.GONE);
                txtLoginBtnText.setText("Login");

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    getSharedPreferences("USER", MODE_PRIVATE)
                            .edit().putString("TOKEN", response.body().getToken()).apply();

                    Toast.makeText(LoginUsernameActivity.this, "Login Successful 🎉", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginUsernameActivity.this, DashboardActivity.class));
                    finish();

                } else {
                    Toast.makeText(LoginUsernameActivity.this, "Invalid Credentials!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {

                // HIDE LOADER
                btnLogin.setEnabled(true);
                loginLoader.setVisibility(View.GONE);
                txtLoginBtnText.setText("Login");

                Toast.makeText(LoginUsernameActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}