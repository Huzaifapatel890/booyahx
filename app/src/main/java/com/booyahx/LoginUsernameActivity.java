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

import com.booyahx.TokenManager;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.LoginRequest;
import com.booyahx.network.models.AuthResponse;
import com.booyahx.utils.CSRFHelper;

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
                etLoginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                eyeLogin.setImageResource(R.drawable.ic_eye_on);
            } else {
                etLoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                eyeLogin.setImageResource(R.drawable.ic_eye_off);
            }
            etLoginPassword.setSelection(etLoginPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> {
            String email = etLoginUsername.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (email.isEmpty()) { etLoginUsername.setError("Enter email"); return; }
            if (pass.isEmpty()) { etLoginPassword.setError("Enter password"); return; }

            // 🔵 FIRST GET CSRF TOKEN BEFORE LOGIN
            CSRFHelper.fetchToken(LoginUsernameActivity.this, new CSRFHelper.CSRFCallback() {
                @Override
                public void onSuccess(String token) {
                    // Now perform login
                    loginUser(email, pass);
                }

                @Override
                public void onFailure(String error) {
                    showTopRightToast("Security error! Please try again.");
                }
            });
        });

        txtForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginUsernameActivity.this, ForgotPasswordActivity.class))
        );

        txtCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(LoginUsernameActivity.this, RegisterActivity.class))
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

    // ----------------------------------------------------
    // LOGIN USER + FULLSCREEN LOADER
    // ----------------------------------------------------
    private void loginUser(String email, String password) {

        LoaderOverlay.show(LoginUsernameActivity.this);

        Call<AuthResponse> call = api.loginUser(new LoginRequest(email, password));

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {

                LoaderOverlay.hide(LoginUsernameActivity.this);

                if (response.isSuccessful() && response.body() != null && response.body().success) {

                    TokenManager.saveTokens(
                            LoginUsernameActivity.this,
                            response.body().data.accessToken,
                            response.body().data.refreshToken
                    );

                    String msg = response.body().message;
                    showTopRightToast(msg != null ? msg : "Login successful");

                    Intent intent = new Intent(LoginUsernameActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                } else {

                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : null;

                        if (err != null) {
                            JSONObject obj = new JSONObject(err);
                            String msg = obj.optString("message", "Invalid Credentials!");
                            showTopRightToast(msg);
                        } else {
                            showTopRightToast("Invalid Credentials!");
                        }

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