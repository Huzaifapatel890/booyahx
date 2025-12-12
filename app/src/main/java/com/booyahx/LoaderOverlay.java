package com.booyahx;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class LoaderOverlay {

    public static void show(Activity activity) {

        View loader = activity.findViewById(R.id.fullLoader);
        View blurLayer = activity.findViewById(R.id.blurLayer);

        if (loader == null || blurLayer == null) return;

        // SHOW BLUR + DIM
        blurLayer.setVisibility(View.VISIBLE);
        blurLayer.setBackgroundColor(0xCC000000); // <-- 80% black dim layer

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurLayer.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(
                            28f, 28f,
                            android.graphics.Shader.TileMode.CLAMP
                    )
            );
        } else {
            blurLayer.setAlpha(0.5f); // fallback dim for old devices
        }

        // SHOW LOADER ABOVE EVERYTHING
        loader.setVisibility(View.VISIBLE);
        loader.bringToFront();
        loader.setClickable(true);

        ImageView ring = loader.findViewById(R.id.loaderRing);
        ImageView glow = loader.findViewById(R.id.loaderGlow);

        // ROTATE RING
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(900);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        ring.startAnimation(rotate);

        // GLOW PULSE
        ScaleAnimation pulse = new ScaleAnimation(
                1f, 1.25f,
                1f, 1.25f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(850);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        glow.startAnimation(pulse);
    }


    public static void hide(Activity activity) {

        View loader = activity.findViewById(R.id.fullLoader);
        View blurLayer = activity.findViewById(R.id.blurLayer);

        if (loader == null || blurLayer == null) return;

        loader.setVisibility(View.GONE);

        ImageView ring = loader.findViewById(R.id.loaderRing);
        ImageView glow = loader.findViewById(R.id.loaderGlow);
        ring.clearAnimation();
        glow.clearAnimation();

        // REMOVE BLUR + DIM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurLayer.setRenderEffect(null);
        }

        blurLayer.setVisibility(View.GONE);
        blurLayer.setAlpha(1f);
    }
}