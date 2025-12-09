package com.booyahx;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ForgotPasswordRequest;
import com.booyahx.network.models.ResetPasswordRequest;
import com.booyahx.network.models.SimpleResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText etForgotEmail;
    EditText otp1, otp2, otp3, otp4, otp5, otp6;
    EditText etNewPassword, etConfirmPassword;

    TextView btnSendOtp, btnVerifyOtp, btnReset;
    ImageView eyeNew, eyeConfirm;

    LinearLayout otpContainer, passwordContainer;

    boolean otpSent = false;
    boolean showPassNew = false;

    CountDownTimer timer;
    long timeLeft = 60000;

    ApiService api;

    private static final String PREFS = "APP_PREFS";
    private static final String KEY_OTP_BLOCK_UNTIL = "otp_block_until";

    private CountDownTimer localBlockTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        api = ApiClient.getClient(this).create(ApiService.class);

        initViews();
        setupOtpAutoMove();
        setupEyeButtons();

        passwordContainer.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);
        btnVerifyOtp.setVisibility(View.GONE);

        etConfirmPassword.setVisibility(View.GONE);
        eyeConfirm.setVisibility(View.GONE);
        hideConfirmPasswordRow();

        long blockUntil = getBlockUntilMillis();
        long now = System.currentTimeMillis();
        if (blockUntil > now) {
            startLocalBlockCountdown(blockUntil - now);
        }

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void initViews() {
        etForgotEmail = findViewById(R.id.etForgotEmail);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnReset = findViewById(R.id.btnReset);

        otpContainer = findViewById(R.id.otpContainer);
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);

        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        passwordContainer = findViewById(R.id.passwordContainer);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        eyeNew = findViewById(R.id.eyeNew);
        eyeConfirm = findViewById(R.id.eyeConfirm);
    }

    // ------------------------------------------------------------
    //  Custom Neon Toast
    // ------------------------------------------------------------
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

    // ------------------------------------------------------------
    //  SEND OTP WITH LOADER
    // ------------------------------------------------------------
    private void sendOtp() {

        LoaderOverlay.show(this);

        String email = etForgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            LoaderOverlay.hide(this);
            showTopRightToast("Enter email");
            return;
        }

        Call<SimpleResponse> call = api.forgotPassword(new ForgotPasswordRequest(email));
        call.enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {

                LoaderOverlay.hide(ForgotPasswordActivity.this);

                if (response.isSuccessful() && response.body() != null) {

                    showTopRightToast(response.body().getMessage());

                    if (response.body().isSuccess()) {
                        otpSent = true;

                        otpContainer.setVisibility(View.VISIBLE);
                        passwordContainer.setVisibility(View.VISIBLE);
                        btnReset.setVisibility(View.VISIBLE);

                        clearOtpBoxes();
                        startOtpCountdown();
                    }

                    return;
                }

                try {
                    if (response.errorBody() != null) {
                        JSONObject obj = new JSONObject(response.errorBody().string());
                        showTopRightToast(obj.optString("message"));
                    }
                } catch (Exception ignored) {
                    showTopRightToast("Error");
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                LoaderOverlay.hide(ForgotPasswordActivity.this);
                showTopRightToast(t.getMessage());
            }
        });
    }

    // ------------------------------------------------------------
    //  RESET PASSWORD WITH LOADER
    // ------------------------------------------------------------
    private void resetPassword() {

        LoaderOverlay.show(this);

        if (!otpSent) {
            LoaderOverlay.hide(this);
            showTopRightToast("Send OTP first");
            return;
        }

        String otp = otp1.getText().toString() +
                otp2.getText().toString() +
                otp3.getText().toString() +
                otp4.getText().toString() +
                otp5.getText().toString() +
                otp6.getText().toString();

        if (otp.length() != 6) {
            LoaderOverlay.hide(this);
            showTopRightToast("Enter full OTP");
            return;
        }

        String newPass = etNewPassword.getText().toString().trim();
        if (newPass.length() < 8) {
            LoaderOverlay.hide(this);
            showTopRightToast("Password must be 8+ characters");
            return;
        }

        ResetPasswordRequest req = new ResetPasswordRequest(
                etForgotEmail.getText().toString().trim(),
                otp,
                newPass
        );

        Call<SimpleResponse> call = api.resetPassword(req);
        call.enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {

                LoaderOverlay.hide(ForgotPasswordActivity.this);

                if (response.isSuccessful() && response.body() != null) {

                    showTopRightToast(response.body().getMessage());

                    if (response.body().isSuccess()) {
                        startActivity(new Intent(ForgotPasswordActivity.this, LoginUsernameActivity.class));
                        finish();
                    }
                    return;
                }

                try {
                    if (response.errorBody() != null) {
                        JSONObject obj = new JSONObject(response.errorBody().string());
                        showTopRightToast(obj.optString("message"));
                    }
                } catch (Exception e) {
                    showTopRightToast("Failed");
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                LoaderOverlay.hide(ForgotPasswordActivity.this);
                showTopRightToast(t.getMessage());
            }
        });
    }

    // ------------------------------------------------------------
    //  OTP TIMER + HELPERS
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

    private void setupOtpAutoMove() {
        EditText[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < boxes.length; i++) {
            int index = i;

            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
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

    private void clearOtpBoxes() {
        otp1.setText(""); otp2.setText(""); otp3.setText("");
        otp4.setText(""); otp5.setText(""); otp6.setText("");
        otp1.requestFocus();
    }

    private void setupEyeButtons() {
        eyeNew.setOnClickListener(v -> {
            showPassNew = !showPassNew;

            if (showPassNew) {
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                eyeNew.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye_on));

            } else {
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                eyeNew.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_eye_off));
            }

            etNewPassword.setSelection(etNewPassword.getText().length());
        });
    }

    private void hideConfirmPasswordRow() {
        if (passwordContainer.getChildCount() > 1) {
            View confirmRow = passwordContainer.getChildAt(1);
            confirmRow.setVisibility(View.GONE);
        }
    }

    private long getBlockUntilMillis() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getLong(KEY_OTP_BLOCK_UNTIL, 0L);
    }

    private void startLocalBlockCountdown(long millis) {

        if (localBlockTimer != null)
            localBlockTimer.cancel();

        btnSendOtp.setEnabled(false);

        localBlockTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long msLeft) {
                btnSendOtp.setText("Resend in " + (msLeft / 1000) + "s");
            }

            @Override
            public void onFinish() {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Send OTP");
            }
        }.start();
    }
}