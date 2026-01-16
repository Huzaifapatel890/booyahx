package com.booyahx.Host;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import com.booyahx.R;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HtmlResultGenerator {

    private final Context context;

    public HtmlResultGenerator(Context context) {
        this.context = context;
    }

    public File generateResultImage(List<FinalRow> rows, String name) {
        try {
            View root = LayoutInflater.from(context)
                    .inflate(R.layout.layout_points_table_render, null, false);

            LinearLayout rowsContainer = root.findViewById(R.id.rowsContainer);
            root.findViewById(R.id.bgImage)
                    .setBackgroundResource(R.drawable.forest_theme); // YOUR IMAGE

            for (int i = 0; i < 12; i++) {
                View rowView = buildRow(i, i < rows.size() ? rows.get(i) : null);
                rowsContainer.addView(rowView);
            }

            Bitmap bmp = render(root, 1080, 1920);
            return saveBitmap(bmp, name);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private View buildRow(int index, FinalRow row) {
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 78));

        rowLayout.addView(cell(80, row == null ? "" : String.format("%02d", index + 1)));
        rowLayout.addView(cell(380, row == null ? "" : row.teamName));
        rowLayout.addView(cell(130, row == null ? "" : String.valueOf(row.booyah)));
        rowLayout.addView(cell(130, row == null ? "" : String.valueOf(row.kp)));
        rowLayout.addView(cell(150, row == null ? "" : String.valueOf(row.pp)));
        rowLayout.addView(cell(160, row == null ? "" : String.valueOf(row.total)));

        return rowLayout;
    }

    private TextView cell(int width, String text) {
        TextView tv = new TextView(context);
        tv.setWidth(width);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(8, 4, 8, 4);

        // Stroke
        tv.setShadowLayer(4, 0, 0, Color.BLACK);
        return tv;
    }

    private Bitmap render(View v, int w, int h) {
        v.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);

        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }

    private File saveBitmap(Bitmap bmp, String name) throws Exception {
        String fileName = "BooyahX_" + name + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".png";

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
}