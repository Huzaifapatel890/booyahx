package com.booyahx.settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.R;

public class AboutActivity {
    public static class AboutUsActivity extends AppCompatActivity {

        // 🔥 SOCIAL MEDIA LINKS
        private static final String INSTAGRAM_URL = "https://www.instagram.com/_booyah_x__?igsh=ejhmODc2cnF4eHJ6";
        private static final String WHATSAPP_URL = "https://whatsapp.com/channel/0029VbC0xQyA2pLKBc1nqI3v";
        private static final String TELEGRAM_URL = "https://t.me/gethelpbooyahx";
        private static final String DISCORD_URL = "https://discord.gg/PcNepCdd";

        // Language toggle state
        private boolean isHindi = false;

        // UI elements
        private ImageView btnLanguageToggle;
        private ScrollView contentContainer;
        private TextView tvTitle;
        private TextView tvDescription;
        private TextView tvDesignedFor;
        private TextView tvFeature1Title;
        private TextView tvFeature1Desc;
        private TextView tvFeature2Title;
        private TextView tvFeature2Desc;
        private TextView tvFeature3Title;
        private TextView tvFeature3Desc;
        private TextView tvMissionTitle;
        private TextView tvMissionDesc;
        private TextView tvFollowTitle;
        private TextView tvFollowDesc;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_about_us);

