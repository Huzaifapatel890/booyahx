package com.booyahx;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class LoaderOverlay {

    private static View blurTargetView = null;

    public static void show(Activity activity) {

        View loader = activity.findViewById(R.id.fullLoader);
        if (loader == null) return;

        ViewGroup root = activity.findViewById(android.R.id.content);

        // Get child 0 - this is your activity content
        if (root.getChildCount() > 0) {
            blurTargetView = root.getChildAt(0);
        }

        // Blur ONLY the background content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurTargetView != null) {
                blurTargetView.setRenderEffect(
                        android.graphics.RenderEffect.createBlurEffect(
                                25f, 25f,
                                android.graphics.Shader.TileMode.CLAMP
                        )
                );
            }
        } else {
            // Older devices - just dim
            if (blurTargetView != null) {
                blurTargetView.setAlpha(0.5f);
            }
        }

        // Show loader
        loader.setVisibility(View.VISIBLE);
        loader.bringToFront();

        ImageView ring = loader.findViewById(R.id.loaderRing);
        ImageView glow = loader.findViewById(R.id.loaderGlow);

        // Rotate animation
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(900);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        ring.startAnimation(rotate);

        // Glow pulse
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
        if (loader == null) return;

        ImageView ring = loader.findViewById(R.id.loaderRing);
        ImageView glow = loader.findViewById(R.id.loaderGlow);

        loader.setVisibility(View.GONE);
        ring.clearAnimation();
        glow.clearAnimation();

        // Remove blur
        if (blurTargetView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurTargetView.setRenderEffect(null);
            } else {
                blurTargetView.setAlpha(1f);
            }
        }
    }
}