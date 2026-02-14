package com.booyahx.payment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.airbnb.lottie.LottieAnimationView;
import com.booyahx.ProfileCacheManager;
import com.booyahx.R;
import com.booyahx.network.ApiService;
import com.booyahx.network.ApiClient;
import com.booyahx.network.models.WithdrawalRequest;
import com.booyahx.network.models.WithdrawalResponse;
import com.booyahx.settings.EditProfileActivity;
import com.booyahx.WalletCacheManager;
import com.booyahx.WalletLimitCache;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WithdrawDialog extends Dialog {

    private static final String TAG = "WithdrawDialog";

    private Context context;

    // Balance info
    private TextView tvCurrentBalance;
    private TextView tvWithdrawableBalance;

    // UPI section
    private EditText etUpiId;
    private LinearLayout layoutUpiWarning;
    private TextView tvUpdateProfileWarning;

    // Amount section
    private EditText etAmount;
    private TextView tvAmountError;

    // Buttons (now TextViews)
    private TextView btnWithdraw;

    // Success screen
    private LinearLayout layoutForm;
    private LinearLayout layoutSuccess;
    private LottieAnimationView animCheck;
    private TextView tvSuccessAmount;
    private TextView btnDone;

    // FIX 2: Error screen
    private LinearLayout layoutError;

    private double currentBalance;
    private int maxWithdrawableGC;
    private String upiId;

    // Withdrawal limits
    private static final int MIN_WITHDRAW = 40;
    private static final int MAX_WITHDRAW = 9999;

    // API
    private ApiService apiService;
    private ProgressDialog progressDialog;

    public WithdrawDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_withdraw);

        // Make dialog background transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set dialog to match parent width with small margins
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        initViews();
        initApi();
        loadData();
        setupListeners();
        loadCachedWithdrawLimit();
    }

    private void initViews() {
        // Balance section
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvWithdrawableBalance = findViewById(R.id.tvWithdrawableBalance);

        // UPI section
        etUpiId = findViewById(R.id.etUpiId);
        layoutUpiWarning = findViewById(R.id.layoutUpiWarning);
        tvUpdateProfileWarning = findViewById(R.id.tvUpdateProfileWarning);

        // Amount section
        etAmount = findViewById(R.id.etAmount);
        tvAmountError = findViewById(R.id.tvAmountError);

        // Button (TextView)
        btnWithdraw = findViewById(R.id.btnWithdraw);

        // Success screen
        layoutForm = findViewById(R.id.layoutForm);
        layoutSuccess = findViewById(R.id.layoutSuccess);
        animCheck = findViewById(R.id.animCheck);
        tvSuccessAmount = findViewById(R.id.tvSuccessAmount);
        btnDone = findViewById(R.id.btnDone);

        // FIX 2: Error screen
        layoutError = findViewById(R.id.layoutError);
    }

    private void initApi() {
        // Initialize ApiService using ApiClient with context for AuthInterceptor
        apiService = ApiClient.getClient(context).create(ApiService.class);

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);
    }

    private void loadData() {
        // Load balance from WalletCacheManager
        currentBalance = WalletCacheManager.getBalance(context);
        int balanceInt = (int) Math.round(currentBalance);

        tvCurrentBalance.setText(balanceInt + " GC");

        // Load UPI ID from ProfileCacheManager
        upiId = ProfileCacheManager.getProfile(context) != null
                ? ProfileCacheManager.getProfile(context).paymentUPI
                : null;

        if (upiId != null && !upiId.isEmpty()) {
            // UPI ID exists - show it and make it non-editable
            etUpiId.setText(upiId);
            etUpiId.setEnabled(false);
            layoutUpiWarning.setVisibility(View.GONE);
        } else {
            // No UPI ID - show warning
            etUpiId.setText("");
            etUpiId.setEnabled(false);
            layoutUpiWarning.setVisibility(View.VISIBLE);
            btnWithdraw.setEnabled(false);
            btnWithdraw.setAlpha(0.5f);
            setButtonGrayStyle();
        }
    }

    private void loadCachedWithdrawLimit() {
        // Check if cached limit is available
        if (WalletLimitCache.isLimitAvailable(context)) {
            // Load from cache
            maxWithdrawableGC = WalletLimitCache.getMaxWithdrawableGC(context);
            int balanceGC = WalletLimitCache.getBalanceGC(context);

            // Update UI with cached data
            tvCurrentBalance.setText(balanceGC + " GC");
            tvWithdrawableBalance.setText(maxWithdrawableGC + " GC");
            currentBalance = balanceGC;

            // Show warning if below minimum
            if (maxWithdrawableGC < MIN_WITHDRAW) {
                btnWithdraw.setEnabled(false);
                btnWithdraw.setAlpha(0.5f);
                setButtonGrayStyle();
            } else if (maxWithdrawableGC == 0) {
                btnWithdraw.setEnabled(false);
                btnWithdraw.setAlpha(0.5f);
                setButtonGrayStyle();
            }

            Log.d(TAG, "Loaded cached withdrawal limit: " + maxWithdrawableGC + " GC");
        } else {
            // FIX 2: Show error screen when no cache data
            showErrorScreen();
            Log.e(TAG, "Withdrawal limit not available in cache");
        }
    }

    // FIX 2: Show error screen
    private void showErrorScreen() {
        layoutForm.setVisibility(View.GONE);
        layoutSuccess.setVisibility(View.GONE);
        if (layoutError != null) {
            layoutError.setVisibility(View.VISIBLE);
        }
    }

    private void setButtonGrayStyle() {
        GradientDrawable grayDrawable = new GradientDrawable();
        grayDrawable.setColor(Color.parseColor("#3a3a3a"));
        grayDrawable.setStroke(2, Color.parseColor("#5a5a5a"));
        grayDrawable.setCornerRadius(12);
        btnWithdraw.setBackground(grayDrawable);
    }

    private void setButtonNeonStyle() {
        btnWithdraw.setBackgroundResource(R.drawable.neon_button);
    }

    private void setupListeners() {
        // Amount validation
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAmount(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Withdraw button (TextView)
        btnWithdraw.setOnClickListener(v -> processWithdrawal());

        // Make entire warning layout clickable
        layoutUpiWarning.setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(context, EditProfileActivity.class);
            context.startActivity(intent);
        });

        // Done button (TextView)
        btnDone.setOnClickListener(v -> dismiss());
    }

    private void validateAmount(String amountStr) {
        if (amountStr.isEmpty()) {
            tvAmountError.setVisibility(View.GONE);
            btnWithdraw.setEnabled(false);
            btnWithdraw.setAlpha(0.5f);
            setButtonGrayStyle();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            boolean hasError = false;

            // 🔥 NEW VALIDATION LOGIC
            if (amount <= 0) {
                tvAmountError.setText("Please enter a valid amount");
                tvAmountError.setVisibility(View.VISIBLE);
                hasError = true;
            } else if (amount < MIN_WITHDRAW) {
                tvAmountError.setText("Minimum withdrawal amount is " + MIN_WITHDRAW + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
                hasError = true;
            } else if (amount > MAX_WITHDRAW) {
                tvAmountError.setText("Maximum withdrawal amount is " + MAX_WITHDRAW + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
                hasError = true;
            } else if (amount > currentBalance) {
                tvAmountError.setText("Amount cannot exceed " + (int) Math.round(currentBalance) + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
                hasError = true;
            } else if (amount > maxWithdrawableGC) {
                if (maxWithdrawableGC <= 0) {
                    tvAmountError.setText("You must deposit and play 50% in games before withdrawal");
                } else {
                    tvAmountError.setText("Maximum withdrawable amount is " + maxWithdrawableGC + " GC");
                }
                tvAmountError.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                tvAmountError.setVisibility(View.GONE);
            }

            if (hasError) {
                btnWithdraw.setEnabled(false);
                btnWithdraw.setAlpha(0.5f);
                setButtonGrayStyle();
            } else if (upiId != null && !upiId.isEmpty()) {
                btnWithdraw.setEnabled(true);
                btnWithdraw.setAlpha(1.0f);
                setButtonNeonStyle();
            }
        } catch (NumberFormatException e) {
            tvAmountError.setText("Invalid amount");
            tvAmountError.setVisibility(View.VISIBLE);
            btnWithdraw.setEnabled(false);
            btnWithdraw.setAlpha(0.5f);
            setButtonGrayStyle();
        }
    }

    private void processWithdrawal() {
        String amountStr = etAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            tvAmountError.setText("Please enter amount");
            tvAmountError.setVisibility(View.VISIBLE);
            return;
        }

        try {
            int amount = Integer.parseInt(amountStr);

            // Validation
            if (amount <= 0) {
                tvAmountError.setText("Please enter a valid amount");
                tvAmountError.setVisibility(View.VISIBLE);
                return;
            }

            if (amount < MIN_WITHDRAW) {
                tvAmountError.setText("Minimum withdrawal amount is " + MIN_WITHDRAW + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
                return;
            }

            if (amount > MAX_WITHDRAW) {
                tvAmountError.setText("Maximum withdrawal amount is " + MAX_WITHDRAW + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
                return;
            }

            if (amount > maxWithdrawableGC) {
                tvAmountError.setText("Maximum withdrawable amount is " + maxWithdrawableGC + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
                return;
            }

            if (amount > currentBalance) {
                tvAmountError.setText("Insufficient balance");
                tvAmountError.setVisibility(View.VISIBLE);
                return;
            }

            // All validations passed - make API call
            submitWithdrawalRequest(amount);

        } catch (NumberFormatException e) {
            tvAmountError.setText("Invalid amount");
            tvAmountError.setVisibility(View.VISIBLE);
        }
    }

    private void submitWithdrawalRequest(int amount) {
        progressDialog.show();

        WithdrawalRequest request = new WithdrawalRequest(amount, "UPI payout");

        apiService.requestWithdrawal(request).enqueue(new Callback<WithdrawalResponse>() {
            @Override
            public void onResponse(Call<WithdrawalResponse> call, Response<WithdrawalResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    WithdrawalResponse withdrawalResponse = response.body();

                    if (withdrawalResponse.isSuccess()) {
                        // Update local balance cache
                        currentBalance -= amount;
                        WalletCacheManager.updateBalance(context, currentBalance);

                        // Send broadcast to update WalletFragment
                        Intent intent = new Intent("WALLET_UPDATED");
                        intent.putExtra("balance", currentBalance);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                        // Show success screen
                        showSuccess(amount);

                        Log.d(TAG, "Withdrawal successful: " + amount + " GC");
                        Toast.makeText(context, withdrawalResponse.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        // API returned error
                        Log.e(TAG, "Withdrawal failed: " + withdrawalResponse.getMessage());
                        Toast.makeText(context, withdrawalResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    handleWithdrawalError(response.code());
                }
            }

            @Override
            public void onFailure(Call<WithdrawalResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Withdrawal request failed", t);
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleWithdrawalError(int code) {
        String message;

        switch (code) {
            case 400:
                message = "Validation error or withdrawal limit exceeded";
                Log.e(TAG, "Withdrawal error 400: " + message);
                break;
            case 401:
                message = "Unauthorized. Please login again.";
                Log.e(TAG, "Withdrawal error 401: " + message);
                break;
            default:
                message = "Failed to process withdrawal";
                Log.e(TAG, "Withdrawal error " + code + ": " + message);
                break;
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(double amount) {
        // Hide form, show success
        layoutForm.setVisibility(View.GONE);
        layoutSuccess.setVisibility(View.VISIBLE);

        // Set amount
        tvSuccessAmount.setText((int) Math.round(amount) + " GC");

        // Play animation
        animCheck.setAnimation(R.raw.anim_check);
        animCheck.playAnimation();
    }
}