            // Enable action bar back
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }

            // Initialize UI elements
            initializeViews();

            // Click listener for custom back button
            findViewById(R.id.btnBack).setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });

            // Language toggle button with animation
            btnLanguageToggle.setOnClickListener(v -> {
                animateLanguageToggle();
                toggleLanguage();
            });

            // 🔥 SETUP SOCIAL MEDIA BUTTONS
            setupSocialMediaButtons();
        }

        private void initializeViews() {
            btnLanguageToggle = findViewById(R.id.btnLanguageToggle);
            contentContainer = findViewById(R.id.contentContainer); // Add this ID to your main content LinearLayout in XML
            tvTitle = findViewById(R.id.tvTitle);
            tvDescription = findViewById(R.id.tvDescription);
            tvDesignedFor = findViewById(R.id.tvDesignedFor);
            tvFeature1Title = findViewById(R.id.tvFeature1Title);
            tvFeature1Desc = findViewById(R.id.tvFeature1Desc);
            tvFeature2Title = findViewById(R.id.tvFeature2Title);
            tvFeature2Desc = findViewById(R.id.tvFeature2Desc);
            tvFeature3Title = findViewById(R.id.tvFeature3Title);
            tvFeature3Desc = findViewById(R.id.tvFeature3Desc);
            tvMissionTitle = findViewById(R.id.tvMissionTitle);
            tvMissionDesc = findViewById(R.id.tvMissionDesc);
            tvFollowTitle = findViewById(R.id.tvFollowTitle);
            tvFollowDesc = findViewById(R.id.tvFollowDesc);
        }

        // 🔥 COOL ANIMATION FOR LANGUAGE TOGGLE
        private void animateLanguageToggle() {
            // Rotate the language toggle button
            ObjectAnimator rotateButton = ObjectAnimator.ofFloat(btnLanguageToggle, "rotation", 0f, 360f);
            rotateButton.setDuration(500);
            rotateButton.setInterpolator(new AccelerateDecelerateInterpolator());
            rotateButton.start();

            // Fade out content
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contentContainer, "alpha", 1f, 0f);
            fadeOut.setDuration(250);
            fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());

            // Slide content slightly to the right while fading out
            ObjectAnimator slideOut = ObjectAnimator.ofFloat(contentContainer, "translationX", 0f, 30f);
            slideOut.setDuration(250);
            slideOut.setInterpolator(new AccelerateDecelerateInterpolator());

            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Update content after fade out
                    updateContent();

                    // Reset position for slide in
                    contentContainer.setTranslationX(-30f);

                    // Fade in content
                    ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contentContainer, "alpha", 0f, 1f);
                    fadeIn.setDuration(250);
                    fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

                    // Slide content back to original position
                    ObjectAnimator slideIn = ObjectAnimator.ofFloat(contentContainer, "translationX", -30f, 0f);
                    slideIn.setDuration(250);
                    slideIn.setInterpolator(new AccelerateDecelerateInterpolator());

                    fadeIn.start();
                    slideIn.start();
                }
            });

            fadeOut.start();
            slideOut.start();
        }

        private void toggleLanguage() {
            isHindi = !isHindi;
            // Content update is now called inside animation
        }

        private void updateContent() {
            if (isHindi) {
                // Switch to Hindi
                tvTitle.setText("हमारे बारे में – BooyahX");
                tvDescription.setText("BooyahX एक नेक्स्ट-जेन एस्पोर्ट्स प्लेटफॉर्म है जो Free Fire खिलाड़ियों को पेड लॉबी, कस्टम रूम, दैनिक मैच, टूर्नामेंट और LAN इवेंट्स का बेहतरीन अनुभव प्रदान करता है।\n\nहमारा मिशन सरल है — भारत के प्रतिस्पर्धी गेमर्स को एक सुरक्षित, निष्पक्ष और पारदर्शी बैटलग्राउंड देना जहां वे अपने कौशल दिखा सकें और एस्पोर्ट्स में आगे बढ़ सकें।");
                tvDesignedFor.setText("BooyahX विशेष रूप से डिज़ाइन किया गया है:");

                tvFeature1Title.setText("🎮 Free Fire पेड लॉबी और कस्टम रूम");
                tvFeature1Desc.setText("• तेज़ और सुरक्षित रूम एक्सेस\n• सत्यापित और विश्वसनीय होस्ट\n• निष्पक्ष गेमप्ले और एंटी-चीट फोकस\n• तुरंत परिणाम और भुगतान");

                tvFeature2Title.setText("🏆 टूर्नामेंट आयोजक");
                tvFeature2Desc.setText("• दैनिक और साप्ताहिक ऑनलाइन टूर्नामेंट\n• मैच नियमों में 100% पारदर्शिता\n• ऑटो ब्रैकेट, परिणाम और लीडरबोर्ड\n• सहज पंजीकरण प्रणाली");

                tvFeature3Title.setText("🖥 LAN टूर्नामेंट आयोजक (केवल प्रीपेड)");
                tvFeature3Desc.setText("• ऑन-ग्राउंड एस्पोर्ट्स अनुभव\n• पूरी तरह से प्रबंधित इवेंट सपोर्ट\n• पेशेवर मैच हैंडलिंग\n• प्रतिस्पर्धी स्टेज वातावरण");

                tvMissionTitle.setText("🚀 एस्पोर्ट्स को बढ़ावा देने के लिए काम कर रहे हैं");
                tvMissionDesc.setText("BooyahX का मुख्य लक्ष्य भारत के युवा गेमर्स को एस्पोर्ट्स का असली एक्सपोज़र देना है। हम कम्युनिटी इवेंट्स, पेड लॉबी और टूर्नामेंट के माध्यम से ग्रासरूट एस्पोर्ट्स को बढ़ावा दे रहे हैं — ताकि हर खिलाड़ी को बड़ा होने का समान मौका मिले।");

                tvFollowTitle.setText("📱 हमें फॉलो करें");
                tvFollowDesc.setText("नवीनतम अपडेट, टूर्नामेंट और कम्युनिटी इवेंट्स के लिए BooyahX से जुड़े रहें!");

            } else {
                // Switch to English
                tvTitle.setText("About Us – BooyahX");
                tvDescription.setText("BooyahX ek next-gen esports platform hai jo Free Fire players ko paid lobbies, custom rooms, daily matches, tournaments, aur LAN events ka best experience provide karta hai.\n\nHumara mission simple hai — India ke competitive gamers ko ek safe, fair aur transparent battleground dena jahan wo apne skills dikha sakein aur esports me grow kar sakein.");
                tvDesignedFor.setText("BooyahX specially design kiya gaya hai:");

                tvFeature1Title.setText("🎮 Free Fire Paid Lobbies and Custom Rooms");
                tvFeature1Desc.setText("• Fast aur secure room access\n• Verified and trusted hosts\n• Fair gameplay and anti-cheat focus\n• Instant results and payouts");

                tvFeature2Title.setText("🏆 Tournament Organizer");
                tvFeature2Desc.setText("• Daily and weekly online tournaments\n• 100% transparency in match rules\n• Auto bracket, results and leaderboard\n• Smooth registration system");

                tvFeature3Title.setText("🖥 LAN Tournament Organizer (Prepaid Only)");
                tvFeature3Desc.setText("• On-ground esports experience\n• Fully managed event support\n• Professional match handling\n• Competitive stage environment");

                tvMissionTitle.setText("🚀 Working to Promote Esports");
                tvMissionDesc.setText("BooyahX ka main goal India ke young gamers ko esports ka real exposure dena hai. Hum community events, paid lobbies aur tournaments ke through grassroot esports ko promote kar rahe hain — taaki har player ko bada hone ka equal chance mile.");

                tvFollowTitle.setText("📱 Follow Us");
                tvFollowDesc.setText("Stay connected with BooyahX for latest updates, tournaments, and community events!");
            }
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