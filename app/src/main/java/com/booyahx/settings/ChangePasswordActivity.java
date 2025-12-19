package com.booyahx.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.LoaderOverlay;
import com.booyahx.R;
import com.booyahx.TokenManager;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ChangePasswordRequest;
import com.booyahx.network.models.CsrfResponse;
import com.booyahx.network.models.SimpleResponse;
import com.booyahx.LoginUsernameActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    EditText edtOldPassword, edtNewPassword, edtConfirmPassword;
    TextView btnChangePassword;
    ImageView btnBack;

    private String csrfToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        fetchCsrfToken();   // Load CSRF with Loader
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnBack.setOnClickListener(v -> finish());
    }

    // ---------------------------------------------------
    // 1️⃣ FETCH CSRF TOKEN — WITH LOADER
    // ---------------------------------------------------
    private void fetchCsrfToken() {

        LoaderOverlay.show(this);   // SHOW LOADER

        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        api.getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call, Response<CsrfResponse> response) {

                LoaderOverlay.hide(ChangePasswordActivity.this);  // HIDE LOADER

                if (response.isSuccessful() && response.body() != null) {
                    csrfToken = response.body().getData().getCsrfToken();
                } else {
                    showToast("Failed to get CSRF token");
                }
            }

            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {
                LoaderOverlay.hide(ChangePasswordActivity.this);
                showToast("Network error fetching CSRF");
            }
        });
    }

    private void setupListeners() {
        btnChangePassword.setOnClickListener(v -> validateAndSend());
    }

    // ---------------------------------------------------
    // 3️⃣ VALIDATION
    // ---------------------------------------------------
    private void validateAndSend() {

        String oldP = edtOldPassword.getText().toString().trim();
        String newP = edtNewPassword.getText().toString().trim();
        String confirmP = edtConfirmPassword.getText().toString().trim();

        if (oldP.isEmpty()) {
            showToast("Enter old password");
            return;
        }

        if (newP.isEmpty()) {
            showToast("Enter new password");
            return;
        }

        if (newP.length() < 8) {
            showToast("New password must be at least 8 characters");
            return;
        }

        if (newP.equals(oldP)) {
            showToast("New password cannot be same as old password");
            return;
        }

        if (!newP.equals(confirmP)) {
            showToast("New password and confirm password must match");
            return;
        }

        sendChangePassword(oldP, newP, confirmP);
    }

    // ---------------------------------------------------
    // 4️⃣ API CALL — WITH LOADER
    // ---------------------------------------------------
    private void sendChangePassword(String oldP, String newP, String confirmP) {

        LoaderOverlay.show(this);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        ChangePasswordRequest req = new ChangePasswordRequest(oldP, newP, confirmP);

        String accessToken = TokenManager.getAccessToken(this);

        api.changePassword(
                "Bearer " + accessToken,
                csrfToken,
                req
        ).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {

                LoaderOverlay.hide(ChangePasswordActivity.this);

                if (response.isSuccessful() && response.body() != null) {

                    showToast(response.body().getMessage());

                    // CLEAR TOKENS
                    TokenManager.logout(ChangePasswordActivity.this);

                    // REDIRECT TO LOGIN
                    Intent intent = new Intent(ChangePasswordActivity.this, LoginUsernameActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    finish();
                } else {
                    showToast("Failed to change password");
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                LoaderOverlay.hide(ChangePasswordActivity.this);
                showToast("Error: " + t.getMessage());
            }
        });
    }

    // ---------------------------------------------------
    // 5️⃣ Neon Toast
    // ---------------------------------------------------
    private void showToast(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(40, 25, 40, 25);
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackgroundResource(R.drawable.toast_bg);
        tv.setTextSize(14);

        android.widget.Toast toast = new android.widget.Toast(getApplicationContext());
        toast.setView(tv);
        toast.setDuration(android.widget.Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.END, 40, 120);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(350);
        tv.startAnimation(fade);

        toast.show();
    }
}