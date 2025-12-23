package com.booyahx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
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
import com.booyahx.tournament.RulesBottomSheet;
import com.booyahx.tournament.JoinTournamentDialog;

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

    // 🔥 FRAGMENT LOADER
    private View fragmentLoader;
    private ImageView loaderRing, loaderGlow;
    private int pendingCalls = 0;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtUsername = view.findViewById(R.id.txtUsername);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtWalletBalance = view.findViewById(R.id.txtWalletBalance);

        tournamentsContainer = view.findViewById(R.id.tournamentsContainer);
        btnBermuda = view.findViewById(R.id.btnBermuda);
        btnClashSquad = view.findViewById(R.id.btnClashSquad);
        btnSpecial = view.findViewById(R.id.btnSpecial);

        fragmentLoader = view.findViewById(R.id.fragmentLoaderContainer);
        loaderRing = view.findViewById(R.id.fragmentLoaderRing);
        loaderGlow = view.findViewById(R.id.fragmentLoaderGlow);

        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        // ✅ ADDED — LISTEN FOR JOIN SUCCESS (NOTHING ELSE CHANGED)
        getParentFragmentManager().setFragmentResultListener(
                "join_success",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadWalletBalance();
                    }
                }
        );

        btnBermuda.setOnClickListener(v -> switchMode("BR"));
        btnClashSquad.setOnClickListener(v -> switchMode("CS"));
        btnSpecial.setOnClickListener(v -> switchMode("LW"));

        updateButtonStates(currentMode);

        view.post(() -> {
            loadProfile();
            loadWalletBalance();
            loadTournaments();
        });
    }

    /* ================= LOADER CONTROL ================= */

    private void showLoader() {
        if (pendingCalls == 0 && isAdded() && fragmentLoader != null) {
            fragmentLoader.setVisibility(View.VISIBLE);
            fragmentLoader.bringToFront();

            RotateAnimation rotate = new RotateAnimation(
                    0, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(900);
            rotate.setRepeatCount(Animation.INFINITE);
            rotate.setInterpolator(new LinearInterpolator());
            loaderRing.startAnimation(rotate);

            ScaleAnimation pulse = new ScaleAnimation(
                    1f, 1.25f,
                    1f, 1.25f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            pulse.setDuration(850);
            pulse.setRepeatCount(Animation.INFINITE);
            pulse.setRepeatMode(Animation.REVERSE);
            loaderGlow.startAnimation(pulse);
        }
        pendingCalls++;
    }

    private void hideLoader() {
        pendingCalls--;
        if (pendingCalls <= 0) {
            pendingCalls = 0;
            if (isAdded() && fragmentLoader != null) {
                fragmentLoader.setVisibility(View.GONE);
                loaderRing.clearAnimation();
                loaderGlow.clearAnimation();
            }
        }
    }

    /* ================= PROFILE ================= */

    private void loadProfile() {
        showLoader();
        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                hideLoader();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
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
            public void onResponse(Call<WalletBalanceResponse> call, Response<WalletBalanceResponse> response) {
                hideLoader();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    txtWalletBalance.setText(String.format("%.2f GC", response.body().data.balanceGC));
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
                    public void onResponse(Call<TournamentResponse> call, Response<TournamentResponse> response) {
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

        LinearLayout mapRotationContainer = card.findViewById(R.id.mapRotationContainer);
        TextView txtMapRotation = card.findViewById(R.id.txtMapRotation);

        View btnRules = card.findViewById(R.id.btnT1Rules);
        View btnJoin = card.findViewById(R.id.btnT1Join);

        txtTitle.setText(t.getTitle());
        txtExpectedPP.setText(t.getExpectedPP() + " GC");
        txtCurrentPP.setText("(" + t.getCurrentPP() + "/" + t.getExpectedPP() + ") GC");

        txtSub.setText("Entry GC " + t.getEntryFee()
                + " • Slots " + t.getUsedSlots() + " / " + t.getTotalSlots());

        txtTime.setText(t.getFormattedDateTime());
        txtMode.setText("Mode: " + t.getDisplayMode());

        String mapRotation = t.getMapRotationShort();
        if (mapRotation != null && !mapRotation.isEmpty()) {
            mapRotationContainer.setVisibility(View.VISIBLE);
            txtMapRotation.setText(mapRotation);
        } else {
            mapRotationContainer.setVisibility(View.GONE);
        }

        if (btnRules != null) {
            btnRules.setOnClickListener(v -> {
                RulesBottomSheet sheet = RulesBottomSheet.newInstance(t);
                sheet.show(getParentFragmentManager(), "RulesBottomSheet");
            });
        }

        if (btnJoin != null) {
            btnJoin.setOnClickListener(v -> {
                JoinTournamentDialog dialog = JoinTournamentDialog.newInstance(t);
                dialog.show(getParentFragmentManager(), "JoinTournamentDialog");
            });
        }

        return card;
    }
}