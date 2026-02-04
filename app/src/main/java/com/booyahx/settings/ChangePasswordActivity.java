package com.booyahx.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.LoaderOverlay;
import com.booyahx.R;
import com.booyahx.TokenManager;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ChangePasswordRequest;
import com.booyahx.network.models.SimpleResponse;
import com.booyahx.LoginUsernameActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    EditText edtOldPassword, edtNewPassword, edtConfirmPassword;
    TextView btnChangePassword;
    ImageView btnBack;

    // Password requirement indicators
    LinearLayout passwordRequirements, passwordMatchContainer;
    TextView checkMinLength, txtMinLength;
    TextView checkUppercase, txtUppercase;
    TextView checkLowercase, txtLowercase;
    TextView checkNumber, txtNumber;
    TextView checkSymbol, txtSymbol;
    TextView checkPasswordMatch, txtPasswordMatch;

    // Password requirement states
    private boolean hasMinLength = false;
    private boolean hasUppercase = false;
    private boolean hasLowercase = false;
    private boolean hasNumber = false;
    private boolean hasSymbol = false;
    private boolean passwordsMatch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Password requirement views
        passwordRequirements = findViewById(R.id.passwordRequirements);
        passwordMatchContainer = findViewById(R.id.passwordMatchContainer);

        checkMinLength = findViewById(R.id.checkMinLength);
        txtMinLength = findViewById(R.id.txtMinLength);

        checkUppercase = findViewById(R.id.checkUppercase);
        txtUppercase = findViewById(R.id.txtUppercase);

        checkLowercase = findViewById(R.id.checkLowercase);
        txtLowercase = findViewById(R.id.txtLowercase);

        checkNumber = findViewById(R.id.checkNumber);
        txtNumber = findViewById(R.id.txtNumber);

        checkSymbol = findViewById(R.id.checkSymbol);
        txtSymbol = findViewById(R.id.txtSymbol);

        checkPasswordMatch = findViewById(R.id.checkPasswordMatch);
        txtPasswordMatch = findViewById(R.id.txtPasswordMatch);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnChangePassword.setOnClickListener(v -> validateAndSend());

        // Real-time password strength checker for new password
        edtNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();

                if (password.isEmpty()) {
                    passwordRequirements.setVisibility(View.GONE);
                } else {
                    passwordRequirements.setVisibility(View.VISIBLE);
                    checkPasswordStrength(password);
                }

                // Also check if passwords match when new password changes
                checkPasswordsMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Real-time password match checker for confirm password
        edtConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordsMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkPasswordStrength(String password) {
        // Check minimum length
        hasMinLength = password.length() >= 8;
        updateRequirement(checkMinLength, txtMinLength, hasMinLength);

        // Check uppercase
        hasUppercase = password.matches(".*[A-Z].*");
        updateRequirement(checkUppercase, txtUppercase, hasUppercase);

        // Check lowercase
        hasLowercase = password.matches(".*[a-z].*");
        updateRequirement(checkLowercase, txtLowercase, hasLowercase);

        // Check number
        hasNumber = password.matches(".*[0-9].*");
        updateRequirement(checkNumber, txtNumber, hasNumber);

        // Check special character
        hasSymbol = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        updateRequirement(checkSymbol, txtSymbol, hasSymbol);
    }

    private void updateRequirement(TextView checkView, TextView textView, boolean isMet) {
        if (isMet) {
            // ✓ Checkmark in neon green when requirement is met
            checkView.setText("✓");
            checkView.setTextColor(0xFF00FF88);
            textView.setTextColor(0xFF00FF88);
        } else {
            // ○ Circle in gray when requirement is not met
            checkView.setText("○");
            checkView.setTextColor(0xFF666666);
            textView.setTextColor(0xFF999999);
        }
    }

    private void checkPasswordsMatch() {
        String newPassword = edtNewPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (confirmPassword.isEmpty()) {
            passwordMatchContainer.setVisibility(View.GONE);
            passwordsMatch = false;
            return;
        }

        passwordMatchContainer.setVisibility(View.VISIBLE);

        if (newPassword.equals(confirmPassword)) {
            passwordsMatch = true;
            txtPasswordMatch.setText("Passwords match");
            checkPasswordMatch.setText("✓");
            checkPasswordMatch.setTextColor(0xFF00FF88);
            txtPasswordMatch.setTextColor(0xFF00FF88);
        } else {
            passwordsMatch = false;
            txtPasswordMatch.setText("Passwords do not match");
            checkPasswordMatch.setText("○");
            checkPasswordMatch.setTextColor(0xFFFF4444);
            txtPasswordMatch.setTextColor(0xFFFF4444);
        }
    }

    private void validateAndSend() {
        String oldP = edtOldPassword.getText().toString().trim();
        String newP = edtNewPassword.getText().toString().trim();
        String confirmP = edtConfirmPassword.getText().toString().trim();

        if (oldP.isEmpty()) {
            showToast("Enter old password");
            return;
        }

        if (newP.isEmpty()) {
            showToast("Enter new password");
            return;
        }

        // Check all password requirements
        if (!hasMinLength) {
            showToast("Password must be at least 8 characters");
            return;
        }

        if (!hasUppercase) {
            showToast("Password must contain an uppercase letter");
            return;
        }

        if (!hasLowercase) {
            showToast("Password must contain a lowercase letter");
            return;
        }

        if (!hasNumber) {
            showToast("Password must contain a number");
            return;
        }

        if (!hasSymbol) {
            showToast("Password must contain a special character");
            return;
        }

        if (newP.equals(oldP)) {
            showToast("New password cannot be same as old password");
            return;
        }

        if (!newP.equals(confirmP)) {
            showToast("Passwords must match");
            return;
        }

        sendChangePassword(oldP, newP, confirmP);
    }

    private void sendChangePassword(String oldP, String newP, String confirmP) {
        String accessToken = TokenManager.getAccessToken(this);

        if (accessToken == null) {
            showToast("Session expired. Please login again.");
            redirectToLogin();
            return;
        }

        LoaderOverlay.show(this);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        ChangePasswordRequest req = new ChangePasswordRequest(oldP, newP, confirmP);

        api.changePassword(req).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                LoaderOverlay.hide(ChangePasswordActivity.this);

                if (response.isSuccessful() && response.body() != null) {
                    showToast(response.body().getMessage());
                    TokenManager.logout(ChangePasswordActivity.this);
                    redirectToLogin();
                } else {
                    String errorMsg = "Failed to change password";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    } else if (response.code() == 401) {
                        errorMsg = "Authentication failed. Please login again.";
                    } else if (response.code() == 400) {
                        errorMsg = "Invalid password. Please check and try again.";
                    }
                    showToast(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                LoaderOverlay.hide(ChangePasswordActivity.this);
                showToast("Error: " + t.getMessage());
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ChangePasswordActivity.this, LoginUsernameActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(40, 25, 40, 25);
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackgroundResource(R.drawable.toast_bg);
        tv.setTextSize(14);

        android.widget.Toast toast = new android.widget.Toast(getApplicationContext());
        toast.setView(tv);
        toast.setDuration(android.widget.Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.END, 40, 120);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(350);
        tv.startAnimation(fade);

        toast.show();
    }
}