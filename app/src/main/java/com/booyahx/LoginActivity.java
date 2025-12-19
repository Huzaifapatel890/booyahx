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
import com.booyahx.TokenManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class
LoginActivity extends AppCompatActivity {

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
                .requestIdToken("94416444686-r9qnipffen0vre39o2sab53689nkfjf0.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> {
            LoaderOverlay.show(LoginActivity.this);   // 🔵 SHOW LOADER BEFORE GOOGLE POPUP
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
                } else {
                    LoaderOverlay.hide(LoginActivity.this);
                    showTopRightToast("Google Sign-In failed");
                }

            } catch (ApiException e) {
                LoaderOverlay.hide(LoginActivity.this);
                showTopRightToast("Google Sign-In failed: " + e.getStatusCode());
            }
        }
    }

    // ----------------------------------------------------
    // SEND GOOGLE TOKEN TO SERVER + LOADER
    // ----------------------------------------------------
    private void sendTokenToServer(String token) {

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        GoogleLoginRequest req = new GoogleLoginRequest(token);

        api.loginWithGoogle(req).enqueue(new Callback<GoogleLoginResponse>() {
            @Override
            public void onResponse(Call<GoogleLoginResponse> call, Response<GoogleLoginResponse> response) {

                LoaderOverlay.hide(LoginActivity.this);  // 🔵 ALWAYS HIDE LOADER

                if (response.isSuccessful() && response.body() != null) {
                    GoogleLoginResponse resp = response.body();

                    if (resp.success && resp.data != null) {

                        TokenManager.saveTokens(
                                LoginActivity.this,
                                resp.data.accessToken,
                                resp.data.refreshToken
                        );

                        showTopRightToast(resp.message != null ? resp.message : "Login success!");

                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        showTopRightToast(resp.message != null ? resp.message : "Login failed");
                    }

                } else {
                    showTopRightToast("Server error");
                }
            }

            @Override
            public void onFailure(Call<GoogleLoginResponse> call, Throwable t) {
                LoaderOverlay.hide(LoginActivity.this);
                showTopRightToast("Network Error: " + t.getMessage());
            }
        });
    }
}