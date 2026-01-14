package com.booyahx.Host;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.OutputStream;
import com.booyahx.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ResultImageGenerator {

    private Context context;
    private Paint blackTextPaint;
    private Paint whiteStrokePaint;
    private Paint numberPaint;
    private Paint numberStrokePaint;

    private static class TemplateConfig {
        int drawableId;

        int startRow = 675;      // moved DOWN
        int rowHeight = 82;      // slightly taller rows

        int slotX = 120;         // centered in gold slot box
        int nameX = 245;         // safely after slot column

        int booyahX = 610;
        int killX = 710;
        int positionX = 820;
        int totalX = 940;

        int nameSize = 30;
        int numberSize = 34;
    }

    public ResultImageGenerator(Context context) {
        this.context = context;
        setupPaints();
    }

    private void setupPaints() {
        // BLACK text with WHITE stroke for team names
        blackTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackTextPaint.setColor(Color.BLACK);
        blackTextPaint.setTextSize(38);
        blackTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        blackTextPaint.setTextAlign(Paint.Align.LEFT);

        whiteStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteStrokePaint.setColor(Color.WHITE);
        whiteStrokePaint.setTextSize(38);
        whiteStrokePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        whiteStrokePaint.setTextAlign(Paint.Align.LEFT);
        whiteStrokePaint.setStyle(Paint.Style.STROKE);
        whiteStrokePaint.setStrokeWidth(4);

        // BLACK numbers with WHITE stroke
        numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberPaint.setColor(Color.BLACK);
        numberPaint.setTextSize(42);
        numberPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        numberPaint.setTextAlign(Paint.Align.CENTER);

        numberStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberStrokePaint.setColor(Color.WHITE);
        numberStrokePaint.setTextSize(42);
        numberStrokePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        numberStrokePaint.setTextAlign(Paint.Align.CENTER);
        numberStrokePaint.setStyle(Paint.Style.STROKE);
        numberStrokePaint.setStrokeWidth(4);
    }

    private TemplateConfig getTemplateForTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        TemplateConfig config = new TemplateConfig();

        if (hour >= 0 && hour < 6) {
            config.drawableId = R.drawable.points_table_1;
        } else if (hour >= 6 && hour < 12) {
            config.drawableId = R.drawable.points_table_2;
        } else if (hour >= 12 && hour < 18) {
            config.drawableId = R.drawable.points_table_4;
        } else {
            config.drawableId = R.drawable.points_table_3;
        }

        return config;
    }

    public File generateResultImage(List<FinalRow> results, String tournamentId) {
        try {
            TemplateConfig config = getTemplateForTime();

            Bitmap template = BitmapFactory.decodeResource(
                    context.getResources(),
                    config.drawableId
            );

            if (template == null) return null;

            Bitmap mutableBitmap = template.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            for (int i = 0; i < Math.min(results.size(), 12); i++) {
                FinalRow row = results.get(i);
                int yPosition = config.startRow + (i * config.rowHeight);

                // ✅ TEAM NAME - Black with white stroke (left-aligned)
                String teamName = truncateText(row.teamName, 340);

                // Draw stroke first
                canvas.drawText(teamName, config.nameX, yPosition, whiteStrokePaint);
                // Draw black text on top
                canvas.drawText(teamName, config.nameX, yPosition, blackTextPaint);

                // ✅ BOOYAH COUNT - Black with white stroke (center)
                String booyah = String.valueOf(row.booyah);
                canvas.drawText(booyah, config.booyahX, yPosition, numberStrokePaint);
                canvas.drawText(booyah, config.booyahX, yPosition, numberPaint);

                // ✅ KILL POINTS - Black with white stroke (center)
                String kp = String.valueOf(row.kp);
                canvas.drawText(kp, config.killX, yPosition, numberStrokePaint);
                canvas.drawText(kp, config.killX, yPosition, numberPaint);

                // ✅ POSITION POINTS - Black with white stroke (center)
                String pp = String.valueOf(row.pp);
                canvas.drawText(pp, config.positionX, yPosition, numberStrokePaint);
                canvas.drawText(pp, config.positionX, yPosition, numberPaint);

                // ✅ TOTAL POINTS - Black with white stroke (center)
                String total = String.valueOf(row.total);
                canvas.drawText(total, config.totalX, yPosition, numberStrokePaint);
                canvas.drawText(total, config.totalX, yPosition, numberPaint);
            }

            return saveBitmapToFile(mutableBitmap, tournamentId);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public File generateMatchImage(
            List<HostPointsHelper.TeamMatchRow> matchData,
            int matchNumber,
            String tournamentId
    ) {
        try {
            TemplateConfig config = getTemplateForTime();

            Bitmap template = BitmapFactory.decodeResource(
                    context.getResources(),
                    config.drawableId
            );

            if (template == null) return null;

            Bitmap mutableBitmap = template.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            // Match title
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.parseColor("#00FFFF"));
            titlePaint.setTextSize(52);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextAlign(Paint.Align.CENTER);

            Paint titleStroke = new Paint(titlePaint);
            titleStroke.setStyle(Paint.Style.STROKE);
            titleStroke.setStrokeWidth(3);
            titleStroke.setColor(Color.BLACK);

            canvas.drawText("MATCH " + matchNumber, mutableBitmap.getWidth() / 2, 520, titleStroke);
            canvas.drawText("MATCH " + matchNumber, mutableBitmap.getWidth() / 2, 520, titlePaint);

            for (int i = 0; i < Math.min(matchData.size(), 12); i++) {
                HostPointsHelper.TeamMatchRow row = matchData.get(i);
                int yPosition = config.startRow + (i * config.rowHeight);

                // Team name
                String teamName = truncateText(row.team, 340);
                canvas.drawText(teamName, config.nameX, yPosition, whiteStrokePaint);
                canvas.drawText(teamName, config.nameX, yPosition, blackTextPaint);

                // Position (in booyah column)
                String pos = "#" + row.position;
                canvas.drawText(pos, config.booyahX, yPosition, numberStrokePaint);
                canvas.drawText(pos, config.booyahX, yPosition, numberPaint);

                // Kill points
                String kp = String.valueOf(row.kp);
                canvas.drawText(kp, config.killX, yPosition, numberStrokePaint);
                canvas.drawText(kp, config.killX, yPosition, numberPaint);

                // Position points
                String pp = String.valueOf(row.pp);
                canvas.drawText(pp, config.positionX, yPosition, numberStrokePaint);
                canvas.drawText(pp, config.positionX, yPosition, numberPaint);

                // Total
                String total = String.valueOf(row.total);
                canvas.drawText(total, config.totalX, yPosition, numberStrokePaint);
                canvas.drawText(total, config.totalX, yPosition, numberPaint);
            }

            return saveBitmapToFile(mutableBitmap, tournamentId + "_match" + matchNumber);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String truncateText(String text, int maxWidth) {
        if (blackTextPaint.measureText(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && blackTextPaint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }

        return text.substring(0, Math.max(0, end)) + ellipsis;
    }

    private File saveBitmapToFile(Bitmap bitmap, String fileName) {
        try {
            String timeStamp = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
            ).format(System.currentTimeMillis());

            String finalName = "BooyahX_" + fileName + "_" + timeStamp + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, finalName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/BooyahX"
            );

            Uri uri = context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (uri == null) return null;

            OutputStream out =
                    context.getContentResolver().openOutputStream(uri);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            // 🎯 Gallery-visible file reference
            return new File(
                    Environment
                            .getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES
                            ),
                    "BooyahX/" + finalName
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public String getCurrentTheme() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 0 && hour < 6) return "Forest Night";
        else if (hour >= 6 && hour < 12) return "Mystic Blue";
        else if (hour >= 12 && hour < 18) return "Fire Storm";
        else return "Cyber Tech";
    }
}