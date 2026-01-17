package com.booyahx.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.R;

public class AboutActivity {
    public static class AboutUsActivity extends AppCompatActivity {

        // 🔥 SOCIAL MEDIA LINKS
        private static final String INSTAGRAM_URL = "https://www.instagram.com/_booyah_x__?igsh=ejhmODc2cnF4eHJ6";
        private static final String WHATSAPP_URL = "https://whatsapp.com/channel/0029VbC0xQyA2pLKBc1nqI3v";
        private static final String TELEGRAM_URL = "https://t.me/gethelpbooyahx";
        private static final String DISCORD_URL = "https://discord.gg/booyahx"; // 🔥 PLACEHOLDER - REPLACE WITH REAL LINK

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_about_us);

            // Enable action bar back
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }

            // Click listener for custom back button
            findViewById(R.id.btnBack).setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });

            // 🔥 SETUP SOCIAL MEDIA BUTTONS
            setupSocialMediaButtons();
        }

        // 🔥 SETUP SOCIAL MEDIA CLICK LISTENERS
        private void setupSocialMediaButtons() {
            LinearLayout btnInstagram = findViewById(R.id.btnInstagram);
            LinearLayout btnWhatsApp = findViewById(R.id.btnWhatsApp);
            LinearLayout btnTelegram = findViewById(R.id.btnTelegram);
            LinearLayout btnDiscord = findViewById(R.id.btnDiscord);

            btnInstagram.setOnClickListener(v -> openSocialMedia(INSTAGRAM_URL, "Instagram"));
            btnWhatsApp.setOnClickListener(v -> openSocialMedia(WHATSAPP_URL, "WhatsApp"));
            btnTelegram.setOnClickListener(v -> openSocialMedia(TELEGRAM_URL, "Telegram"));
            btnDiscord.setOnClickListener(v -> openSocialMedia(DISCORD_URL, "Discord"));
        }

        // 🔥 OPEN SOCIAL MEDIA LINK
        private void openSocialMedia(String url, String platformName) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "Unable to open " + platformName + ". Please try again.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == android.R.id.home) {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void finish() {
            super.finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}