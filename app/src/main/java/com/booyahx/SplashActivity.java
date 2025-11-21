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

        long delay = 250; // letter drop spacing

        // DROP LETTERS + mini vibration pop each time
        for (int i = 0; i < letters.length; i++) {
            int index = i;

            new Handler().postDelayed(() -> {

                // animate drop
                letters[index].animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(450)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();

                // mini pop vibration
                vibrateMini();

            }, delay * index);
        }

        // X DROP + strong vibration impact
        new Handler().postDelayed(() -> {

            x.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(550)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            vibrateBoom(); // BIG pulse for main logo

        }, 1800);

        // Move to login
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, 3000);
    }

    // ====== VIBRATION FUNCTIONS ======

    private void vibrateMini() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    25,  // duration
                    200  // amplitude (light pop)
            ));
        } else {
            vibrator.vibrate(25);
        }
    }

    private void vibrateBoom() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    120,  // duration
                    255   // maximum amplitude = BOOM effect
            ));
        } else {
            vibrator.vibrate(120);
        }
    }
}