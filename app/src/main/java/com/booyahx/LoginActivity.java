package com.booyahx;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.booyahx.network.ApiService;
import com.booyahx.network.ApiClient;
import com.booyahx.network.models.GoogleLoginRequest;
import com.booyahx.network.models.GoogleLoginResponse;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    LinearLayout btnGoogle, btnMobile;
    GoogleSignInClient mGoogleSignInClient;
    private static final int RC_GOOGLE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btnGoogle);
        btnMobile = findViewById(R.id.btnMobile);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("94416444686-65lr45ch2ujn7rv1latge4qmiau73jg2.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE);
        });

        btnMobile.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, LoginUsernameActivity.class))
        );
    }

    // ----------------------------------------------------
    // CUSTOM NEON TOAST
    // ----------------------------------------------------
    private void showTopRightToast(String message) {
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

    // ----------------------------------------------------
    // GOOGLE SIGN-IN RESULT
    // ----------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                if (account != null) {
                    sendTokenToServer(account.getIdToken());
                }

            } catch (ApiException e) {
                showTopRightToast("Google Sign-In failed: " + e.getStatusCode());
            }
        }
    }

    // ----------------------------------------------------
    // SEND GOOGLE TOKEN TO server
    // ----------------------------------------------------
    private void sendTokenToServer(String token) {

        GoogleLoginRequest req = new GoogleLoginRequest(token);
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.loginWithGoogle(req).enqueue(new Callback<GoogleLoginResponse>() {
            @Override
            public void onResponse(Call<GoogleLoginResponse> call, Response<GoogleLoginResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    GoogleLoginResponse resp = response.body();

                    if (resp.success) {

                        saveJwt(resp.jwt);

                        // 🔥 SUCCESS MESSAGE FROM BACKEND
                        showTopRightToast(resp.message != null ? resp.message : "Login success!");

                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();

                    } else {
                        // 🔥 ERROR MESSAGE FROM BACKEND
                        showTopRightToast(resp.message != null ? resp.message : "Login failed");
                    }

                } else {
                    // 🔥 API error JSON message
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : null;

                        if (err != null && err.contains("message")) {
                            showTopRightToast(new org.json.JSONObject(err).optString("message", "Server error"));
                        } else {
                            showTopRightToast("Server error");
                        }

                    } catch (Exception e) {
                        showTopRightToast("Server error");
                    }
                }
            }

            @Override
            public void onFailure(Call<GoogleLoginResponse> call, Throwable t) {
                showTopRightToast("Network Error: " + t.getMessage());
            }
        });
    }

    // ----------------------------------------------------
    // SAVE JWT
    // ----------------------------------------------------
    private void saveJwt(String jwt) {
        getSharedPreferences("AUTH", MODE_PRIVATE)
                .edit()
                .putString("jwt", jwt)
                .apply();
    }
}