package com.booyahx;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
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

        // REMOVE ACTION BAR
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // ---------------------------
        // GOOGLE LOGIN SETUP
        // ---------------------------
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("944164444686-o2vtkpfvrbgas2o7qgfgi3in20sudq2in.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // ON GOOGLE BUTTON CLICK
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE);
        });

        // MOBILE LOGIN
        btnMobile.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginUsernameActivity.class);
            startActivity(intent);
        });
    }

    // ------------------------------------
    // HANDLE GOOGLE LOGIN RESULT
    // ------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                if (account != null) {
                    String idToken = account.getIdToken();

                    // SEND TOKEN TO BACKEND
                    sendTokenToServer(idToken);
                }

            } catch (ApiException e) {
                e.printStackTrace();
                Toast.makeText(this, "Google Sign-In Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ------------------------------------
    // SEND GOOGLE TOKEN TO BACKEND
    // ------------------------------------
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

                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        // GO TO DASHBOARD
                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(LoginActivity.this, resp.message, Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(LoginActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GoogleLoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // SAVE JWT TOKEN
    private void saveJwt(String jwt) {
        getSharedPreferences("AUTH", MODE_PRIVATE)
                .edit()
                .putString("jwt", jwt)
                .apply();
    }
}