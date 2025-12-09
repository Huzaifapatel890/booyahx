package com.booyahx;

import android.app.Activity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class LoaderOverlay {

    public static void show(Activity activity) {

        View loader = activity.findViewById(R.id.fullLoader);
        if (loader == null) return;

        ImageView ring = loader.findViewById(R.id.loaderRing);
        ImageView glow = loader.findViewById(R.id.loaderGlow);

        loader.setVisibility(View.VISIBLE);
        loader.bringToFront();     // Always in front
        loader.setClickable(true); // Block touches

        // Rotate the neon ring
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setDuration(900);
        rotate.setInterpolator(new LinearInterpolator());
        ring.startAnimation(rotate);

        // Glow pulse animation
        ScaleAnimation pulse = new ScaleAnimation(
                1f, 1.25f,
                1f, 1.25f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setDuration(850);
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
    }
}