package com.booyahx.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.BlurMaskFilter;

/**
 * Generates neon-style initials avatars matching app's cyan/blue theme
 */
public class AvatarGenerator {

    private static final int NEON_CYAN = 0xFF00D9FF;  // Bright cyan like your app
    private static final int BLACK_BG = 0xFF000000;

    private static final int[] NEON_COLORS = {
            0xFF00D9FF, // Cyan (primary)
            0xFF00FFB3, // Mint green
            0xFFFF00E5, // Magenta
            0xFF9D00FF, // Purple
            0xFFFFD600, // Gold
            0xFFFF3D00, // Orange-red
            0xFF00FF88, // Emerald
            0xFF0099FF, // Blue
            0xFFFF0099, // Pink
            0xFF00FFFF, // Aqua
            0xFFBB00FF, // Violet
            0xFFFFFF00  // Yellow
    };

    /**
     * Generate neon-style avatar with black background and cyan border
     */
    public static Bitmap generateAvatar(String name, int sizeDp, Context context) {
        if (name == null || name.trim().isEmpty()) {
            name = "U";
        }

        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);

        String initials = getInitials(name);
        int neonColor = getColorForName(name);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float centerX = sizePx / 2f;
        float centerY = sizePx / 2f;

        // Add padding to prevent glow cutoff
        float padding = sizePx * 0.08f; // 8% padding
        float radius = (sizePx / 2f) - padding;

        // Draw black background circle
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(BLACK_BG);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, radius, bgPaint);

        // Draw neon glow effect (outer glow)
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(neonColor);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(sizePx * 0.04f);
        glowPaint.setMaskFilter(new BlurMaskFilter(sizePx * 0.05f, BlurMaskFilter.Blur.OUTER));
        canvas.drawCircle(centerX, centerY, radius, glowPaint);

        // Draw neon border (solid ring)
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(neonColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(sizePx * 0.05f); // Slightly thinner
        canvas.drawCircle(centerX, centerY, radius, borderPaint);

        // Draw initials with stylish font
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sizePx * 0.35f); // Slightly smaller to fit better
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setLetterSpacing(0.05f);

        // Add subtle text glow
        textPaint.setShadowLayer(sizePx * 0.02f, 0, 0, neonColor);

        // Calculate text position (centered vertically)
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f;

        canvas.drawText(initials, centerX, textY, textPaint);

        return bitmap;
    }

    /**
     * Extract initials from name
     * "DEROXY FF" -> "DF"
     * "DEROXY" -> "D"
     */
    private static String getInitials(String name) {
        name = name.trim().toUpperCase();
        String[] parts = name.split("\\s+");

        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0);
        } else if (parts.length == 1 && parts[0].length() > 0) {
            return String.valueOf(parts[0].charAt(0));
        }

        return "U";
    }

    /**
     * Get consistent neon color based on name hash
     */
    private static int getColorForName(String name) {
        int hash = 0;
        for (char c : name.toCharArray()) {
            hash = hash * 31 + c;
        }
        int index = Math.abs(hash) % NEON_COLORS.length;
        return NEON_COLORS[index];
    }

    /**
     * Convenience method that gets name from ProfileCacheManager
     */
    public static Bitmap generateAvatarFromCache(Context context, int sizeDp) {
        String name = com.booyahx.ProfileCacheManager.getName(context);
        return generateAvatar(name, sizeDp, context);
    }
}