package com.booyahx;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etPassword;
    RelativeLayout btnSendOtp, btnRegister;
    TextView txtSendOtp, txtRegister, txtGoLogin;
    ProgressBar loaderSendOtp, loaderRegister;

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

    // 🌐 SEND OTP
    private void sendOtp() {
        String email = etEmail.getText().toString().trim();
        String name = etUsername.getText().toString().trim();

        if (email.isEmpty()) { Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show(); return; }
        if (name.isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return; }

        startLoading(btnSendOtp, txtSendOtp, loaderSendOtp);

        Call<RegisterResponse> call = api.registerUser(new RegisterRequest(email, name));
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                stopLoading(btnSendOtp, txtSendOtp, loaderSendOtp, "Send OTP");

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    otpContainer.setVisibility(View.VISIBLE);
                    otpSent = true;
                    startOtpCountdown();
                    Toast.makeText(RegisterActivity.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Failed to send OTP!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                stopLoading(btnSendOtp, txtSendOtp, loaderSendOtp, "Send OTP");
                Toast.makeText(RegisterActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // VERIFY OTP + REGISTER
    private void verifyOtpAndRegister() {

        if (!otpSent) { Toast.makeText(this, "Send OTP first", Toast.LENGTH_SHORT).show(); return; }

        String otp =
                otp1.getText().toString() + otp2.getText().toString() +
                        otp3.getText().toString() + otp4.getText().toString() +
                        otp5.getText().toString() + otp6.getText().toString();

        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (otp.length() != 6) { Toast.makeText(this, "Invalid OTP!", Toast.LENGTH_SHORT).show(); return; }
        if (pass.length() < 8) { Toast.makeText(this, "Password must be 8+ characters", Toast.LENGTH_SHORT).show(); return; }

        startLoading(btnRegister, txtRegister, loaderRegister);

        Call<AuthResponse> call = api.verifyOtp(new VerifyOtpRequest(email, otp, pass));
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                stopLoading(btnRegister, txtRegister, loaderRegister, "Register");

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(RegisterActivity.this, "Registration Successful 🎉", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Wrong OTP!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                stopLoading(btnRegister, txtRegister, loaderRegister, "Register");
                Toast.makeText(RegisterActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // LOADING SYSTEM
    private void startLoading(RelativeLayout btn, TextView txt, ProgressBar loader) {
        btn.setEnabled(false);
        txt.setVisibility(View.INVISIBLE);
        loader.setVisibility(View.VISIBLE);
    }

    private void stopLoading(RelativeLayout btn, TextView txt, ProgressBar loader, String label) {
        btn.setEnabled(true);
        txt.setText(label);
        txt.setVisibility(View.VISIBLE);
        loader.setVisibility(View.GONE);
    }

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

    private void setupOtpAutoMove() {
        EditText[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < boxes.length - 1)
                        boxes[index + 1].requestFocus();
                    else if (s.length() == 0 && index > 0)
                        boxes[index - 1].requestFocus();
                }
            });
        }
    }
}