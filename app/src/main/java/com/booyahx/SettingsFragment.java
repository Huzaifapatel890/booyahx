package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup click listeners
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            // TODO: Open Edit Profile Activity
            // startActivity(new Intent(requireContext(), EditProfileActivity.class));
        });

        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            // TODO: Open Change Password Activity
            // startActivity(new Intent(requireContext(), ChangePasswordActivity.class));
        });

        view.findViewById(R.id.btnWinningHistory).setOnClickListener(v -> {
            // TODO: Open Winning History Activity
            // startActivity(new Intent(requireContext(), WinningHistoryActivity.class));
        });

        view.findViewById(R.id.btnThemes).setOnClickListener(v -> {
            // TODO: Open Themes Activity
            // startActivity(new Intent(requireContext(), ThemeActivity.class));
        });

        view.findViewById(R.id.btnAboutUs).setOnClickListener(v -> {
            // TODO: Open About Us Activity
            // startActivity(new Intent(requireContext(), AboutActivity.class));
        });

        view.findViewById(R.id.btnSupport).setOnClickListener(v -> {
            // TODO: Open Support Activity
            // startActivity(new Intent(requireContext(), SupportActivity.class));
        });

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        // Clear token storage
        TokenManager.logout(requireContext());

        // Navigate to login
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}