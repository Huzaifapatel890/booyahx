package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.AuthResponse;
import com.booyahx.network.models.RegisterRequest;
import com.booyahx.network.models.RegisterResponse;
import com.booyahx.network.models.VerifyOtpRequest;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etPassword;
    RelativeLayout btnSendOtp, btnRegister;
    TextView txtSendOtp, txtRegister, txtGoLogin;

    LinearLayout otpContainer;
    EditText otp1, otp2, otp3, otp4, otp5, otp6;

    boolean otpSent = false;
    CountDownTimer timer;
    long timeLeft = 60000;

    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        api = ApiClient.getClient().create(ApiService.class);

        initViews();
        setupOtpAutoMove();

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnRegister.setOnClickListener(v -> verifyOtpAndRegister());
        txtGoLogin.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etUsername = findViewById(R.id.etRegUsername);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnRegister = findViewById(R.id.btnRegister);

        txtSendOtp = findViewById(R.id.txtSendOtp);
        txtRegister = findViewById(R.id.txtRegister);
        txtGoLogin = findViewById(R.id.txtGoLogin);

        otpContainer = findViewById(R.id.otpContainer);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
    }

    // ----------------------------------------------------
    // NEON CUSTOM TOAST
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
    // SEND OTP
    // ----------------------------------------------------
    private void sendOtp() {
        String email = etEmail.getText().toString().trim();
        String name = etUsername.getText().toString().trim();

        if (email.isEmpty()) { showTopRightToast("Enter email"); return; }
        if (name.isEmpty()) { showTopRightToast("Enter username"); return; }

        Call<RegisterResponse> call = api.registerUser(new RegisterRequest(email, name));
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    otpContainer.setVisibility(android.view.View.VISIBLE);
                    otpSent = true;
                    startOtpCountdown();

                    // 🔥 SUCCESS TOAST FROM BACKEND MESSAGE
                    showTopRightToast(response.body().getMessage());

                } else {
                    // 🔥 ERROR TOAST FROM BACKEND
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : null;
                        if (err != null) {
                            JSONObject obj = new JSONObject(err);
                            showTopRightToast(obj.optString("message", "Failed to send OTP!"));
                        } else {
                            showTopRightToast("Failed to send OTP!");
                        }
                    } catch (Exception e) {
                        showTopRightToast("Failed to send OTP!");
                    }
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                showTopRightToast("Network Error!");
            }
        });
    }

    // ----------------------------------------------------
    // VERIFY OTP + REGISTER
    // ----------------------------------------------------
    private void verifyOtpAndRegister() {

        if (!otpSent) { showTopRightToast("Send OTP first"); return; }

        String otp =
                otp1.getText().toString() + otp2.getText().toString() +
                        otp3.getText().toString() + otp4.getText().toString() +
                        otp5.getText().toString() + otp6.getText().toString();

        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (otp.length() != 6) { showTopRightToast("Invalid OTP!"); return; }
        if (pass.length() < 8) { showTopRightToast("Password must be 8+ characters"); return; }

        Call<AuthResponse> call = api.verifyOtp(new VerifyOtpRequest(email, otp, pass));
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    // 🔥 SUCCESS MESSAGE FROM BACKEND
                    showTopRightToast(response.body().getMessage());

                    startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                    finish();

                } else {
                    // 🔥 ERROR MESSAGE FROM BACKEND
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : null;
                        if (err != null) {
                            JSONObject obj = new JSONObject(err);
                            showTopRightToast(obj.optString("message", "Check Otp!"));
                        } else {
                            showTopRightToast("Check Otp!");
                        }
                    } catch (Exception e) {
                        showTopRightToast("Check Otp!");
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showTopRightToast("Network Error!");
            }
        });
    }

    // OTP TIMER
    private void startOtpCountdown() {
        timer = new CountDownTimer(timeLeft, 1000) {
            @Override public void onTick(long ms) {
                txtSendOtp.setText("Resend in " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                txtSendOtp.setText("Resend OTP");
                btnSendOtp.setEnabled(true);
            }
        }.start();
    }

    // AUTO MOVE OTP FOCUS
    private void setupOtpAutoMove() {
        EditText[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < boxes.length - 1)
                        boxes[index + 1].requestFocus();
                    else if (s.length() == 0 && index > 0)
                        boxes[index - 1].requestFocus();
                }
            });
        }
    }
}