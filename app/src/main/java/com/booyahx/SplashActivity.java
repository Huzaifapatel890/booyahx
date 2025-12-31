package com.booyahx;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

public class SplashActivity extends AppCompatActivity {

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // REMOVE HEADER
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        ImageView b = findViewById(R.id.letter_b);
        ImageView o = findViewById(R.id.letter_o);
        ImageView o2 = findViewById(R.id.letter_o2);
        ImageView y = findViewById(R.id.letter_y);
        ImageView a = findViewById(R.id.letter_a);
        ImageView h = findViewById(R.id.letter_h);
        ImageView x = findViewById(R.id.logo_x);

        ImageView[] letters = {b, o, o2, y, a, h};

        long delay = 250; // drop spacing

        // DROP ANIMATION
        for (int i = 0; i < letters.length; i++) {
            int index = i;

            new Handler().postDelayed(() -> {

                letters[index].setAlpha(1f);
                letters[index].setScaleX(1f);
                letters[index].setScaleY(1f);
                letters[index].animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(450)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();

                vibrateMini();

            }, delay * index);
        }

        // X logo drop
        new Handler().postDelayed(() -> {

            x.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(550)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            vibrateBoom();

        }, 1800);

        // AFTER animations → Decide where to go
        new Handler().postDelayed(this::checkLoginStatus, 3000);
    }

    // =========================
    //   CHECK TOKEN / REDIRECT
    // =========================
    private void checkLoginStatus() {

        // ✔ Use TokenManager instead of old SharedPreferences
        String accessToken = TokenManager.getAccessToken(this);

        if (accessToken != null && !accessToken.isEmpty()) {

            // USER LOGGED IN → GO DASHBOARD
            Intent i = new Intent(SplashActivity.this, DashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);

        } else {

            // NO TOKEN → GO LOGIN
            Intent i = new Intent(SplashActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }

        finish();
    }

    // =========================
    // VIBRATION HELPERS
    // =========================
    private void vibrateMini() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, 200));
        } else {
            vibrator.vibrate(25);
        }
    }

    private void vibrateBoom() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(120, 255));
        } else {
            vibrator.vibrate(120);
        }
    }
}
