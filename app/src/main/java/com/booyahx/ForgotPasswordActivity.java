package com.booyahx;

import android.content.Intent;
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

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ForgotPasswordRequest;
import com.booyahx.network.models.ResetPasswordRequest;
import com.booyahx.network.models.SimpleResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    // INPUTS
    EditText etForgotEmail;
    EditText otp1, otp2, otp3, otp4, otp5, otp6;
    EditText etNewPassword, etConfirmPassword;

    // BUTTONS
    TextView btnSendOtp, btnVerifyOtp, btnReset;
    ImageView eyeNew, eyeConfirm;

    // LAYOUTS
    LinearLayout otpContainer, passwordContainer;

    // FLAGS
    boolean otpSent = false;
    boolean otpVerified = false;
    boolean showPassNew = false;
    boolean showPassConfirm = false;

    // TIMER
    CountDownTimer timer;
    long timeLeft = 60000; // 60 seconds

    // API
    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        api = ApiClient.getClient().create(ApiService.class);

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

        btnVerifyOtp       = findViewById(R.id.btnVerifyOtp);
        passwordContainer  = findViewById(R.id.passwordContainer);
        etNewPassword      = findViewById(R.id.etNewPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        eyeNew             = findViewById(R.id.eyeNew);
        eyeConfirm         = findViewById(R.id.eyeConfirm);
    }

    // ------------------------------------------------------------
    //  🔹 SEND OTP WITH LOADING
    // ------------------------------------------------------------
    private void sendOtp() {
        String email = etForgotEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        startLoading(btnSendOtp);

        Call<SimpleResponse> call = api.forgotPassword(new ForgotPasswordRequest(email));
        call.enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {

                stopLoading(btnSendOtp, "Send OTP");

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    otpSent = true;
                    otpContainer.setVisibility(View.VISIBLE);
                    passwordContainer.setVisibility(View.GONE);
                    clearOtpBoxes();
                    startOtpCountdown();
                    Toast.makeText(ForgotPasswordActivity.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Failed! Check email", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                stopLoading(btnSendOtp, "Send OTP");
                Toast.makeText(ForgotPasswordActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------------------------------------------------
    //  🔸 VERIFY OTP (UI only)
    // ------------------------------------------------------------
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
            Toast.makeText(this, "Enter full OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        startLoading(btnVerifyOtp);

        // Delay to simulate server verification smoothness
        btnVerifyOtp.postDelayed(() -> {
            stopLoading(btnVerifyOtp, "Verify OTP");

            otpVerified = true;
            passwordContainer.setVisibility(View.VISIBLE);
            btnSendOtp.setText("Verified");

            Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show();
        }, 600);
    }

    // ------------------------------------------------------------
    //  🟢 RESET PASSWORD API WITH LOADER
    // ------------------------------------------------------------
    private void resetPassword() {

        if (!otpVerified) {
            Toast.makeText(this, "Verify OTP first", Toast.LENGTH_SHORT).show();
            return;
        }

        String newPass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (newPass.length() < 8) {
            Toast.makeText(this, "Password must be 8+ characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullOtp = otp1.getText().toString() +
                otp2.getText().toString() +
                otp3.getText().toString() +
                otp4.getText().toString() +
                otp5.getText().toString() +
                otp6.getText().toString();

        ResetPasswordRequest req = new ResetPasswordRequest(
                etForgotEmail.getText().toString().trim(),
                fullOtp,
                newPass
        );

        startLoading(btnReset);

        Call<SimpleResponse> call = api.resetPassword(req);

        call.enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {

                stopLoading(btnReset, "Reset Password");

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Password Reset Successful 🎉", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(ForgotPasswordActivity.this, LoginUsernameActivity.class));
                    finish();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Invalid OTP or Email!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                stopLoading(btnReset, "Reset Password");
                Toast.makeText(ForgotPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ------------------------------------------------------------
    //  ⏱ OTP TIMER
    // ------------------------------------------------------------
    private void startOtpCountdown() {

        btnSendOtp.setEnabled(false);

        timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long ms) {
                timeLeft = ms;
                btnSendOtp.setText("Resend in " + (ms / 1000) + "s");
            }

            @Override
            public void onFinish() {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Resend OTP");
                timeLeft = 60000;
            }
        }.start();
    }

    // ------------------------------------------------------------
    //  OTP AUTOFILL MOVEMENT
    // ------------------------------------------------------------
    private void setupOtpAutoMove() {
        EditText[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < boxes.length - 1) boxes[index + 1].requestFocus();
                    else if (s.length() == 0 && index > 0) boxes[index - 1].requestFocus();
                }
            });
        }
    }

    private void clearOtpBoxes() {
        otp1.setText(""); otp2.setText(""); otp3.setText("");
        otp4.setText(""); otp5.setText(""); otp6.setText("");
        otp1.requestFocus();
    }

    // ------------------------------------------------------------
    //  👁 PASSWORD EYE TOGGLE
    // ------------------------------------------------------------
    private void setupEyeButtons() {

        eyeNew.setOnClickListener(v -> {
            showPassNew = !showPassNew;
            if (showPassNew)
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            else
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            etNewPassword.setSelection(etNewPassword.getText().length());
            eyeNew.setSelected(showPassNew);
        });

        eyeConfirm.setOnClickListener(v -> {
            showPassConfirm = !showPassConfirm;
            if (showPassConfirm)
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            else
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            eyeConfirm.setSelected(showPassConfirm);
        });
    }

    // ------------------------------------------------------------
    //  ⭐ BUTTON LOADING SYSTEM (NO UI CHANGES)
    // ------------------------------------------------------------
    private void startLoading(TextView btn) {
        btn.setEnabled(false);
        btn.setAlpha(0.5f);
    }

    private void stopLoading(TextView btn, String label) {
        btn.setEnabled(true);
        btn.setAlpha(1f);
        btn.setText(label);
    }
}