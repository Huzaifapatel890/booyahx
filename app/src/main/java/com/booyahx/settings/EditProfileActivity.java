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
import com.booyahx.TokenManager;
import com.booyahx.LoaderOverlay;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.network.models.UpdateProfileRequest;
import com.booyahx.network.models.SimpleResponse;
import com.booyahx.utils.CSRFHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    ImageView btnBack;

    EditText etName, etAge, etGameName, etPhone, etUpiId, etEmail, etRole;
    TextView btnAgePlus, btnAgeMinus, btnCancel, btnUpdate;

    Spinner spinnerGender, spinnerPaymentMethod;

    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_profile);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        api = ApiClient.getClient(this).create(ApiService.class);

        initViews();
        setupGenderSpinner();
        setupPaymentMethodSpinner();
        setupAgeButtons();
        setupButtons();

        loadProfile();   // ⭐ Load data from server
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

    // ⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐
    // ⭐ GET PROFILE & LOAD VALUES
    // ⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐
    private void loadProfile() {

        LoaderOverlay.show(this);

        String token = TokenManager.getAccessToken(this);

        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {

                LoaderOverlay.hide(EditProfileActivity.this);

                if (response.isSuccessful() && response.body() != null && response.body().success) {

                    ProfileResponse.Data d = response.body().data;

                    etName.setText(d.name);
                    etEmail.setText(d.email);
                    etGameName.setText(d.ign);
                    etPhone.setText(d.phoneNumber);
                    etUpiId.setText(d.paymentUPI);

                    int age = (d.age <= 0) ? 18 : d.age;
                    etAge.setText(String.valueOf(age));

                    etRole.setText("User");

                    setSpinnerSelection(spinnerGender, d.gender);
                    setSpinnerSelection(spinnerPaymentMethod, d.paymentMethod);

                } else {

                    // ⭐ Try to read server-provided message
                    String serverMsg = null;
                    try { serverMsg = response.errorBody().string(); } catch (Exception ignored) {}

                    if (serverMsg != null && !serverMsg.isEmpty())
                        showTopRightToast(serverMsg);
                    else
                        showTopRightToast("Failed to load profile!");
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                LoaderOverlay.hide(EditProfileActivity.this);
                showTopRightToast("Error loading profile!");
            }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null) return;
        int pos = adapter.getPosition(value);
        if (pos >= 0) spinner.setSelection(pos);
    }

    // ---------------------------------------------------------
    // DEFAULT SPINNERS
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
    // BUTTONS (PUT API)
    // ---------------------------------------------------------
    private void setupButtons() {

        btnCancel.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> {

            if (!validateInputs()) return;

            updateProfile(); // ⭐ REAL UPDATE
        });
    }

    // ---------------------------------------------------------
    // ⭐ PUT UPDATE PROFILE API
    // ---------------------------------------------------------
    private void updateProfile() {

        LoaderOverlay.show(this);   // SHOW BEFORE CSRF CALL

        String token = TokenManager.getAccessToken(this);

        String genderFixed = spinnerGender.getSelectedItem().toString().toLowerCase();
        String upiClean = etUpiId.getText().toString().trim().replace("-", "").replace(" ", "");

        UpdateProfileRequest req = new UpdateProfileRequest(
                etName.getText().toString().trim(),
                etGameName.getText().toString().trim(),
                genderFixed,
                Integer.parseInt(etAge.getText().toString().trim()),
                etPhone.getText().toString().trim(),
                upiClean,
                spinnerPaymentMethod.getSelectedItem().toString()
        );

        // ⭐ FETCH CSRF TOKEN FIRST
        CSRFHelper.fetchToken(this, new CSRFHelper.CSRFCallback() {

            @Override
            public void onSuccess(String csrf) {

                api.updateProfile(
                        "Bearer " + token,
                        csrf,
                        req
                ).enqueue(new Callback<SimpleResponse>() {
                    @Override
                    public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {

                        LoaderOverlay.hide(EditProfileActivity.this);

                        if (response.isSuccessful() && response.body() != null) {
                            showTopRightToast(response.body().getMessage());
                            finish();
                        } else {

                            String serverMsg = null;
                            try { serverMsg = response.errorBody().string(); } catch (Exception ignored) {}

                            if (serverMsg != null && !serverMsg.isEmpty())
                                showTopRightToast(serverMsg);
                            else
                                showTopRightToast("Update failed!");
                        }
                    }

                    @Override
                    public void onFailure(Call<SimpleResponse> call, Throwable t) {
                        LoaderOverlay.hide(EditProfileActivity.this);
                        showTopRightToast("Error updating profile!");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                LoaderOverlay.hide(EditProfileActivity.this);
                showTopRightToast(error != null ? error : "Security error! Try again.");
            }
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

        if (phone.isEmpty() || phone.length() != 10 || !phone.matches("\\d{10}")) {
            showTopRightToast("Invalid phone number");
            return false;
        }

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