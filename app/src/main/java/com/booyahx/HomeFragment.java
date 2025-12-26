package com.booyahx;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.booyahx.adapters.TournamentStatusAdapter;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.network.models.Tournament;
import com.booyahx.network.models.TournamentResponse;
import com.booyahx.network.models.WalletBalanceResponse;
import com.booyahx.tournament.RulesBottomSheet;
import com.booyahx.tournament.JoinTournamentDialog;
import com.booyahx.utils.NotificationPref;
import com.booyahx.notifications.NotificationActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // Profile
    private TextView txtUsername, txtWalletBalance;
    private ImageView btnNotification;

    // Tabs + tournaments
    private LinearLayout tournamentsContainer;
    private LinearLayout btnBermuda, btnClashSquad, btnSpecial;

    // Tournament Status Spinner
    private Spinner spinnerTournamentStatus;
    private TournamentStatusAdapter statusAdapter;

    private String currentMode = "BR";
    private String currentStatus = "upcoming";
    private ApiService api;

    // Loader
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
        txtWalletBalance = view.findViewById(R.id.txtWalletBalance);
        btnNotification = view.findViewById(R.id.btnNotification);

        tournamentsContainer = view.findViewById(R.id.tournamentsContainer);
        btnBermuda = view.findViewById(R.id.btnBermuda);
        btnClashSquad = view.findViewById(R.id.btnClashSquad);
        btnSpecial = view.findViewById(R.id.btnSpecial);

        spinnerTournamentStatus = view.findViewById(R.id.spinnerTournamentStatus);

        fragmentLoader = view.findViewById(R.id.fragmentLoaderContainer);
        loaderRing = view.findViewById(R.id.fragmentLoaderRing);
        loaderGlow = view.findViewById(R.id.fragmentLoaderGlow);

        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        setupStatusSpinner();
        updateNotificationIcon();

        btnNotification.setOnClickListener(v -> {
            NotificationPref.setUnread(requireContext(), false);
            updateNotificationIcon();

            Intent intent = new Intent(requireContext(), NotificationActivity.class);
            startActivity(intent);
        });

        getParentFragmentManager().setFragmentResultListener(
                "joined_refresh",
                this,
                (key, bundle) -> {
                    NotificationPref.setUnread(requireContext(), true);
                    updateNotificationIcon();
                }
        );

        getParentFragmentManager().setFragmentResultListener(
                "join_success",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadWalletBalance();
                        String tournamentId = bundle.getString("tournament_id");
                        if (tournamentId != null) {
                            markTournamentAsJoined(tournamentId);
                        }
                    }
                }
        );

        getParentFragmentManager().setFragmentResultListener(
                "tournament_status_changed",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        String tournamentId = bundle.getString("tournament_id");
                        String newStatus = bundle.getString("new_status");

                        android.util.Log.d("HomeFragment", "Status changed for tournament: " + tournamentId + " -> " + newStatus);

                        loadTournaments();
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
        });
    }

    private void setupStatusSpinner() {
        List<TournamentStatusAdapter.StatusItem> statusItems = new ArrayList<>();
        statusItems.add(new TournamentStatusAdapter.StatusItem("upcoming", "Upcoming Tournaments"));
        statusItems.add(new TournamentStatusAdapter.StatusItem("live", "Live Tournaments"));
        statusItems.add(new TournamentStatusAdapter.StatusItem("completed", "Completed Tournaments"));
        statusItems.add(new TournamentStatusAdapter.StatusItem("pendingResult", "Pending Result Tournaments"));

        statusAdapter = new TournamentStatusAdapter(requireContext(), statusItems);
        spinnerTournamentStatus.setAdapter(statusAdapter);

        spinnerTournamentStatus.setSelection(0);

        spinnerTournamentStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TournamentStatusAdapter.StatusItem selected = statusAdapter.getItem(position);
                if (selected != null && !selected.apiValue.equals(currentStatus)) {
                    currentStatus = selected.apiValue;
                    loadTournaments();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateNotificationIcon() {
        if (NotificationPref.hasUnread(requireContext())) {
            btnNotification.setImageResource(R.drawable.ic_notification_red);
        } else {
            btnNotification.setImageResource(R.drawable.ic_notification);
        }
    }

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

    private void loadProfile() {
        showLoader();
        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                hideLoader();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {

                    ProfileResponse.Data data = response.body().data;

                    txtUsername.setText(data.name);

                    if (data.userId != null && !data.userId.isEmpty()) {
                        TokenManager.saveUserId(requireContext(), data.userId);
                    }

                    loadTournaments();
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

    private void loadTournaments() {
        tournamentsContainer.removeAllViews();
        showLoader();

        api.getTournaments(currentStatus, currentMode)
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

    private void markTournamentAsJoined(String tournamentId) {
        for (int i = 0; i < tournamentsContainer.getChildCount(); i++) {
            View card = tournamentsContainer.getChildAt(i);
            View btnJoin = card.findViewById(R.id.btnT1Join);
            if (btnJoin != null && btnJoin.getTag() != null && btnJoin.getTag().equals(tournamentId)) {
                btnJoin.setEnabled(false);
                btnJoin.setAlpha(0.5f);
                ((TextView) btnJoin).setText("Joined");
                break;
            }
        }
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
            btnJoin.setTag(t.getId());

            btnJoin.setOnClickListener(v -> {
                JoinTournamentDialog dialog = JoinTournamentDialog.newInstance(t);
                dialog.show(getParentFragmentManager(), "JoinTournamentDialog");
            });

            // ================= DERIVED JOIN STATE =================
            String userId = TokenManager.getUserId(requireContext());

            if (!"upcoming".equalsIgnoreCase(t.getStatus())) {
                btnJoin.setVisibility(View.GONE);
            } else if (t.isJoinedDerived(userId)) {
                btnJoin.setEnabled(false);
                btnJoin.setAlpha(0.5f);
                ((TextView) btnJoin).setText("Joined");
            }
        }

        return card;
    }
}
