package com.booyahx.settings;

import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.ProfileCacheManager;
import com.booyahx.R;
import com.booyahx.LoaderOverlay;
import com.booyahx.adapters.UniversalSpinnerAdapter;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.network.models.UpdateProfileRequest;
import com.booyahx.network.models.SimpleResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    ImageView btnBack;

    EditText etName, etAge, etGameName, etPhone, etUpiId, etEmail, etRole;
    TextView btnAgePlus, btnAgeMinus, btnCancel, btnUpdate;

    Spinner spinnerGender;
    UniversalSpinnerAdapter genderAdapter;

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
        setupAgeButtons();
        setupButtons();

        // 🔥 LOAD FROM CACHE FIRST (INSTANT), THEN API (BACKGROUND)
        loadProfileFromCache();
        loadProfileFromAPI();
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

        btnCancel = findViewById(R.id.btnCancel);
        btnUpdate = findViewById(R.id.btnUpdate);

        btnBack.setOnClickListener(v -> finish());
    }

    // 🔥 LOAD FROM CACHE (INSTANT)
    private void loadProfileFromCache() {
        ProfileResponse.Data profile = ProfileCacheManager.getProfile(this);

        if (profile != null) {
            populateFields(profile);
        }
    }

    // 🔥 LOAD FROM API (BACKGROUND - ENSURES FRESH DATA)
    private void loadProfileFromAPI() {
        LoaderOverlay.show(this);

        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                LoaderOverlay.hide(EditProfileActivity.this);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().success) {

                    ProfileResponse.Data data = response.body().data;

                    // 🔥 UPDATE CACHE
                    ProfileCacheManager.saveProfile(EditProfileActivity.this, data);

                    // Update UI
                    populateFields(data);

                } else {
                    String serverMsg = null;
                    try {
                        serverMsg = response.errorBody().string();
                    } catch (Exception ignored) {}

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

    private void populateFields(ProfileResponse.Data d) {
        etName.setText(d.name);
        etEmail.setText(d.email);
        etGameName.setText(d.ign);
        etPhone.setText(d.phoneNumber);
        etUpiId.setText(d.paymentUPI);

        int age = (d.age <= 0) ? 18 : d.age;
        etAge.setText(String.valueOf(age));

        etRole.setText(d.role != null ? d.role : "user");

        setSpinnerSelection(spinnerGender, d.gender);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        UniversalSpinnerAdapter adapter = (UniversalSpinnerAdapter) spinner.getAdapter();
        if (adapter == null) return;

        int pos = adapter.getPosition(value);
        if (pos >= 0) {
            spinner.setSelection(pos);
        }
    }

    // 🔥 CUSTOM GENDER SPINNER WITH NEON STYLING
    private void setupGenderSpinner() {
        List<UniversalSpinnerAdapter.SpinnerItem> genderItems = new ArrayList<>();
        genderItems.add(new UniversalSpinnerAdapter.SpinnerItem("male", "Male"));
        genderItems.add(new UniversalSpinnerAdapter.SpinnerItem("female", "Female"));
        genderItems.add(new UniversalSpinnerAdapter.SpinnerItem("other", "Other"));
        genderItems.add(new UniversalSpinnerAdapter.SpinnerItem("prefer not to say", "Prefer not to say"));

        genderAdapter = new UniversalSpinnerAdapter(this, genderItems);
        spinnerGender.setAdapter(genderAdapter);

        // 🔥 FORCE WHITE COLOR ON SPINNER TEXT
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (view != null && view instanceof TextView) {
                    ((TextView) view).setTextColor(0xFFFFFFFF);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

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

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> {
            if (!validateInputs()) return;
            updateProfile();
        });
    }

    // 🔥 UPDATE PROFILE API - ALSO UPDATE CACHE
    private void updateProfile() {
        LoaderOverlay.show(this);

        String name = etName.getText().toString().trim();
        String ign = etGameName.getText().toString().trim();

        UniversalSpinnerAdapter.SpinnerItem selectedGender = genderAdapter.getItem(spinnerGender.getSelectedItemPosition());
        String genderFixed = selectedGender != null ? selectedGender.apiValue : "male";

        int age = Integer.parseInt(etAge.getText().toString().trim());
        String phone = etPhone.getText().toString().trim();
        String upiClean = etUpiId.getText().toString().trim().replace("-", "").replace(" ", "");
        String paymentMethod = "UPI"; // 🔥 HARDCODED - NO MORE SPINNER

        UpdateProfileRequest req = new UpdateProfileRequest(
                name, ign, genderFixed, age, phone, upiClean, paymentMethod
        );

        api.updateProfile(req).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                LoaderOverlay.hide(EditProfileActivity.this);

                if (response.isSuccessful() && response.body() != null) {

                    // 🔥 UPDATE CACHE IMMEDIATELY (NO NEED TO RE-FETCH)
                    ProfileCacheManager.updateProfile(
                            EditProfileActivity.this,
                            name, ign, genderFixed, age, phone, upiClean, paymentMethod
                    );

                    showTopRightToast(response.body().getMessage());
                    finish();

                } else {
                    String serverMsg = null;
                    try {
                        serverMsg = response.errorBody().string();
                    } catch (Exception ignored) {}

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