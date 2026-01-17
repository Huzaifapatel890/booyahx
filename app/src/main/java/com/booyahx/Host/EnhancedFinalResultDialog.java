package com.booyahx.Host;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.booyahx.R;

import java.io.File;
import java.util.Calendar;
import java.util.List;

public class EnhancedFinalResultDialog extends Dialog {

    private final Context context;
    private final List<FinalRow> results;

    private ImageView resultPreview;
    private LinearLayout downloadBtn, shareBtn;
    private TextView closeBtn;
    private ProgressBar progressBar;

    private ProfessionalResultGenerator generator;

    // ✅ File exists ONLY after user presses download
    private File savedImageFile;

    // ✅ SINGLE SOURCE OF TRUTH FOR THEME
    private int selectedThemeRes;

    private volatile boolean isDestroyed = false;

    public EnhancedFinalResultDialog(
            @NonNull Context context,
            List<FinalRow> results
    ) {
        super(context);
        this.context = context;
        this.results = results;
        this.generator = new ProfessionalResultGenerator(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_enhanced_result);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        initViews();

        // ✅ FIX: wait until ImageView is laid out
        resultPreview.post(this::generateBasedOnTime);

        setupButtons();
    }

    @Override
    public void dismiss() {
        isDestroyed = true;
        super.dismiss();
    }

    private void initViews() {
        resultPreview = findViewById(R.id.resultPreview);
        downloadBtn = findViewById(R.id.downloadBtn);
        shareBtn = findViewById(R.id.shareBtn);
        closeBtn = findViewById(R.id.closeBtn);
        progressBar = findViewById(R.id.progressBar);

        resultPreview.setAdjustViewBounds(true);
        resultPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    // ================= PREVIEW (NO SAVE) =================
    private void generateBasedOnTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) {
            selectedThemeRes = R.drawable.bloom_theme;
        } else if (hour >= 12 && hour < 17) {
            selectedThemeRes = R.drawable.anim_theme;
        } else if (hour >= 17 && hour < 21) {
            selectedThemeRes = R.drawable.nostalagic_theme;
        } else if (hour >= 21 && hour < 24) {
            selectedThemeRes = R.drawable.mountain_theme;
        } else {
            selectedThemeRes = R.drawable.galexy_theme;
        }

        generatePreview();
    }

    private void generatePreview() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        resultPreview.setImageDrawable(null);

        new Thread(() -> {
            Bitmap preview =
                    generator.generatePreviewBitmap(results, selectedThemeRes);

            resultPreview.post(() -> {
                if (isDestroyed) return;
                resultPreview.setImageBitmap(preview);
                progressBar.setVisibility(ProgressBar.GONE);
            });
        }).start();
    }
    // ====================================================

    private void setupButtons() {

        // ================= DOWNLOAD =================
        downloadBtn.setOnClickListener(v -> {
            if (savedImageFile != null && savedImageFile.exists()) {
                Toast.makeText(context,
                        "Result already saved",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(ProgressBar.VISIBLE);

            new Thread(() -> {
                File file = generator.generateWithBackground(
                        results,
                        "Final_Result",
                        selectedThemeRes // ✅ SAME THEME AS PREVIEW
                );

                savedImageFile = file;

                resultPreview.post(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(context,
                            "Result saved to Pictures/BooyahX/",
                            Toast.LENGTH_LONG).show();
                });
            }).start();
        });

        // ================= SHARE =================
        shareBtn.setOnClickListener(v -> {
            if (savedImageFile == null || !savedImageFile.exists()) {
                Toast.makeText(context,
                        "Please download first",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            shareImage();
        });

        closeBtn.setOnClickListener(v -> dismiss());
    }

    private void shareImage() {
        if (savedImageFile == null || !savedImageFile.exists()) {
            Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri imageUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    savedImageFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.putExtra(Intent.EXTRA_TEXT,
                    "🏆 Tournament Results - Shared from BooyahX");

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 🔥 REQUIRED FOR DIALOG

            context.startActivity(Intent.createChooser(intent, "Share Result"));

        } catch (Exception e) {
            e.printStackTrace(); // 🔥 NOW YOU WILL SEE LOGS
            Toast.makeText(context,
                    "Failed to share image",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private int getCurrentTheme() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) return R.drawable.bloom_theme;
        if (hour >= 12 && hour < 17) return R.drawable.anim_theme;
        if (hour >= 17 && hour < 21) return R.drawable.nostalagic_theme;
        if (hour >= 21 && hour < 24) return R.drawable.mountain_theme;
        return R.drawable.galexy_theme;
    }
}