package com.booyahx.Host;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
    private File currentImageFile;
    private volatile boolean isDestroyed = false;

    public EnhancedFinalResultDialog(@NonNull Context context, List<FinalRow> results) {
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
        generateBasedOnTime();
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

    // ✅ FIXED TIME RANGE LOGIC
    private void generateBasedOnTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int themeRes;

        if (hour >= 6 && hour < 12) {
            themeRes = R.drawable.mountain_theme;      // Morning
        } else if (hour >= 12 && hour < 17) {
            themeRes = R.drawable.forest_theme;        // Afternoon
        } else if (hour >= 17 && hour < 21) {
            themeRes = R.drawable.forest_theme;        // Evening
        } else if (hour >= 21 && hour < 24) {
            themeRes = R.drawable.lightning_theme;     // Night
        } else {
            themeRes = R.drawable.galexy_theme;        // Midnight
        }

        generatePreview(themeRes);
    }

    private void generatePreview(final int backgroundRes) {
        progressBar.setVisibility(View.VISIBLE);
        resultPreview.setImageDrawable(null);

        new Thread(() -> {
            try {
                File file = generator.generateWithBackground(
                        results, "Preview", backgroundRes);

                if (isDestroyed || file == null || !file.exists()) return;

                currentImageFile = file;

                // ✅ SAFE PREVIEW DECODING (scaled)
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 2; // half resolution for preview
                final Bitmap preview = BitmapFactory.decodeFile(
                        file.getAbsolutePath(), opts);

                resultPreview.post(() -> {
                    if (isDestroyed) return;
                    resultPreview.setImageBitmap(preview);
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to generate preview");
            }
        }).start();
    }

    private void setupButtons() {

        downloadBtn.setOnClickListener(v -> {
            if (currentImageFile == null || !currentImageFile.exists()) {
                Toast.makeText(context,
                        "Please wait, generating image...",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(context,
                    "Result saved to Pictures/BooyahX/",
                    Toast.LENGTH_LONG).show();
        });

        shareBtn.setOnClickListener(v -> {
            if (currentImageFile == null || !currentImageFile.exists()) {
                Toast.makeText(context,
                        "Please wait, generating image...",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            shareImage();
        });

        closeBtn.setOnClickListener(v -> dismiss());
    }

    private void shareImage() {
        try {
            Uri imageUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    currentImageFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.putExtra(Intent.EXTRA_TEXT,
                    "🏆 Tournament Results - Shared from BooyahX");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Share Result"));

        } catch (Exception e) {
            Toast.makeText(context,
                    "Failed to share image",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(final String message) {
        progressBar.post(() -> {
            if (isDestroyed) return;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }
}