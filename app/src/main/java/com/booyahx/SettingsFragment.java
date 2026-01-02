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

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
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

        fetchUserProfile();

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

    }

    private void fetchUserProfile() {
        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);

        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call,
                                   Response<ProfileResponse> response) {

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().success) {

                    ProfileResponse.Data data = response.body().data;
                    txtUserName.setText(data.name != null ? data.name : "Unknown");
                    txtEmail.setText(data.email != null ? data.email : "No Email");
                } else {
                    showToast("Failed to load profile");
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                showToast("Network error");
            }
        });
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