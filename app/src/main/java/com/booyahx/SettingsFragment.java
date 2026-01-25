package com.booyahx;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.LogoutRequest;
import com.booyahx.network.models.LogoutResponse;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.settings.AboutActivity;
import com.booyahx.settings.ChangePasswordActivity;
import com.booyahx.settings.EditProfileActivity;
import com.booyahx.settings.HelpSupportActivity;
import com.booyahx.settings.WinningHistoryActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private TextView txtUserName, txtEmail;
    private Dialog logoutDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtUserName = view.findViewById(R.id.txtUserName);
        txtEmail = view.findViewById(R.id.txtEmail);

        // 🔥 LOAD FROM CACHE (NO API CALL)
        loadProfileFromCache();

        view.findViewById(R.id.btnEditProfile)
                .setOnClickListener(v -> startActivity(
                        new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.btnChangePassword)
                .setOnClickListener(v -> startActivity(
                        new Intent(requireContext(), ChangePasswordActivity.class)));

        view.findViewById(R.id.btnAboutUs)
                .setOnClickListener(v -> startActivity(
                        new Intent(requireContext(), AboutActivity.AboutUsActivity.class)));

        view.findViewById(R.id.btnSupport)
                .setOnClickListener(v -> startActivity(
                        new Intent(requireContext(), HelpSupportActivity.class)));

        view.findViewById(R.id.btnWinningHistory)
                .setOnClickListener(v -> startActivity(
                        new Intent(requireContext(), WinningHistoryActivity.class)));

        // 🔥 CUSTOM LOGOUT DIALOG
        view.findViewById(R.id.btnLogout)
                .setOnClickListener(v -> showCustomLogoutDialog());

        // 🔥 LISTEN FOR PROFILE UPDATES
        getParentFragmentManager().setFragmentResultListener(
                "profile_updated",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadProfileFromCache();
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // 🔥 REFRESH FROM CACHE WHEN RETURNING FROM EDIT PROFILE
        loadProfileFromCache();
    }

    // 🔥 LOAD FROM CACHE - NO API CALL
    private void loadProfileFromCache() {
        ProfileResponse.Data profile = ProfileCacheManager.getProfile(requireContext());

        if (profile != null) {
            txtUserName.setText(profile.name != null ? profile.name : "Unknown");
            txtEmail.setText(profile.email != null ? profile.email : "No Email");
        } else {
            // If no cache exists, show defaults
            txtUserName.setText("Unknown");
            txtEmail.setText("No Email");
        }
    }

    // 🔥 CUSTOM LOGOUT DIALOG WITH NEON BLUE STYLING
    private void showCustomLogoutDialog() {
        logoutDialog = new Dialog(requireContext());
        logoutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        logoutDialog.setContentView(R.layout.dialog_logout);

        // Make dialog background transparent to show custom background
        if (logoutDialog.getWindow() != null) {
            logoutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView btnCancel = logoutDialog.findViewById(R.id.btnCancel);
        TextView btnLogout = logoutDialog.findViewById(R.id.btnLogout);

        // Cancel button - just dismiss dialog
        btnCancel.setOnClickListener(v -> logoutDialog.dismiss());

        // Logout button - call API then logout
        btnLogout.setOnClickListener(v -> {
            logoutDialog.dismiss();
            callLogoutApi();
        });

        logoutDialog.show();
    }

    // 🔥 CALL LOGOUT API
    private void callLogoutApi() {
        String refreshToken = TokenManager.getRefreshToken(requireContext());

        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "No refresh token found, logging out locally");
            logoutLocally();
            return;
        }

        Log.d(TAG, "Calling logout API...");

        // Create request with refresh token
        LogoutRequest request = new LogoutRequest(refreshToken);

        // 🔥 Use ApiClient.getClient() to create ApiService
        ApiService apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        // Make API call
        Call<LogoutResponse> call = apiService.logout(request);
        call.enqueue(new Callback<LogoutResponse>() {
            @Override
            public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                if (!isAdded()) return; // Check if fragment is still attached

                if (response.isSuccessful() && response.body() != null) {
                    LogoutResponse logoutResponse = response.body();

                    if (logoutResponse.isSuccess()) {
                        Log.d(TAG, "Logout successful: " + logoutResponse.getMessage());
                        showToast("Logged out successfully");
                    } else {
                        Log.w(TAG, "Logout failed: " + logoutResponse.getMessage());
                        showToast(logoutResponse.getMessage() != null ?
                                logoutResponse.getMessage() : "Logout failed");
                    }
                } else {
                    Log.e(TAG, "Logout API error: " + response.code());
                    showToast("Logout error: " + response.code());
                }

                // Always logout locally regardless of API response
                logoutLocally();
            }

            @Override
            public void onFailure(Call<LogoutResponse> call, Throwable t) {
                if (!isAdded()) return; // Check if fragment is still attached

                Log.e(TAG, "Logout API network error: " + t.getMessage());
                showToast("Network error: " + t.getMessage());

                // Still logout locally even if network fails
                logoutLocally();
            }
        });
    }

    // 🔥 LOGOUT LOCALLY AND CLEAR ALL DATA
    private void logoutLocally() {
        Log.d(TAG, "Clearing local data and logging out...");

        // Clear all caches
        ProfileCacheManager.clear(requireContext());
        WalletCacheManager.clear(requireContext());
        TokenManager.logout(requireContext());

        // 🔥 Reset ApiClient instances
        ApiClient.reset();

        // Navigate to login screen
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Finish current activity
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void showToast(String msg) {
        android.widget.Toast t =
                android.widget.Toast.makeText(requireContext(), msg,
                        android.widget.Toast.LENGTH_SHORT);
        t.setGravity(Gravity.TOP | Gravity.END, 40, 120);
        t.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Dismiss dialog if still showing to prevent memory leaks
        if (logoutDialog != null && logoutDialog.isShowing()) {
            logoutDialog.dismiss();
        }
    }
}