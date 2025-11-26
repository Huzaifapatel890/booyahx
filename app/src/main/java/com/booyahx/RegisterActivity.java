package com.booyahx;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.RegisterRequest;
import com.booyahx.network.models.RegisterResponse;
import com.booyahx.network.models.VerifyOtpRequest;
import com.booyahx.network.models.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etPassword;
    TextView btnSendOtp, btnRegister, txtGoLogin;
    LinearLayout otpContainer;
    EditText otp1, otp2, otp3, otp4, otp5, otp6;

    boolean otpSent = false;
    CountDownTimer timer;
    long timeLeft = 60000; // 60 sec
    ApiService api; // API OBJECT

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        api = ApiClient.getClient().create(ApiService.class); // INIT API

        initViews();
        setupOtpAutoMove();

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnRegister.setOnClickListener(v -> verifyOtpAndRegister());
        txtGoLogin.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etUsername   = findViewById(R.id.etRegUsername);
        etEmail      = findViewById(R.id.etRegEmail);
        etPassword   = findViewById(R.id.etRegPassword);

        btnSendOtp   = findViewById(R.id.btnSendOtp);
        btnRegister  = findViewById(R.id.btnRegister);
        txtGoLogin   = findViewById(R.id.txtGoLogin);

        otpContainer = findViewById(R.id.otpContainer);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
    }

    // 🔵 SEND OTP USING BACKEND
    private void sendOtp() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        if (email.isEmpty()) { Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show(); return; }
        if (username.isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return; }

        btnSendOtp.setEnabled(false);

        Call<RegisterResponse> call = api.registerUser(new RegisterRequest(email, username));
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                btnSendOtp.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    otpContainer.setVisibility(View.VISIBLE);
                    otpSent = true;
                    startOtpCountdown();
                    Toast.makeText(RegisterActivity.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Email already exists!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                btnSendOtp.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🟢 VERIFY OTP + PASSWORD
    private void verifyOtpAndRegister() {

        if (!otpSent) { Toast.makeText(this, "Send OTP first", Toast.LENGTH_SHORT).show(); return; }

        String otp = otp1.getText().toString()+otp2.getText().toString()+otp3.getText().toString()+
                otp4.getText().toString()+otp5.getText().toString()+otp6.getText().toString();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (otp.length() != 6) { Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show(); return; }
        if (pass.length() < 8) { Toast.makeText(this, "Password must be 8+ characters", Toast.LENGTH_SHORT).show(); return;}

        Call<AuthResponse> call = api.verifyOtp(new VerifyOtpRequest(email, otp, pass));
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(RegisterActivity.this, "Registration Successful 🎉", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Wrong OTP!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOtpCountdown() {
        btnSendOtp.setEnabled(false);
        timer = new CountDownTimer(timeLeft, 1000) {
            @Override public void onTick(long ms) { btnSendOtp.setText("Resend in " + ms/1000 + "s"); }
            @Override public void onFinish() { btnSendOtp.setEnabled(true); btnSendOtp.setText("Resend OTP"); }
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
                    if (s.length()==1 && index < boxes.length - 1) boxes[index+1].requestFocus();
                    else if (s.length()==0 && index > 0) boxes[index-1].requestFocus();
                }
            });
        }
    }
}