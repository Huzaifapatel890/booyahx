package com.booyahx.Host;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.booyahx.R;

import java.io.File;
import java.util.List;

public class FinalResultDialog extends Dialog {

    private Context context;
    private List<FinalRow> results;
    private LinearLayout tableContainer;
    private TextView exportButton, closeButton, themeIndicator;
    private ResultImageGenerator imageGenerator;

    public FinalResultDialog(@NonNull Context context, List<FinalRow> results) {
        super(context);
        this.context = context;
        this.results = results;
        this.imageGenerator = new ResultImageGenerator(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_final_result);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        tableContainer = findViewById(R.id.tableContainer);
        exportButton = findViewById(R.id.exportButton);
        closeButton = findViewById(R.id.closeButton);
        themeIndicator = findViewById(R.id.themeIndicator);

        // Show current theme
        themeIndicator.setText("Theme: " + imageGenerator.getCurrentTheme());

        buildTable();
        setupButtons();
    }

    private void buildTable() {
        tableContainer.removeAllViews();

        for (int i = 0; i < results.size(); i++) {
            FinalRow row = results.get(i);
            View rowView = LayoutInflater.from(context)
                    .inflate(R.layout.item_final_result_row, tableContainer, false);

            TextView rank = rowView.findViewById(R.id.rankText);
            TextView team = rowView.findViewById(R.id.teamText);
            TextView kp = rowView.findViewById(R.id.kpText);
            TextView pp = rowView.findViewById(R.id.ppText);
            TextView total = rowView.findViewById(R.id.totalText);
            TextView booyah = rowView.findViewById(R.id.booyahText);

            rank.setText(String.valueOf(i + 1));
            team.setText(row.teamName);
            kp.setText(String.valueOf(row.kp));
            pp.setText(String.valueOf(row.pp));
            total.setText(String.valueOf(row.total));
            booyah.setText(String.valueOf(row.booyah));

            // Highlight top 3
            if (i < 3) {
                rowView.setBackgroundColor(Color.parseColor("#1A00FF00"));
            }

            tableContainer.addView(rowView);
        }
    }

    private void setupButtons() {
        exportButton.setOnClickListener(v -> {
            exportButton.setEnabled(false);
            exportButton.setText("Generating...");

            new Thread(() -> {
                File imageFile = imageGenerator.generateResultImage(
                        results,
                        "tournament_final"
                );

                ((android.app.Activity) context).runOnUiThread(() -> {
                    exportButton.setEnabled(true);
                    exportButton.setText("Export Image");

                    if (imageFile != null) {
                        Toast.makeText(
                                context,
                                "Saved to: " + imageFile.getAbsolutePath(),
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                context,
                                "Failed to generate image",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }).start();
        });

        closeButton.setOnClickListener(v -> dismiss());
    }
}