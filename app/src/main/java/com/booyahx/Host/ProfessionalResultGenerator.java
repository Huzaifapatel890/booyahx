package com.booyahx.Host;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.booyahx.R;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProfessionalResultGenerator {

    private final Context context;
    private Paint textPaint, headerPaint, boldPaint, boxPaint, borderPaint, shadowPaint;

    public static class Config {
        public int backgroundDrawable = R.drawable.forest_theme;
        public int width = 1080;
        public int height = 1920;
        public int tableTopMargin = 520;
        public int maxTeams = 12;
        public boolean showBorders = true;
        public int headerColor = Color.parseColor("#FFD700");
        public int rowBgColor = Color.parseColor("#1A000000");
        public int alternateBgColor = Color.parseColor("#33000000");
    }

    public ProfessionalResultGenerator(Context context) {
        this.context = context;
        setupPaints();
    }

    private void setupPaints() {
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#FFD700"));
        headerPaint.setTextSize(32);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);
        headerPaint.setTextAlign(Paint.Align.CENTER);

        boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldPaint.setColor(Color.parseColor("#00FF00"));
        boldPaint.setTextSize(40);
        boldPaint.setTypeface(Typeface.DEFAULT_BOLD);
        boldPaint.setTextAlign(Paint.Align.CENTER);

        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        borderPaint.setColor(Color.parseColor("#555555"));

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setMaskFilter(new BlurMaskFilter(4, BlurMaskFilter.Blur.NORMAL));
    }

    public File generateResultImage(List<FinalRow> rows, String name) {
        return generateResultImage(rows, name, new Config());
    }

    public File generateResultImage(List<FinalRow> rows, String name, Config config) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(config.width, config.height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            drawBackground(canvas, config);
            drawProfessionalTable(canvas, rows, config);

            return saveBitmap(bitmap, name);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void drawBackground(Canvas canvas, Config config) {
        try {
            Bitmap bg = BitmapFactory.decodeResource(context.getResources(), config.backgroundDrawable);
            if (bg != null) {
                Bitmap scaledBg = Bitmap.createScaledBitmap(bg, config.width, config.height, true);
                canvas.drawBitmap(scaledBg, 0, 0, null);
                bg.recycle();
            } else {
                canvas.drawColor(Color.parseColor("#0a0a0a"));
            }
        } catch (Exception e) {
            canvas.drawColor(Color.parseColor("#0a0a0a"));
        }
    }

    private void drawProfessionalTable(Canvas canvas, List<FinalRow> rows, Config config) {
        int startY = config.tableTopMargin;
        int startX = 25;

        int[] colWidths = {80, 380, 130, 130, 150, 160};
        String[] headers = {"SLO", "SLOT NAME", "BOOYAH", "KILL", "POSITION", "TOTAL"};

        drawHeaderRow(canvas, startX, startY, colWidths, headers, config);

        int rowHeight = 78;
        int currentY = startY + 70;

        for (int i = 0; i < config.maxTeams; i++) {
            FinalRow row = i < rows.size() ? rows.get(i) : null;
            boolean isAlternate = i % 2 == 1;

            drawDataRow(canvas, startX, currentY, colWidths, i + 1, row, isAlternate, config);
            currentY += rowHeight;
        }

        if (config.showBorders) {
            RectF outerRect = new RectF(startX, startY, startX + sumArray(colWidths), currentY);
            borderPaint.setStrokeWidth(4);
            borderPaint.setColor(Color.parseColor("#FFD700"));
            canvas.drawRoundRect(outerRect, 15, 15, borderPaint);
        }
    }

    private void drawHeaderRow(Canvas canvas, int x, int y, int[] colWidths, String[] headers, Config config) {
        boxPaint.setColor(config.headerColor);
        boxPaint.setAlpha(100);
        RectF headerRect = new RectF(x, y, x + sumArray(colWidths), y + 70);
        canvas.drawRoundRect(headerRect, 12, 12, boxPaint);

        if (config.showBorders) {
            borderPaint.setColor(config.headerColor);
            borderPaint.setStrokeWidth(3);
            canvas.drawRoundRect(headerRect, 12, 12, borderPaint);
        }

        int currentX = x;
        for (int i = 0; i < headers.length; i++) {
            float textX = currentX + colWidths[i] / 2f;
            float textY = y + 45;

            drawTextWithShadow(canvas, headers[i], textX, textY, headerPaint);

            if (i < headers.length - 1 && config.showBorders) {
                currentX += colWidths[i];
                canvas.drawLine(currentX, y + 10, currentX, y + 60, borderPaint);
            } else {
                currentX += colWidths[i];
            }
        }
    }

    private void drawDataRow(Canvas canvas, int x, int y, int[] colWidths, int slotNum,
                             FinalRow row, boolean isAlternate, Config config) {

        int rowHeight = 78;
        int rowWidth = sumArray(colWidths);

        boxPaint.setColor(isAlternate ? config.alternateBgColor : config.rowBgColor);
        RectF rowRect = new RectF(x, y, x + rowWidth, y + rowHeight);
        canvas.drawRoundRect(rowRect, 8, 8, boxPaint);

        if (config.showBorders) {
            borderPaint.setColor(Color.parseColor("#444444"));
            borderPaint.setStrokeWidth(2);
            canvas.drawRoundRect(rowRect, 8, 8, borderPaint);
        }

        int currentX = x;

        // Slot number
        drawCellWithBox(canvas, currentX, y, colWidths[0], rowHeight,
                String.format(Locale.US, "%02d", slotNum), textPaint, config);
        currentX += colWidths[0];

        // Team name
        if (row != null) {
            textPaint.setTextAlign(Paint.Align.LEFT);
            String teamName = truncateText(row.teamName, 20);
            drawCellWithBox(canvas, currentX, y, colWidths[1], rowHeight,
                    teamName, textPaint, config);
            textPaint.setTextAlign(Paint.Align.CENTER);
        } else {
            drawCellWithBox(canvas, currentX, y, colWidths[1], rowHeight, "", textPaint, config);
        }
        currentX += colWidths[1];

        // Booyah
        String booyah = row != null ? String.valueOf(row.booyah) : "-";
        drawCellWithBox(canvas, currentX, y, colWidths[2], rowHeight, booyah, textPaint, config);
        currentX += colWidths[2];

        // Kill points
        String kp = row != null ? String.valueOf(row.kp) : "-";
        drawCellWithBox(canvas, currentX, y, colWidths[3], rowHeight, kp, textPaint, config);
        currentX += colWidths[3];

        // Position points
        String pp = row != null ? String.valueOf(row.pp) : "-";
        drawCellWithBox(canvas, currentX, y, colWidths[4], rowHeight, pp, textPaint, config);
        currentX += colWidths[4];

        // Total
        String total = row != null ? String.valueOf(row.total) : "-";
        Paint totalPaint = row != null ? boldPaint : textPaint;
        drawCellWithBox(canvas, currentX, y, colWidths[5], rowHeight, total, totalPaint, config);
    }

    private void drawCellWithBox(Canvas canvas, int x, int y, int width, int height,
                                 String text, Paint paint, Config config) {

        if (config.showBorders) {
            canvas.drawLine(x, y + 10, x, y + height - 10, borderPaint);
        }

        float textX = paint.getTextAlign() == Paint.Align.LEFT ? x + 15 : x + width / 2f;
        float textY = y + height / 2f + 12;

        drawTextWithShadow(canvas, text, textX, textY, paint);
    }

    private void drawTextWithShadow(Canvas canvas, String text, float x, float y, Paint paint) {
        shadowPaint.setTextSize(paint.getTextSize());
        shadowPaint.setTextAlign(paint.getTextAlign());
        canvas.drawText(text, x + 2, y + 2, shadowPaint);
        canvas.drawText(text, x, y, paint);
    }

    private int sumArray(int[] arr) {
        int sum = 0;
        for (int val : arr) sum += val;
        return sum;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }

    private File saveBitmap(Bitmap bmp, String name) throws Exception {
        String fileName = "BooyahX_" + name + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(System.currentTimeMillis()) + ".png";

        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        v.put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/BooyahX");

        Uri uri = context.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);

        OutputStream out = context.getContentResolver().openOutputStream(uri);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();

        return new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES),
                "BooyahX/" + fileName);
    }

    public File generateWithBackground(List<FinalRow> rows, String name, int drawableRes) {
        Config config = new Config();
        config.backgroundDrawable = drawableRes;
        return generateResultImage(rows, name, config);
    }
}