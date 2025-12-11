package com.booyahx.settings;

import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.R;

public class ActivityChangePassword {
    public static class ChangePasswordActivity extends AppCompatActivity {

        EditText edtOldPassword, edtNewPassword, edtConfirmPassword;
        TextView btnChangePassword;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.change_password);

            // Enable ActionBar back button (same as AboutUsActivity)
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }

            edtOldPassword = findViewById(R.id.edtOldPassword);
            edtNewPassword = findViewById(R.id.edtNewPassword);
            edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
            btnChangePassword = findViewById(R.id.btnChangePassword);

            // Custom back button (blue arrow)
            findViewById(R.id.btnBack).setOnClickListener(v -> finishWithAnim());

            // Change Password Button
            btnChangePassword.setOnClickListener(v -> validateInputs());
        }

        // ----------------------------------------------------
        // VALIDATION + TOASTS
        // ----------------------------------------------------
        private void validateInputs() {

            String oldPass = edtOldPassword.getText().toString().trim();
            String newPass = edtNewPassword.getText().toString().trim();
            String confirmPass = edtConfirmPassword.getText().toString().trim();

            if (oldPass.isEmpty()) {
                showTopRightToast("Enter old password");
                return;
            }

            if (newPass.isEmpty()) {
                showTopRightToast("Enter new password");
                return;
            }

            if (confirmPass.isEmpty()) {
                showTopRightToast("Confirm your new password");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                showTopRightToast("Passwords do not match");
                return;
            }

            // TODO: CALL CHANGE PASSWORD API HERE
            showTopRightToast("Changing password...");
        }

        // ----------------------------------------------------
        // CUSTOM NEON TOAST (YOUR EXACT TOAST FROM LoginActivity)
        // ----------------------------------------------------
        private void showTopRightToast(String message) {
            TextView tv = new TextView(this);
            tv.setText(message);
            tv.setPadding(40, 25, 40, 25);
            tv.setTextColor(0xFFFFFFFF);
            tv.setBackgroundResource(R.drawable.toast_bg);
            tv.setTextSize(14);

            Toast toast = new Toast(getApplicationContext());
            toast.setView(tv);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP | Gravity.END, 40, 120);

            AlphaAnimation fade = new AlphaAnimation(0f, 1f);
            fade.setDuration(350);
            tv.startAnimation(fade);

            toast.show();
        }

        // ----------------------------------------------------
        // ACTIONBAR BACK BUTTON
        // ----------------------------------------------------
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == android.R.id.home) {
                finishWithAnim();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        // ----------------------------------------------------
        // SLIDE EXIT ANIMATION
        // ----------------------------------------------------
        private void finishWithAnim() {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }

        @Override
        public void finish() {
            super.finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}
