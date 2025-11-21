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

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etPassword;
    TextView btnSendOtp, btnRegister, txtGoLogin;
    LinearLayout otpContainer;
    EditText otp1, otp2, otp3, otp4, otp5, otp6;

    boolean otpSent = false;
    CountDownTimer timer;
    long timeLeft = 60000; // 60 seconds timer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        initViews();
        setupOtpAutoMove();

        btnSendOtp.setOnClickListener(v -> sendOtp());

        btnRegister.setOnClickListener(v -> attemptRegister());

        txtGoLogin.setOnClickListener(v ->
                finish() // you will return to LoginUsernameActivity
        );
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

    // SEND OTP
    private void sendOtp() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        otpContainer.setVisibility(View.VISIBLE);
        otpSent = true;

        startOtpCountdown();

        Toast.makeText(this, "OTP sent to your email (Demo)", Toast.LENGTH_SHORT).show();
    }

    // TIMER
    private void startOtpCountdown() {

        btnSendOtp.setEnabled(false);

        timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long ms) {
                timeLeft = ms;
                long sec = ms / 1000;
                btnSendOtp.setText("Resend OTP in " + sec + "s");
            }

            @Override
            public void onFinish() {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Resend OTP");
                timeLeft = 60000;
            }
        }.start();
    }

    // OTP AUTO MOVE
    private void setupOtpAutoMove() {
        EditText[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                    if (s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                    else if (s.length() == 0 && index > 0) {
                        boxes[index - 1].requestFocus();
                    }
                }
            });
        }
    }

    // REGISTER VALIDATION
    private void attemptRegister() {

        if (!otpSent) {
            Toast.makeText(this, "Please verify email first", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = etUsername.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String pass     = etPassword.getText().toString().trim();

        String otp = otp1.getText().toString() +
                otp2.getText().toString() +
                otp3.getText().toString() +
                otp4.getText().toString() +
                otp5.getText().toString() +
                otp6.getText().toString();

        if (username.isEmpty()) {
            Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.isEmpty()) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp.length() != 6) {
            Toast.makeText(this, "Enter valid OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Registration Successful (Demo)", Toast.LENGTH_LONG).show();
    }
}