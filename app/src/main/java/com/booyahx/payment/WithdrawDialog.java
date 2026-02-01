package com.booyahx.payment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;
import com.booyahx.ProfileCacheManager;
import com.booyahx.R;
import com.booyahx.settings.EditProfileActivity;
import com.booyahx.WalletCacheManager;

public class WithdrawDialog extends Dialog {

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

    private double currentBalance;
    private String upiId;

    // Withdrawal limits
    private static final int MIN_WITHDRAW = 40;
    private static final int MAX_WITHDRAW = 9999;

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
        loadData();
        setupListeners();
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
    }

    private void loadData() {
        // Load balance from WalletCacheManager
        currentBalance = WalletCacheManager.getBalance(context);
        int balanceInt = (int) Math.round(currentBalance);

        tvCurrentBalance.setText(balanceInt + " GC");
        tvWithdrawableBalance.setText(balanceInt + " GC");

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
        }
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
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            if (amount <= 0) {
                tvAmountError.setText("Please enter a valid amount");
                tvAmountError.setVisibility(View.VISIBLE);
            } else if (amount < MIN_WITHDRAW) {
                tvAmountError.setText("Minimum withdrawal amount is " + MIN_WITHDRAW + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
            } else if (amount > MAX_WITHDRAW) {
                tvAmountError.setText("Maximum withdrawal amount is " + MAX_WITHDRAW + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
            } else if (amount > currentBalance) {
                tvAmountError.setText("Amount cannot exceed " + (int) Math.round(currentBalance) + " GC");
                tvAmountError.setVisibility(View.VISIBLE);
            } else {
                tvAmountError.setVisibility(View.GONE);
            }
        } catch (NumberFormatException e) {
            tvAmountError.setText("Invalid amount");
            tvAmountError.setVisibility(View.VISIBLE);
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
            double amount = Double.parseDouble(amountStr);

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

            if (amount > currentBalance) {
                tvAmountError.setText("Insufficient balance");
                tvAmountError.setVisibility(View.VISIBLE);
                return;
            }

            // All validations passed - show success
            showSuccess(amount);

        } catch (NumberFormatException e) {
            tvAmountError.setText("Invalid amount");
            tvAmountError.setVisibility(View.VISIBLE);
        }
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