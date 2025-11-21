package com.booyahx;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    // OLD FIELDS (same IDs you had)
    EditText etForgotEmail;
    TextView btnSendOtp, btnReset;
    LinearLayout otpContainer;
    EditText otp1, otp2, otp3, otp4, otp5, otp6;

    // NEW FIELDS
    TextView btnVerifyOtp;
    LinearLayout passwordContainer;
    EditText etNewPassword, etConfirmPassword;
    ImageView eyeNew, eyeConfirm;

    CountDownTimer timer;
    long timeLeft = 60000;   // 60 seconds
    boolean otpSent = false;
    boolean otpVerified = false;
    boolean showPassNew = false;
    boolean showPassConfirm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupOtpAutoMove();
        setupEyeButtons();

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void initViews() {
        etForgotEmail = findViewById(R.id.etForgotEmail);
        btnSendOtp    = findViewById(R.id.btnSendOtp);
        btnReset      = findViewById(R.id.btnReset);

        otpContainer  = findViewById(R.id.otpContainer);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);

        // new
        btnVerifyOtp       = findViewById(R.id.btnVerifyOtp);
        passwordContainer  = findViewById(R.id.passwordContainer);
        etNewPassword      = findViewById(R.id.etNewPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        eyeNew             = findViewById(R.id.eyeNew);
        eyeConfirm         = findViewById(R.id.eyeConfirm);
    }

    // SEND OTP FUNCTION
    private void sendOtp() {

        String email = etForgotEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        // simple email validation
        if (!email.contains("@") || !email.contains(".")) {
            Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        otpSent = true;
        otpVerified = false;
        passwordContainer.setVisibility(View.GONE);  // hide passwords until OTP verified

        otpContainer.setVisibility(View.VISIBLE);
        clearOtpBoxes();

        startOtpCountdown();
        Toast.makeText(this, "OTP sent (Demo)", Toast.LENGTH_SHORT).show();
    }

    // VERIFY OTP BUTTON CLICK
    private void verifyOtp() {

        if (!otpSent) {
            Toast.makeText(this, "Send OTP first", Toast.LENGTH_SHORT).show();
            return;
        }

        String otp = otp1.getText().toString() +
                otp2.getText().toString() +
                otp3.getText().toString() +
                otp4.getText().toString() +
                otp5.getText().toString() +
                otp6.getText().toString();

        if (otp.length() != 6) {
            Toast.makeText(this, "Enter full 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // DEMO: always success if 6 digits
        otpVerified = true;
        passwordContainer.setVisibility(View.VISIBLE);
        btnSendOtp.setText("Verified");

        Toast.makeText(this, "OTP Verified! (Demo)", Toast.LENGTH_SHORT).show();
    }

    // RESET PASSWORD BUTTON CLICK
    private void resetPassword() {

        if (!otpVerified) {
            Toast.makeText(this, "Verify OTP first", Toast.LENGTH_SHORT).show();
            return;
        }

        String newPass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (newPass.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Password Reset Successful (Demo)", Toast.LENGTH_LONG).show();
    }

    // OTP COUNTDOWN
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

    // AUTO MOVE BETWEEN OTP BOXES
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

    private void clearOtpBoxes() {
        otp1.setText("");
        otp2.setText("");
        otp3.setText("");
        otp4.setText("");
        otp5.setText("");
        otp6.setText("");
        otp1.requestFocus();
    }

    // EYE BUTTONS
    private void setupEyeButtons() {

        eyeNew.setOnClickListener(v -> {
            showPassNew = !showPassNew;
            if (showPassNew) {
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etNewPassword.setSelection(etNewPassword.getText().length());
        });

        eyeConfirm.setOnClickListener(v -> {
            showPassConfirm = !showPassConfirm;
            if (showPassConfirm) {
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });
    }
}