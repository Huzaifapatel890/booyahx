package com.booyahx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.network.models.Tournament;
import com.booyahx.network.models.TournamentResponse;
import com.booyahx.network.models.WalletBalanceResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // Profile
    private TextView txtUsername, txtEmail, txtWalletBalance;

    // Tabs + tournaments
    private LinearLayout tournamentsContainer;
    private LinearLayout btnBermuda, btnClashSquad, btnSpecial;

    private String currentMode = "BR"; // BR / CS / LW
    private ApiService api;

    // 🔥 LOADER STATE (FIX)
    private int pendingCalls = 0;

    /* ================= LIFECYCLE ================= */

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // ❌ DO NOT CALL APIs HERE
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Profile views
        txtUsername = view.findViewById(R.id.txtUsername);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtWalletBalance = view.findViewById(R.id.txtWalletBalance);

        // Tournament views
        tournamentsContainer = view.findViewById(R.id.tournamentsContainer);
        btnBermuda = view.findViewById(R.id.btnBermuda);
        btnClashSquad = view.findViewById(R.id.btnClashSquad);
        btnSpecial = view.findViewById(R.id.btnSpecial);

        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        // Tabs
        btnBermuda.setOnClickListener(v -> switchMode("BR"));
        btnClashSquad.setOnClickListener(v -> switchMode("CS"));
        btnSpecial.setOnClickListener(v -> switchMode("LW"));

        updateButtonStates(currentMode);

        // 🔥 CRITICAL: wait until view is ATTACHED to window
        view.post(() -> {
            loadProfile();
            loadWalletBalance();
            loadTournaments();
        });
    }

    /* ================= LOADER CONTROL ================= */

    private void showLoader() {
        if (pendingCalls == 0 && isAdded()) {
            LoaderOverlay.show(requireActivity());
        }
        pendingCalls++;
    }

    private void hideLoader() {
        pendingCalls--;
        if (pendingCalls <= 0) {
            pendingCalls = 0;
            if (isAdded()) {
                LoaderOverlay.hide(requireActivity());
            }
        }
    }

    /* ================= PROFILE ================= */

    private void loadProfile() {
        showLoader();

        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(
                    Call<ProfileResponse> call,
                    Response<ProfileResponse> response
            ) {
                hideLoader();

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    txtUsername.setText(response.body().data.name);
                    txtEmail.setText(response.body().data.email);
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                hideLoader();
            }
        });
    }

    private void loadWalletBalance() {
        showLoader();

        api.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(
                    Call<WalletBalanceResponse> call,
                    Response<WalletBalanceResponse> response
            ) {
                hideLoader();

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    txtWalletBalance.setText(
                            String.format("%.2f GC", response.body().data.balanceGC)
                    );
                }
            }

            @Override
            public void onFailure(Call<WalletBalanceResponse> call, Throwable t) {
                hideLoader();
            }
        });
    }

    /* ================= GAME MODE ================= */

    private void switchMode(String mode) {
        currentMode = mode;
        updateButtonStates(mode);
        loadTournaments();
    }

    private void updateButtonStates(String mode) {
        btnBermuda.setAlpha(0.4f);
        btnClashSquad.setAlpha(0.4f);
        btnSpecial.setAlpha(0.4f);

        if ("BR".equals(mode)) btnBermuda.setAlpha(1f);
        if ("CS".equals(mode)) btnClashSquad.setAlpha(1f);
        if ("LW".equals(mode)) btnSpecial.setAlpha(1f);
    }

    /* ================= TOURNAMENTS ================= */

    private void loadTournaments() {
        tournamentsContainer.removeAllViews();
        showLoader();

        api.getTournaments("upcoming", currentMode)
                .enqueue(new Callback<TournamentResponse>() {
                    @Override
                    public void onResponse(
                            Call<TournamentResponse> call,
                            Response<TournamentResponse> response
                    ) {
                        hideLoader();

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().data != null
                                && response.body().data.tournaments != null) {

                            for (Tournament t : response.body().data.tournaments) {
                                tournamentsContainer.addView(createTournamentCard(t));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<TournamentResponse> call, Throwable t) {
                        hideLoader();
                    }
                });
    }

    private View createTournamentCard(Tournament t) {

        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_tournament_card, tournamentsContainer, false);

        TextView txtTitle = card.findViewById(R.id.txtT1Title);
        TextView txtExpectedPP = card.findViewById(R.id.txtExpectedPP);
        TextView txtCurrentPP = card.findViewById(R.id.txtCurrentPP);
        TextView txtSub = card.findViewById(R.id.txtT1Sub);
        TextView txtTime = card.findViewById(R.id.txtT1Time);
        TextView txtMode = card.findViewById(R.id.txtT1Mode);

        txtTitle.setText(t.getTitle());

        txtExpectedPP.setText(t.getExpectedPP() + " GC");
        txtCurrentPP.setText(
                "(" + t.getCurrentPP() + "/" + t.getExpectedPP() + ") GC"
        );

        txtSub.setText(
                "Entry GC " + t.getEntryFee()
                        + " • Slots "
                        + t.getUsedSlots()
                        + " / "
                        + t.getTotalSlots()
        );

        txtTime.setText(t.getFormattedDateTime());
        txtMode.setText("Mode: " + t.getDisplayMode());

        return card;
    }
}