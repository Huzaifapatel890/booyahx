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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.booyahx.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EnhancedFinalResultDialog extends Dialog {

    private final Context context;
    private final List<FinalRow> results;

    private Spinner backgroundSpinner;
    private ImageView resultPreview;
    private LinearLayout downloadBtn, shareBtn;
    private TextView closeBtn;
    private ProgressBar progressBar;

    private ProfessionalResultGenerator generator;
    private File currentImageFile;

    private static class BgOption {
        String name;
        int drawableRes;

        BgOption(String name, int drawableRes) {
            this.name = name;
            this.drawableRes = drawableRes;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    private List<BgOption> backgroundOptions;

    public EnhancedFinalResultDialog(@NonNull Context context, List<FinalRow> results) {
        super(context);
        this.context = context;
        this.results = results;
        this.generator = new ProfessionalResultGenerator(context);
        setupBackgroundOptions();
    }

    private void setupBackgroundOptions() {
        backgroundOptions = new ArrayList<>();
        backgroundOptions.add(new BgOption("🌲 Forest Theme", R.drawable.forest_theme));
        backgroundOptions.add(new BgOption("🌊 Ocean Theme", R.drawable.ocean_theme));
        backgroundOptions.add(new BgOption("🔥 Fire Theme", R.drawable.fire_theme));
        backgroundOptions.add(new BgOption("⚡ Lightning Theme", R.drawable.lightning_theme));
        backgroundOptions.add(new BgOption("🌌 Galaxy Theme", R.drawable.galexy_theme));
        backgroundOptions.add(new BgOption("🏔️ Mountain Theme", R.drawable.mountain_theme));
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
        setupBackgroundSpinner();
        generatePreview(backgroundOptions.get(0).drawableRes);
        setupButtons();
    }

    private void initViews() {
        backgroundSpinner = findViewById(R.id.backgroundSpinner);
        resultPreview = findViewById(R.id.resultPreview);
        downloadBtn = findViewById(R.id.downloadBtn);
        shareBtn = findViewById(R.id.shareBtn);
        closeBtn = findViewById(R.id.closeBtn);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupBackgroundSpinner() {
        ArrayAdapter<BgOption> adapter = new ArrayAdapter<BgOption>(
                context,
                android.R.layout.simple_spinner_item,
                backgroundOptions
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(16);
                tv.setPadding(20, 20, 20, 20);
                return view;
            }

            @NonNull
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(Color.WHITE);
                tv.setBackgroundColor(Color.parseColor("#1a1a1a"));
                tv.setPadding(30, 30, 30, 30);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        backgroundSpinner.setAdapter(adapter);

        backgroundSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        BgOption selected = backgroundOptions.get(position);
                        generatePreview(selected.drawableRes);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                }
        );
    }

    private void generatePreview(final int backgroundRes) {
        progressBar.setVisibility(View.VISIBLE);
        resultPreview.setImageDrawable(null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentImageFile = generator.generateWithBackground(
                            results, "Preview", backgroundRes);

                    if (currentImageFile != null && currentImageFile.exists()) {
                        final Bitmap preview = BitmapFactory.decodeFile(
                                currentImageFile.getAbsolutePath());

                        resultPreview.post(new Runnable() {
                            @Override
                            public void run() {
                                resultPreview.setImageBitmap(preview);
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        showError("Failed to generate preview");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void setupButtons() {

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentImageFile == null || !currentImageFile.exists()) {
                    Toast.makeText(context, "Please wait, generating image...",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(context,
                        "Result saved to Pictures/BooyahX/",
                        Toast.LENGTH_LONG).show();
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentImageFile == null || !currentImageFile.exists()) {
                    Toast.makeText(context, "Please wait, generating image...",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                shareImage();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void shareImage() {
        try {
            Uri imageUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    currentImageFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "🏆 Tournament Results - Shared from BooyahX");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Share Result"));

        } catch (Exception e) {
            Toast.makeText(context,
                    "Failed to share: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showError(final String message) {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}