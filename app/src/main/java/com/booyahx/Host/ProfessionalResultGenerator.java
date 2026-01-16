package com.booyahx.Host;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.res.ResourcesCompat;

import com.booyahx.R;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProfessionalResultGenerator {

    private final Context context;

    private Paint textPaint, headerPaint, totalPaint;
    private Paint boxPaint, borderPaint, strokePaint;
    private Typeface font;

    // Rank colors
    private static final int GOLD = Color.parseColor("#FFD700");
    private static final int SILVER = Color.parseColor("#C0C0C0");
    private static final int BRONZE = Color.parseColor("#CD7F32");
    private static final int WHITE = Color.WHITE;

    public static class Config {
        public int backgroundDrawable = R.drawable.forest_theme;
        public int width = 1080;
        public int height = 1920;
        public int tableTopMargin = 520;
        public int maxTeams = 12;

        public int rowBgColor = Color.parseColor("#22000000");
        public int alternateBgColor = Color.parseColor("#16000000");
        public int headerBgColor = Color.parseColor("#33000000");
    }

    public ProfessionalResultGenerator(Context context) {
        this.context = context;
        setupPaints();
    }

    private void setupPaints() {

        font = ResourcesCompat.getFont(context, R.font.esports_regular);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(WHITE);
        textPaint.setTextSize(38);
        textPaint.setTypeface(font);
        textPaint.setTextAlign(Paint.Align.CENTER);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#FFEFA6"));
        headerPaint.setTextSize(36);
        headerPaint.setTypeface(font);
        headerPaint.setTextAlign(Paint.Align.CENTER);

        totalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        totalPaint.setColor(Color.parseColor("#5FFF60"));
        totalPaint.setTextSize(38);
        totalPaint.setTypeface(font);
        totalPaint.setTextAlign(Paint.Align.CENTER);

        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        borderPaint.setColor(Color.parseColor("#88FFFFFF"));

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4);
        strokePaint.setColor(Color.BLACK);
        strokePaint.setTypeface(font);
        strokePaint.setTextAlign(Paint.Align.CENTER);
    }

    // ================= BACKGROUND FIX =================
    private void drawBackground(Canvas canvas, Config config) {
        Drawable drawable = ResourcesCompat.getDrawable(
                context.getResources(),
                config.backgroundDrawable,
                null
        );

        if (drawable == null) {
            canvas.drawColor(Color.BLACK);
            return;
        }

        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();

        float scale;
        float dx = 0, dy = 0;

        if (dWidth * config.height > config.width * dHeight) {
            // image is wider than canvas
            scale = (float) config.height / (float) dHeight;
            dx = (config.width - dWidth * scale) * 0.5f;
        } else {
            // image is taller than canvas
            scale = (float) config.width / (float) dWidth;
            dy = (config.height - dHeight * scale) * 0.5f;
        }

        canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);

        drawable.setBounds(0, 0, dWidth, dHeight);
        drawable.draw(canvas);

        canvas.restore();
    }
    // ==================================================

    // ✅ THIS METHOD WAS MISSING (CRITICAL FIX)
    public File generateWithBackground(List<FinalRow> rows, String name, int backgroundRes) {
        Config config = new Config();
        config.backgroundDrawable = backgroundRes;
        return generateResultImage(rows, name, config);
    }

    public File generateResultImage(List<FinalRow> rows, String name) {
        return generateResultImage(rows, name, new Config());
    }

    public File generateResultImage(List<FinalRow> rows, String name, Config config) {
        try {
            Bitmap bmp = Bitmap.createBitmap(
                    config.width,
                    config.height,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bmp);

            drawBackground(canvas, config);
            drawTable(canvas, rows, config);

            return saveBitmap(bmp, name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void drawTable(Canvas canvas, List<FinalRow> rows, Config config) {
        int x = 30;
        int y = config.tableTopMargin;

        int[] w = {90, 380, 130, 130, 150, 160};

        drawHeader(canvas, x, y, w);
        y += 74;

        for (int i = 0; i < config.maxTeams; i++) {
            drawRow(canvas, x, y, w, rows, i, config);
            y += 80;
        }

        RectF outer = new RectF(x, config.tableTopMargin, x + sum(w), y);
        borderPaint.setColor(Color.parseColor("#CCFFFFFF"));
        canvas.drawRoundRect(outer, 16, 16, borderPaint);
    }

    private void drawHeader(Canvas canvas, int x, int y, int[] w) {
        boxPaint.setColor(Color.parseColor("#33000000"));
        RectF r = new RectF(x, y, x + sum(w), y + 74);
        canvas.drawRoundRect(r, 14, 14, boxPaint);

        borderPaint.setColor(Color.parseColor("#FFEFA6"));
        canvas.drawRoundRect(r, 14, 14, borderPaint);

        String[] heads = {"#", "SLOT NAME", "BOOYAH", "KILL", "POSITION", "TOTAL"};
        int cx = x;

        for (int i = 0; i < heads.length; i++) {
            drawStroke(canvas, heads[i], cx + w[i] / 2f, y + 48, headerPaint);
            cx += w[i];
        }
    }

    private void drawRow(Canvas canvas, int x, int y, int[] w,
                         List<FinalRow> rows, int index, Config config) {

        RectF r = new RectF(x, y, x + sum(w), y + 80);
        boxPaint.setColor(index % 2 == 0
                ? config.rowBgColor
                : config.alternateBgColor);

        canvas.drawRoundRect(r, 10, 10, boxPaint);
        canvas.drawRoundRect(r, 10, 10, borderPaint);

        FinalRow row = index < rows.size() ? rows.get(index) : null;
        int cx = x;

        int rankColor = WHITE;
        if (index == 0) rankColor = GOLD;
        else if (index == 1) rankColor = SILVER;
        else if (index == 2) rankColor = BRONZE;

        textPaint.setColor(rankColor);

        String rankText;
        if (index == 0) rankText = "🥇";
        else if (index == 1) rankText = "🥈";
        else if (index == 2) rankText = "🥉";
        else rankText = String.format("%02d", index + 1);

        drawStroke(canvas, rankText, cx + w[0] / 2f, y + 50, textPaint);
        cx += w[0];

        drawStrokeLeft(canvas,
                row != null ? truncate(row.teamName, 20) : "",
                cx + 15, y + 50, textPaint);
        cx += w[1];

        textPaint.setColor(WHITE);

        drawStroke(canvas, row != null ? String.valueOf(row.booyah) : "-",
                cx + w[2] / 2f, y + 50, textPaint);
        cx += w[2];

        drawStroke(canvas, row != null ? String.valueOf(row.kp) : "-",
                cx + w[3] / 2f, y + 50, textPaint);
        cx += w[3];

        drawStroke(canvas, row != null ? String.valueOf(row.pp) : "-",
                cx + w[4] / 2f, y + 50, textPaint);
        cx += w[4];

        drawStroke(canvas, row != null ? String.valueOf(row.total) : "-",
                cx + w[5] / 2f, y + 50, totalPaint);
    }

    private void drawStroke(Canvas c, String t, float x, float y, Paint p) {
        strokePaint.setTextSize(p.getTextSize());
        c.drawText(t, x, y, strokePaint);
        c.drawText(t, x, y, p);
    }

    private void drawStrokeLeft(Canvas c, String t, float x, float y, Paint p) {
        strokePaint.setTextSize(p.getTextSize());
        strokePaint.setTextAlign(Paint.Align.LEFT);
        p.setTextAlign(Paint.Align.LEFT);

        c.drawText(t, x, y, strokePaint);
        c.drawText(t, x, y, p);

        strokePaint.setTextAlign(Paint.Align.CENTER);
        p.setTextAlign(Paint.Align.CENTER);
    }

    private int sum(int[] a) {
        int s = 0;
        for (int i : a) s += i;
        return s;
    }

    private String truncate(String s, int m) {
        return s == null || s.length() <= m
                ? s
                : s.substring(0, m - 2) + "..";
    }

    private File saveBitmap(Bitmap bmp, String name) throws Exception {
        String file = "BooyahX_" + name + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(System.currentTimeMillis()) + ".png";

        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, file);
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
                "BooyahX/" + file
        );
    }
}