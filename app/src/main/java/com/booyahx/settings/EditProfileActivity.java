package com.booyahx.settings;

import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.R;

public class EditProfileActivity extends AppCompatActivity {

    ImageView btnBack;

    EditText etName, etAge, etGameName, etPhone, etUpiId, etEmail, etRole;
    TextView btnAgePlus, btnAgeMinus, btnCancel, btnUpdate;

    Spinner spinnerGender, spinnerPaymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_profile);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        initViews();
        setupGenderSpinner();
        setupPaymentMethodSpinner();
        setupAgeButtons();
        setupButtons();
    }

    private void initViews() {

        btnBack = findViewById(R.id.btnBack);

        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etGameName = findViewById(R.id.etGameName);
        etPhone = findViewById(R.id.etPhone);
        etUpiId = findViewById(R.id.etUpiId);
        etEmail = findViewById(R.id.etEmail);
        etRole = findViewById(R.id.etRole);

        btnAgePlus = findViewById(R.id.btnAgePlus);
        btnAgeMinus = findViewById(R.id.btnAgeMinus);

        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);

        btnCancel = findViewById(R.id.btnCancel);
        btnUpdate = findViewById(R.id.btnUpdate);

        etEmail.setText("user@gmail.com");
        etRole.setText("User");

        btnBack.setOnClickListener(v -> finish());
    }

    // ---------------------------------------------------------
    // DEFAULT SPINNER ADAPTER (NO CUSTOM LAYOUTS)
    // ---------------------------------------------------------
    private void setupGenderSpinner() {
        String[] genders = {"Male", "Female", "Other", "Prefer not to say"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genders
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupPaymentMethodSpinner() {

        String[] methods = {
                "UPI",
                "Bank Transfer",
                "Credit Card",
                "Debit Card",
                "Wallet",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                methods
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(adapter);
    }

    // ---------------------------------------------------------
    // AGE BUTTONS
    // ---------------------------------------------------------
    private void setupAgeButtons() {

        btnAgePlus.setOnClickListener(v -> {
            String ageStr = etAge.getText().toString().trim();
            int age = ageStr.isEmpty() ? 18 : Integer.parseInt(ageStr);
            age++;
            if (age > 100) age = 100;
            etAge.setText(String.valueOf(age));
        });

        btnAgeMinus.setOnClickListener(v -> {
            String ageStr = etAge.getText().toString().trim();
            int age = ageStr.isEmpty() ? 18 : Integer.parseInt(ageStr);
            age--;
            if (age < 12) age = 12;
            etAge.setText(String.valueOf(age));
        });
    }

    // ---------------------------------------------------------
    // BUTTONS
    // ---------------------------------------------------------
    private void setupButtons() {

        btnCancel.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> {

            if (!validateInputs()) return;

            showTopRightToast("Profile Updated Successfully!");
        });
    }

    // ---------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------
    private boolean validateInputs() {

        String name = etName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String gameName = etGameName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String upi = etUpiId.getText().toString().trim();

        if (name.isEmpty()) {
            showTopRightToast("Name is required");
            return false;
        }

        if (age.isEmpty() || Integer.parseInt(age) < 12) {
            showTopRightToast("Age must be 12+");
            return false;
        }

        if (gameName.isEmpty()) {
            showTopRightToast("In-game name is required");
            return false;
        }

        // -------- PHONE VALIDATION --------
        if (phone.isEmpty()) {
            showTopRightToast("Phone number required");
            return false;
        }

        if (phone.length() != 10) {
            showTopRightToast("Phone number must be exactly 10 digits");
            return false;
        }

        if (!phone.matches("\\d{10}")) {
            showTopRightToast("Invalid phone number format");
            return false;
        }
        // ----------------------------------

        if (upi.isEmpty()) {
            showTopRightToast("UPI ID required");
            return false;
        }

        return true;
    }

    // ---------------------------------------------------------
    // CUSTOM NEON TOAST
    // ---------------------------------------------------------
    private void showTopRightToast(String message) {

        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(40, 25, 40, 25);
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackgroundResource(R.drawable.toast_bg);
        tv.setTextSize(14);

        android.widget.Toast toast = new android.widget.Toast(getApplicationContext());
        toast.setView(tv);
        toast.setDuration(android.widget.Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.END, 40, 120);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(350);
        tv.startAnimation(fade);

        toast.show();
    }
}