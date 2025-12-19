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

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

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

        // Initial load
        updateButtonStates(currentMode);
        loadProfile();
        loadWalletBalance();
        loadTournaments();

        return view;
    }

    /* ================= PROFILE ================= */

    private void loadProfile() {
        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(
                    Call<ProfileResponse> call,
                    Response<ProfileResponse> response
            ) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    txtUsername.setText(response.body().data.name);
                    txtEmail.setText(response.body().data.email);
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                // keep existing UI
            }
        });
    }

    private void loadWalletBalance() {
        api.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(
                    Call<WalletBalanceResponse> call,
                    Response<WalletBalanceResponse> response
            ) {
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
                // silent fail
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

        api.getTournaments("upcoming", currentMode)
                .enqueue(new Callback<TournamentResponse>() {
                    @Override
                    public void onResponse(
                            Call<TournamentResponse> call,
                            Response<TournamentResponse> response
                    ) {
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
                        // showTopRightToast("Failed to load tournaments");
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

        // Numbers only (labels already in XML)
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