package com.booyahx.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.booyahx.R;
import com.booyahx.notifications.NotificationActivity;

/**
 * InAppNotificationBanner
 *
 * Shows a sleek top-of-screen banner with:
 *  - Fade IN  (400ms)
 *  - Visible  (2500ms)
 *  - Fade OUT (500ms)
 *
 * Clicking the banner opens NotificationActivity.
 *
 * USAGE:
 *   InAppNotificationBanner.show(this, "Title Here", "Your message here");
 */
public class InAppNotificationBanner {

    private static final long FADE_IN_DURATION  = 400L;
    private static final long VISIBLE_DURATION  = 2500L;
    private static final long FADE_OUT_DURATION = 500L;

    public static void show(Activity activity, String title, String message) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        // If a banner is already showing, just update its text — no stacking
        View existingBanner = rootView.findViewWithTag("IN_APP_BANNER");
        if (existingBanner != null) {
            TextView tvTitle   = existingBanner.findViewById(R.id.bannerTitle);
            TextView tvMessage = existingBanner.findViewById(R.id.bannerMessage);
            if (tvTitle   != null) tvTitle.setText(title);
            if (tvMessage != null) tvMessage.setText(message);
            return;
        }

        // Inflate banner layout
        View bannerView = LayoutInflater.from(activity)
                .inflate(R.layout.notification_banner, rootView, false);
        bannerView.setTag("IN_APP_BANNER");

        // Set text
        TextView tvTitle   = bannerView.findViewById(R.id.bannerTitle);
        TextView tvMessage = bannerView.findViewById(R.id.bannerMessage);
        if (tvTitle   != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);

        // Add at top of view hierarchy (above all fragments/views)
        rootView.addView(bannerView, 0);
        bannerView.setVisibility(View.VISIBLE);
        bannerView.setAlpha(0f);

        // ✅ FIX: Set click listener on bannerRoot (the inner LinearLayout that has
        // android:clickable="true"), NOT on the CardView wrapper.
        // The LinearLayout intercepts touch first — so this is where the listener must live.
        View bannerRoot = bannerView.findViewById(R.id.bannerRoot);
        if (bannerRoot != null) {
            bannerRoot.setOnClickListener(v -> {
                dismissBanner(rootView, bannerView);
                Intent intent = new Intent(activity, NotificationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            });
        }

        // FADE IN
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(bannerView, "alpha", 0f, 1f);
        fadeIn.setDuration(FADE_IN_DURATION);
        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Hold visible, then FADE OUT
                bannerView.postDelayed(() -> {
                    if (bannerView.getParent() == null) return;

                    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(bannerView, "alpha", 1f, 0f);
                    fadeOut.setDuration(FADE_OUT_DURATION);
                    fadeOut.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            removeBanner(rootView, bannerView);
                        }
                    });
                    fadeOut.start();

                }, VISIBLE_DURATION);
            }
        });
        fadeIn.start();
    }

    // Quick fade-out on click before opening NotificationActivity
    private static void dismissBanner(ViewGroup root, View banner) {
        banner.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> removeBanner(root, banner))
                .start();
    }

    private static void removeBanner(ViewGroup root, View banner) {
        banner.setTag(null);
        if (banner.getParent() != null) {
            root.removeView(banner);
        }
    }
}