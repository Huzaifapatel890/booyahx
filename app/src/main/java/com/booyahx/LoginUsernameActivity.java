package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.CsrfResponse;
import com.booyahx.network.models.LoginRequest;
import com.booyahx.network.models.AuthResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginUsernameActivity extends AppCompatActivity {

    EditText etLoginUsername, etLoginPassword;
    LinearLayout btnLogin;
    TextView txtCreateAccount, txtForgotPassword, txtLoginBtnText;
    ImageView eyeLogin;
    ProgressBar loginLoader;

    Boolean showPass = false;
    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_username);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        api = ApiClient.getClient(this).create(ApiService.class);

        etLoginUsername = findViewById(R.id.etLoginUsername);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtCreateAccount = findViewById(R.id.txtCreateAccount);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        eyeLogin = findViewById(R.id.eyeLogin);
        loginLoader = findViewById(R.id.loginLoader);
        txtLoginBtnText = findViewById(R.id.txtLoginBtnText);

        loginLoader.setVisibility(View.GONE);

        eyeLogin.setOnClickListener(v -> {
            showPass = !showPass;

            if (showPass) {
                etLoginPassword.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance()
                );
                eyeLogin.setImageResource(R.drawable.ic_eye_on);
            } else {
                etLoginPassword.setTransformationMethod(
                        PasswordTransformationMethod.getInstance()
                );
                eyeLogin.setImageResource(R.drawable.ic_eye_off);
            }
            etLoginPassword.setSelection(etLoginPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> {
            String email = etLoginUsername.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etLoginUsername.setError("Enter email");
                return;
            }
            if (pass.isEmpty()) {
                etLoginPassword.setError("Enter password");
                return;
            }

            fetchCsrfAndLogin(email, pass);
        });

        txtForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(
                        LoginUsernameActivity.this,
                        ForgotPasswordActivity.class
                ))
        );

        txtCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(
                        LoginUsernameActivity.this,
                        RegisterActivity.class
                ))
        );
    }

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

    private void fetchCsrfAndLogin(String email, String password) {

        LoaderOverlay.show(LoginUsernameActivity.this);

        api.getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call, Response<CsrfResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    String csrfToken = response.body().getData().getCsrfToken();
                    TokenManager.saveCsrf(LoginUsernameActivity.this, csrfToken);

                    loginUser(email, password);
                } else {
                    LoaderOverlay.hide(LoginUsernameActivity.this);
                    showTopRightToast("Failed to get CSRF token");
                }
            }

            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {
                LoaderOverlay.hide(LoginUsernameActivity.this);
                showTopRightToast("Network Error!");
            }
        });
    }

    private void loginUser(String email, String password) {

        Call<AuthResponse> call = api.loginUser(new LoginRequest(email, password));

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {

                LoaderOverlay.hide(LoginUsernameActivity.this);

                if (response.isSuccessful() && response.body() != null) {

                    AuthResponse resp = response.body();

                    String backendMsg = resp.message != null ? resp.message : "Success";
                    showTopRightToast(backendMsg);

                    if (resp.success && resp.data != null) {

                        TokenManager.saveTokens(
                                LoginUsernameActivity.this,
                                resp.data.accessToken,
                                resp.data.refreshToken
                        );
                        TokenManager.saveRole(LoginUsernameActivity.this, resp.data.role);

                        // 🔥 FCM: Save device token to backend so user receives push notifications
                        FcmHelper.saveTokenToServer(LoginUsernameActivity.this);

                        Intent intent = new Intent(LoginUsernameActivity.this, DashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                } else {

                    try {
                        String raw = response.errorBody() != null ? response.errorBody().string() : null;

                        if (raw != null) {
                            JSONObject obj = new JSONObject(raw);

                            if (obj.has("message")) {
                                showTopRightToast(obj.getString("message"));
                                return;
                            }

                            if (obj.has("errors")) {
                                JSONArray arr = obj.getJSONArray("errors");
                                if (arr.length() > 0) {
                                    String msg = arr.getJSONObject(0)
                                            .optString("message", "Invalid Credentials!");
                                    showTopRightToast(msg);
                                    return;
                                }
                            }
                        }

                        showTopRightToast("Invalid Credentials!");

                    } catch (Exception e) {
                        showTopRightToast("Invalid Credentials!");
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {

                LoaderOverlay.hide(LoginUsernameActivity.this);

                showTopRightToast("Network Error!");
            }
        });
    }
}