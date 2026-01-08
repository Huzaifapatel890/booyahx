package com.booyahx.payment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.booyahx.R;
import com.booyahx.WalletFragment;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ConfirmPaymentRequest;
import com.booyahx.network.models.CreateQRRequest;
import com.booyahx.network.models.CreateQRResponse;
import com.booyahx.network.models.SuccessResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentTopUpDialog extends Dialog {

    private static final String TAG = "PaymentDialog";

    private EditText etAmount, etUTR;
    private TextView btnContinue, btnSubmit, btnDone, tvTimer, tvAmount;
    private ImageView ivQR, btnClose;
    private View layoutAmount, layoutQR, layoutDone;
    private View loaderStep1, loaderStep2;
    private View step1Circle, step2Circle, stepConnector;

    private ApiService api;
    private WalletFragment parent;
    private CountDownTimer timer;

    private String qrCodeId;
    private int paymentAmount;

    public PaymentTopUpDialog(@NonNull Context context, WalletFragment parent) {
        super(context);
        this.parent = parent;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_payment_topup);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        api = ApiClient.getClient(context).create(ApiService.class);
        setCancelable(true);

        initViews();
        setupListeners();
        showAmountScreen();
    }

    private void initViews() {
        layoutAmount = findViewById(R.id.layoutAmount);
        layoutQR = findViewById(R.id.layoutQR);
        layoutDone = findViewById(R.id.layoutDone);
        loaderStep1 = findViewById(R.id.loaderStep1);
        loaderStep2 = findViewById(R.id.loaderStep2);

        etAmount = findViewById(R.id.etAmount);
        etUTR = findViewById(R.id.etUTR);

        btnContinue = findViewById(R.id.btnContinue);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnDone = findViewById(R.id.btnDone);
        btnClose = findViewById(R.id.btnClose);

        ivQR = findViewById(R.id.ivQR);
        tvTimer = findViewById(R.id.tvTimer);
        tvAmount = findViewById(R.id.tvAmount);

        step1Circle = findViewById(R.id.step1Circle);
        step2Circle = findViewById(R.id.step2Circle);
        stepConnector = findViewById(R.id.stepConnector);
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(v -> createQR());
        btnSubmit.setOnClickListener(v -> submitUTR());
        btnDone.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            dismiss();
        });
        btnClose.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            dismiss();
        });
    }

    private void animateStepTransition(int fromStep, int toStep) {
        if (toStep == 2) {
            // Animate step 2 circle to active with slower, more visible animation
            step2Circle.setBackgroundResource(R.drawable.step_active);
            step2Circle.setScaleX(0.5f);
            step2Circle.setScaleY(0.5f);
            step2Circle.setAlpha(0.5f);
            step2Circle.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(1200)  // Slower: 600ms (was 300ms)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            // Animate connector line color transition
            ValueAnimator colorAnim = ValueAnimator.ofArgb(
                    ContextCompat.getColor(getContext(), R.color.neon_cyan_alpha_30),
                    ContextCompat.getColor(getContext(), R.color.cyan)
            );
            colorAnim.setDuration(600);  // Slower: 600ms (was 300ms)
            colorAnim.addUpdateListener(animator ->
                    stepConnector.setBackgroundColor((int) animator.getAnimatedValue())
            );
            colorAnim.start();
        }
    }

    private void showAmountScreen() {
        layoutAmount.setVisibility(View.VISIBLE);
        layoutQR.setVisibility(View.GONE);
        layoutDone.setVisibility(View.GONE);
        loaderStep1.setVisibility(View.GONE);

        step1Circle.setBackgroundResource(R.drawable.step_active);
        step2Circle.setBackgroundResource(R.drawable.step_inactive);
        stepConnector.setBackgroundColor(getContext().getColor(R.color.neon_cyan_alpha_30));
    }

    private void showQRScreen() {
        animateStepTransition(1, 2);

        // Slide out amount layout to the left
        layoutAmount.animate()
                .translationX(-layoutAmount.getWidth())
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutAmount.setVisibility(View.GONE);
                        layoutAmount.setTranslationX(0);
                        layoutAmount.setAlpha(1f);
                    }
                })
                .start();

        // Slide in QR layout from the right
        layoutQR.setTranslationX(layoutQR.getWidth());
        layoutQR.setAlpha(0f);
        layoutQR.setVisibility(View.VISIBLE);
        layoutQR.animate()
                .translationX(0)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        loaderStep1.setVisibility(View.GONE);
    }

    private void showDoneScreen() {
        // Slide out QR layout to the left
        layoutQR.animate()
                .translationX(-layoutQR.getWidth())
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutQR.setVisibility(View.GONE);
                        layoutQR.setTranslationX(0);
                        layoutQR.setAlpha(1f);
                    }
                })
                .start();

        // Slide in done layout from the right (FIXED - was missing alpha!)
        layoutDone.setTranslationX(layoutDone.getWidth());
        layoutDone.setAlpha(0f);  // Start invisible
        layoutDone.setVisibility(View.VISIBLE);
        layoutDone.animate()
                .translationX(0)
                .alpha(1f)  // Fade in while sliding
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        loaderStep2.setVisibility(View.GONE);
    }

    private void showLoaderStep1() {
        loaderStep1.setVisibility(View.VISIBLE);
        btnContinue.setEnabled(false);
        btnContinue.setAlpha(0.5f);
    }

    private void hideLoaderStep1() {
        loaderStep1.setVisibility(View.GONE);
        btnContinue.setEnabled(true);
        btnContinue.setAlpha(1f);
    }

    private void showLoaderStep2() {
        loaderStep2.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.5f);
    }

    private void hideLoaderStep2() {
        loaderStep2.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
        btnSubmit.setAlpha(1f);
    }

    private void createQR() {
        String amountStr = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Enter amount");
            etAmount.requestFocus();
            return;
        }

        try {
            paymentAmount = Integer.parseInt(amountStr);
            if (paymentAmount < 1) {
                etAmount.setError("Minimum ₹1");
                etAmount.requestFocus();
                return;
            }
        } catch (Exception e) {
            etAmount.setError("Invalid number");
            etAmount.requestFocus();
            return;
        }

        showLoaderStep1();
        Log.d(TAG, "Creating QR for amount: " + paymentAmount);

        api.createQR(new CreateQRRequest(paymentAmount, true, "Top-up"))
                .enqueue(new Callback<CreateQRResponse>() {
                    @Override
                    public void onResponse(Call<CreateQRResponse> call, Response<CreateQRResponse> res) {
                        hideLoaderStep1();

                        Log.d(TAG, "Response code: " + res.code());

                        if (!res.isSuccessful()) {
                            Log.e(TAG, "API error: " + res.code());
                            Toast.makeText(getContext(), "Server error: " + res.code(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (res.body() == null) {
                            Log.e(TAG, "Response body is null");
                            Toast.makeText(getContext(), "Invalid response from server", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!res.body().success) {
                            Log.e(TAG, "API returned success=false: " + res.body().message);
                            Toast.makeText(getContext(), res.body().message, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (res.body().data == null) {
                            Log.e(TAG, "Response data is null");
                            Toast.makeText(getContext(), "Invalid data from server", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Log.d(TAG, "QR created successfully");
                        Log.d(TAG, "QR Code ID: " + res.body().data.qrCodeId);

                        qrCodeId = res.body().data.qrCodeId;
                        tvAmount.setText("Amount to Pay: ₹" + paymentAmount);

                        if (!TextUtils.isEmpty(res.body().data.qrCodeImage)) {
                            Log.d(TAG, "Using base64 image");
                            loadBase64Image(res.body().data.qrCodeImage);
                        } else if (!TextUtils.isEmpty(res.body().data.upiLink)) {
                            Log.d(TAG, "Generating QR from UPI link");
                            generateQR(res.body().data.upiLink);
                        } else if (!TextUtils.isEmpty(res.body().data.qrCodeString)) {
                            Log.d(TAG, "Generating QR from QR string");
                            generateQR(res.body().data.qrCodeString);
                        } else {
                            Log.e(TAG, "No QR content available");
                            Toast.makeText(getContext(), "QR code data missing", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        startTimer();
                        showQRScreen();
                    }

                    @Override
                    public void onFailure(Call<CreateQRResponse> call, Throwable t) {
                        hideLoaderStep1();
                        Log.e(TAG, "QR creation error", t);
                        Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadBase64Image(String base64String) {
        try {
            String base64Image = base64String;
            if (base64String.contains(",")) {
                base64Image = base64String.split(",")[1];
            }

            Log.d(TAG, "Decoding base64 image");
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            if (bitmap != null) {
                ivQR.setImageBitmap(bitmap);
                ivQR.setVisibility(View.VISIBLE);
                Log.d(TAG, "Base64 image loaded successfully");
            } else {
                Log.e(TAG, "Failed to decode bitmap from base64");
                Toast.makeText(getContext(), "Failed to load QR image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading base64 image", e);
            Toast.makeText(getContext(), "Failed to load QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateQR(String content) {
        if (TextUtils.isEmpty(content)) {
            Log.e(TAG, "Cannot generate QR: content is null or empty");
            Toast.makeText(getContext(), "Invalid QR content", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.d(TAG, "Generating QR bitmap for: " + content);

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ivQR.setImageBitmap(bmp);
            ivQR.setVisibility(View.VISIBLE);
            Log.d(TAG, "QR bitmap generated successfully");

        } catch (Exception e) {
            Log.e(TAG, "QR generation failed", e);
            Toast.makeText(getContext(), "Failed to generate QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void submitUTR() {
        String utr = etUTR.getText().toString().trim();

        if (TextUtils.isEmpty(utr)) {
            etUTR.setError("Enter UTR");
            etUTR.requestFocus();
            return;
        }

        if (utr.length() < 8 || utr.length() > 20) {
            etUTR.setError("UTR must be 8-20 characters");
            etUTR.requestFocus();
            return;
        }
        if (!utr.matches("\\d+")) {
            etUTR.setError("Only numbers allowed");
            return;
        }

        showLoaderStep2();
        Log.d(TAG, "Submitting UTR: " + utr);

        api.confirmPayment(new ConfirmPaymentRequest(qrCodeId, utr, "proof"))
                .enqueue(new Callback<SuccessResponse>() {
                    @Override
                    public void onResponse(Call<SuccessResponse> call, Response<SuccessResponse> res) {
                        hideLoaderStep2();

                        if (res.isSuccessful() && res.body() != null && res.body().success) {
                            Log.d(TAG, "Payment confirmed successfully");
                            if (timer != null) timer.cancel();
                            showDoneScreen();
                            parent.refreshBalance();
                        } else {
                            String msg = res.body() != null ? res.body().message : "Unknown error";
                            Log.e(TAG, "Payment confirmation failed: " + msg);
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SuccessResponse> call, Throwable t) {
                        hideLoaderStep2();
                        Log.e(TAG, "Payment confirmation error", t);
                        Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(10 * 60 * 1000, 1000) {
            public void onTick(long ms) {
                long seconds = ms / 1000;
                tvTimer.setText(String.format("⏱ Expires in: %02d:%02d", seconds / 60, seconds % 60));
            }

            public void onFinish() {
                tvTimer.setText("⏱ QR Expired");
                tvTimer.setTextColor(getContext().getColor(android.R.color.holo_red_light));
                Toast.makeText(getContext(), "QR Code expired. Please create a new one.", Toast.LENGTH_LONG).show();
            }
        };
        timer.start();
    }

    @Override
    public void dismiss() {
        if (timer != null) {
            timer.cancel();
        }
        super.dismiss();
    }
}