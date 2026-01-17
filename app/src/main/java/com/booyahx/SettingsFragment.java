package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.booyahx.network.models.ProfileResponse;
import com.booyahx.settings.AboutActivity;
import com.booyahx.settings.ChangePasswordActivity;
import com.booyahx.settings.EditProfileActivity;
import com.booyahx.settings.HelpSupportActivity;
import com.booyahx.settings.WinningHistoryActivity;

public class SettingsFragment extends Fragment {

    private TextView txtUserName, txtEmail;

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

        view.findViewById(R.id.btnLogout)
                .setOnClickListener(v -> showLogoutDialog());

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

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (d, w) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        // 🔥 CLEAR ALL CACHES ON LOGOUT
        ProfileCacheManager.clear(requireContext());
        WalletCacheManager.clear(requireContext());
        TokenManager.logout(requireContext());

        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    private void showToast(String msg) {
        android.widget.Toast t =
                android.widget.Toast.makeText(requireContext(), msg,
                        android.widget.Toast.LENGTH_SHORT);
        t.setGravity(Gravity.TOP | Gravity.END, 40, 120);
        t.show();
    }
}