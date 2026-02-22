package com.booyahx;

import com.booyahx.utils.TopRightToast;
import com.booyahx.utils.TournamentJoinStateManager;
import com.booyahx.settings.EditProfileActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.booyahx.adapters.TournamentStatusAdapter;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.network.models.Tournament;
import com.booyahx.network.models.TournamentResponse;
import com.booyahx.network.models.WalletBalanceResponse;
import com.booyahx.network.models.HostApplyRequest;
import com.booyahx.network.models.HostApplyResponse;
import com.booyahx.network.models.HostTournamentsListResponse;
import com.booyahx.tournament.RulesBottomSheet;
import com.booyahx.tournament.JoinTournamentDialog;
import com.booyahx.utils.NotificationPref;
import com.booyahx.notifications.NotificationActivity;
import com.booyahx.utils.AvatarGenerator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment_DEBUG";

    private TextView txtUsername, txtWalletBalance;
    private TextView txtHostBadge;
    private ImageView btnNotification;
    private ImageView ivProfilePic;

    private LinearLayout tournamentsContainer;
    private LinearLayout btnBermuda, btnClashSquad, btnSpecial;

    // ✅ SubMode spinner (Solo / Duo / Squad) — visible only for Bermuda (BR).
    private Spinner spinnerSubMode;
    private TournamentStatusAdapter subModeAdapter;

    private String currentMode = "BR";
    // currentStatus fixed to "upcoming" — status filter removed from UI
    private String currentStatus = "upcoming";
    // Active subMode filter; null = show all submodes
    private String currentSubMode = null;
    private ApiService api;

    private boolean tournamentsLoaded = false;

    // ─────────────────────────────────────────────────────────────────────────
    // SOCKET BROADCAST RECEIVER
    //
    // Behavior per event:
    //   • TOURNAMENT_STATUS_UPDATED → full reload WITH loader (removeAllViews +
    //     regular API client so GlobalLoadingInterceptor fires).
    //     This is intentional: a status change (upcoming→live etc.) means the
    //     card set itself may change, so we want a clean redraw with visual feedback.
    //
    //   • WALLET_UPDATED → refresh wallet balance only.
    //
    //   • TOURNAMENT_ROOM_UPDATED / PAYMENT_QR_UPDATED → notification bell only;
    //     no card reload (room-level events must NOT wipe tournament cards).
    // ─────────────────────────────────────────────────────────────────────────
    private final BroadcastReceiver socketEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "📨 Socket broadcast received in HomeFragment: " + action);

            if (!isAdded()) return;

            // Mark notification as unread and update bell icon for any socket event
            NotificationPref.setUnread(requireContext(), true);
            updateNotificationIcon();

            // ✅ FIX: TOURNAMENT_STATUS_UPDATED → full reload WITH loader.
            // Previously this called silentRefreshTournaments() which skipped the loader
            // and did an in-place patch. A status change warrants a full, visible reload
            // so the user sees updated cards with correct state immediately.
            //
            // TOURNAMENT_ROOM_UPDATED → bell only (already updated above).
            // No card reload — room credentials are pushed directly to ParticipatedFragment
            // via the "tournament_room_updated" fragment result in DashboardActivity.
            if ("TOURNAMENT_STATUS_UPDATED".equals(action)) {
                Log.d(TAG, action + " received — doing full reload with loader");
                tournamentsLoaded = false;
                loadTournaments();
            }

            // ✅ FIX: NOTIFICATION_PUSH — admin created a new tournament.
            // Bell already updated above. Reload list so new card appears immediately.
            if ("NOTIFICATION_PUSH".equals(action)) {
                tournamentsLoaded = false;
                loadTournaments();
            }

            // Wallet balance refresh
            if ("WALLET_UPDATED".equals(action)) {
                loadWalletBalanceFromAPI();
            }
        }
    };

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
        txtHostBadge = view.findViewById(R.id.txtHostBadge);
        txtWalletBalance = view.findViewById(R.id.txtWalletBalance);
        btnNotification = view.findViewById(R.id.btnNotification);
        ivProfilePic = view.findViewById(R.id.imgAvatar);

        tournamentsContainer = view.findViewById(R.id.tournamentsContainer);
        btnBermuda = view.findViewById(R.id.btnBermuda);
        btnClashSquad = view.findViewById(R.id.btnClashSquad);
        btnSpecial = view.findViewById(R.id.btnSpecial);

        spinnerSubMode = view.findViewById(R.id.spinnerTournamentStatus);

        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        setupSubModeSpinner();
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

        // ─────────────────────────────────────────────────────────────────────
        // join_success → SILENT in-place update only.
        //
        // After the user joins a tournament:
        //   1. Refresh wallet balance (entry fee was deducted).
        //   2. Locally mark the tournament as joined (TournamentJoinStateManager).
        //   3. Immediately update the join button on the existing card (markTournamentAsJoined).
        //   4. Silently re-fetch the tournament list to sync slot counts / PP values
        //      WITHOUT removing or recreating any card views (silentRefreshTournaments).
        //
        // ✅ silentRefreshTournaments uses ApiClient.getSilentClient() so the
        //    GlobalLoadingInterceptor is bypassed — no loader flicker, no removeAllViews().
        // ─────────────────────────────────────────────────────────────────────
        getParentFragmentManager().setFragmentResultListener(
                "join_success",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        // ✅ Silent wallet refresh — no loader shown after successful join
                        silentRefreshWalletBalance();

                        String tournamentId = bundle.getString("tournament_id");
                        String userId = ProfileCacheManager.getUserId(requireContext());

                        if (tournamentId != null && userId != null) {
                            TournamentJoinStateManager.markAsJoined(
                                    requireContext(),
                                    userId,
                                    tournamentId
                            );
                            // Immediately grey out the join button on the existing card
                            markTournamentAsJoined(tournamentId);
                        }

                        // ✅ Silent API re-fetch: updates slot counts, PP values, etc. in-place.
                        // Only patches existing card fields — does NOT recreate or add cards.
                        silentRefreshTournamentsFieldsOnly();
                    }
                }
        );

        // ─────────────────────────────────────────────────────────────────────
        // tournament_status_changed → notification bell update ONLY.
        //
        // loadTournaments() is intentionally NOT called here — the
        // socketEventReceiver already handles it for TOURNAMENT_STATUS_UPDATED.
        // This listener exists purely as a reliable fallback so the notification
        // bell is always refreshed even if the socket broadcast arrived while
        // HomeFragment was briefly stopped (e.g. during dialog transitions when
        // isAdded() returns false and the broadcast is dropped early).
        // ─────────────────────────────────────────────────────────────────────
        getParentFragmentManager().setFragmentResultListener(
                "tournament_status_changed",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        NotificationPref.setUnread(requireContext(), true);
                        updateNotificationIcon();
                    }
                }
        );

        getParentFragmentManager().setFragmentResultListener(
                "balance_updated",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadWalletBalanceFromCache();
                    }
                }
        );

        btnBermuda.setOnClickListener(v -> switchMode("BR"));
        btnClashSquad.setOnClickListener(v -> switchMode("CS"));
        btnSpecial.setOnClickListener(v -> switchMode("LW"));

        updateButtonStates(currentMode);

        // ✅ First app start: loadTournaments() uses ApiClient.getClient() (regular client)
        // so GlobalLoadingInterceptor shows the loader automatically.
        view.post(() -> {
            loadProfileData();
            loadWalletData();
        });
    }

    // ✅ Register BroadcastReceiver when fragment is visible
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("WALLET_UPDATED");
        filter.addAction("TOURNAMENT_ROOM_UPDATED");
        filter.addAction("TOURNAMENT_STATUS_UPDATED");
        filter.addAction("PAYMENT_QR_UPDATED");
        filter.addAction("NOTIFICATION_PUSH");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(socketEventReceiver, filter);
        Log.d(TAG, "✅ Socket BroadcastReceiver registered");
    }

    // ✅ Unregister BroadcastReceiver when fragment is not visible
    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(socketEventReceiver);
        Log.d(TAG, "✅ Socket BroadcastReceiver unregistered");
    }

    private void setupSubModeSpinner() {
        List<TournamentStatusAdapter.StatusItem> subModeItems = new ArrayList<>();
        subModeItems.add(new TournamentStatusAdapter.StatusItem(null,    "All Modes"));
        subModeItems.add(new TournamentStatusAdapter.StatusItem("solo",  "Solo"));
        subModeItems.add(new TournamentStatusAdapter.StatusItem("duo",   "Duo"));
        subModeItems.add(new TournamentStatusAdapter.StatusItem("squad", "Squad"));

        subModeAdapter = new TournamentStatusAdapter(requireContext(), subModeItems);
        spinnerSubMode.setAdapter(subModeAdapter);
        spinnerSubMode.setSelection(0);

        // Only visible for Bermuda mode
        spinnerSubMode.setVisibility("BR".equals(currentMode) ? View.VISIBLE : View.GONE);

        spinnerSubMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TournamentStatusAdapter.StatusItem selected = subModeAdapter.getItem(position);
                if (selected != null) {
                    String newSubMode = selected.apiValue;
                    boolean changed = (newSubMode == null)
                            ? currentSubMode != null
                            : !newSubMode.equals(currentSubMode);
                    if (changed) {
                        currentSubMode = newSubMode;
                        tournamentsLoaded = false;
                        loadTournaments();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateNotificationIcon() {
        if (NotificationPref.hasUnread(requireContext())) {
            btnNotification.setImageResource(R.drawable.ic_notification_red);
        } else {
            btnNotification.setImageResource(R.drawable.ic_notification);
        }
    }

    private void loadProfileData() {
        if (ProfileCacheManager.hasProfile(requireContext())) {
            loadProfileFromCache();
            checkAccountSwitch();
            loadTournaments();
        } else {
            loadProfileFromAPI();
        }
    }

    private void loadProfileFromCache() {
        ProfileResponse.Data profile = ProfileCacheManager.getProfile(requireContext());
        if (profile != null) {
            updateProfileUI(profile);
        }
    }

    private void loadProfileFromAPI() {
        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    ProfileResponse.Data data = response.body().data;

                    ProfileCacheManager.saveProfile(requireContext(), data);

                    if (data.userId != null) {
                        TokenManager.saveUserId(requireContext(), data.userId);
                        checkAccountSwitch();
                    }
                    if (data.role != null) {
                        TokenManager.saveRole(requireContext(), data.role);
                    }

                    updateProfileUI(data);

                    Bundle b = new Bundle();
                    getParentFragmentManager().setFragmentResult("role_updated", b);

                    loadTournaments();
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
            }
        });
    }

    private void checkAccountSwitch() {
        String currentUserId = ProfileCacheManager.getUserId(requireContext());

        if (currentUserId != null) {
            if (TournamentJoinStateManager.hasUserSwitched(requireContext(), currentUserId)) {
                TournamentJoinStateManager.setCurrentUser(requireContext(), currentUserId);
                tournamentsLoaded = false;
            } else {
                TournamentJoinStateManager.setCurrentUser(requireContext(), currentUserId);
            }
        }
    }

    private void updateProfileUI(ProfileResponse.Data data) {
        txtUsername.setText(data.name != null ? data.name : "User");

        try {
            Bitmap avatarBitmap = AvatarGenerator.generateAvatar(
                    data.name != null ? data.name : "U", 48, requireContext());
            ivProfilePic.setImageBitmap(avatarBitmap);
        } catch (Exception e) {
            // Keep default if fails
        }

        String role = data.role != null ? data.role : "user";
        if ("host".equalsIgnoreCase(role)) {
            txtHostBadge.setVisibility(View.VISIBLE);
        } else {
            txtHostBadge.setVisibility(View.GONE);
        }
    }

    private void loadWalletData() {
        if (WalletCacheManager.hasBalance(requireContext())) {
            loadWalletBalanceFromCache();
        } else {
            loadWalletBalanceFromAPI();
        }
    }

    private void loadWalletBalanceFromCache() {
        double balance = WalletCacheManager.getBalance(requireContext());
        txtWalletBalance.setText(String.format("%.2f GC", balance));
    }

    private void loadWalletBalanceFromAPI() {
        ApiClient.getSilentClient(requireContext()).create(ApiService.class).getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(Call<WalletBalanceResponse> call, Response<WalletBalanceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    double balance = response.body().data.balanceGC;
                    WalletCacheManager.saveBalance(requireContext(), balance);
                    txtWalletBalance.setText(String.format("%.2f GC", balance));
                }
            }

            @Override
            public void onFailure(Call<WalletBalanceResponse> call, Throwable t) {
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SILENT WALLET REFRESH (join_success only)
    //
    // Uses ApiClient.getSilentClient() → GlobalLoadingInterceptor is bypassed.
    // Updates both WalletCacheManager and the balance TextView in-place.
    // No loader is shown — called after a successful tournament join so the
    // deducted entry fee is reflected without any visual loading flicker.
    // ─────────────────────────────────────────────────────────────────────────
    private void silentRefreshWalletBalance() {
        if (!isAdded()) return;
        ApiService silentApi = ApiClient.getSilentClient(requireContext()).create(ApiService.class);
        silentApi.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(Call<WalletBalanceResponse> call, Response<WalletBalanceResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {
                    double balance = response.body().data.balanceGC;
                    WalletCacheManager.saveBalance(requireContext(), balance);
                    txtWalletBalance.setText(String.format("%.2f GC", balance));
                }
            }

            @Override
            public void onFailure(Call<WalletBalanceResponse> call, Throwable t) {
            }
        });
    }

    private void switchMode(String mode) {
        currentMode = mode;
        updateButtonStates(mode);
        if ("BR".equals(mode)) {
            spinnerSubMode.setVisibility(View.VISIBLE);
        } else {
            spinnerSubMode.setVisibility(View.GONE);
            currentSubMode = null;
        }
        tournamentsLoaded = false;
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

    // ─────────────────────────────────────────────────────────────────────────
    // FULL LOAD — with loader (GlobalLoadingInterceptor fires automatically
    // because we use ApiClient.getClient(), not getSilentClient()).
    // Called on: first app start, mode/submode switch, TOURNAMENT_STATUS_UPDATED.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadTournaments() {
        if (tournamentsLoaded) {
            return;
        }

        // Special / LW mode — no API, show Coming Soon immediately.
        if ("LW".equals(currentMode)) {
            showComingSoon();
            tournamentsLoaded = true;
            return;
        }

        tournamentsContainer.removeAllViews();

        String role = ProfileCacheManager.getRole(requireContext());

        if ("host".equalsIgnoreCase(role)) {
            loadHostTournaments();
        } else {
            loadUserTournaments();
        }
    }

    private void loadUserTournaments() {
        api.getTournaments(currentStatus, currentMode)
                .enqueue(new Callback<TournamentResponse>() {
                    @Override
                    public void onResponse(Call<TournamentResponse> call, Response<TournamentResponse> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().data != null
                                && response.body().data.tournaments != null) {

                            List<Tournament> uniqueTournaments = removeDuplicateTournaments(
                                    response.body().data.tournaments
                            );

                            uniqueTournaments = filterBySubMode(uniqueTournaments);
                            sortTournamentsByTime(uniqueTournaments);
                            renderTournaments(uniqueTournaments);
                            tournamentsLoaded = true;
                        }
                    }

                    @Override
                    public void onFailure(Call<TournamentResponse> call, Throwable t) {
                    }
                });
    }

    private void loadHostTournaments() {
        api.getHostTournaments(currentStatus, currentMode)
                .enqueue(new Callback<HostTournamentsListResponse>() {
                    @Override
                    public void onResponse(Call<HostTournamentsListResponse> call, Response<HostTournamentsListResponse> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().data != null
                                && response.body().data.tournaments != null) {

                            List<Tournament> uniqueTournaments = removeDuplicateTournaments(
                                    response.body().data.tournaments
                            );

                            uniqueTournaments = filterBySubMode(uniqueTournaments);
                            sortTournamentsByTime(uniqueTournaments);
                            renderTournaments(uniqueTournaments);
                            tournamentsLoaded = true;
                        }
                    }

                    @Override
                    public void onFailure(Call<HostTournamentsListResponse> call, Throwable t) {
                    }
                });
    }

    private List<Tournament> removeDuplicateTournaments(List<Tournament> tournaments) {
        if (tournaments == null || tournaments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Tournament> uniqueMap = new LinkedHashMap<>();

        for (Tournament tournament : tournaments) {
            String tournamentId = tournament.getId();

            if (tournamentId != null && !tournamentId.trim().isEmpty()) {
                if (!uniqueMap.containsKey(tournamentId)) {
                    uniqueMap.put(tournamentId, tournament);
                }
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    // Client-side subMode filter — only applied when a specific subMode is selected.
    private List<Tournament> filterBySubMode(List<Tournament> tournaments) {
        if (currentSubMode == null || currentSubMode.isEmpty()) {
            return tournaments;
        }
        List<Tournament> filtered = new ArrayList<>();
        for (Tournament t : tournaments) {
            if (currentSubMode.equalsIgnoreCase(t.getSubMode())) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    // ========== TOURNAMENT SORTING ==========
    private void sortTournamentsByTime(List<Tournament> tournaments) {
        if (tournaments == null || tournaments.isEmpty()) {
            return;
        }

        boolean reverseOrder = "completed".equalsIgnoreCase(currentStatus)
                || "cancelled".equalsIgnoreCase(currentStatus)
                || "pendingResult".equalsIgnoreCase(currentStatus);

        Collections.sort(tournaments, new Comparator<Tournament>() {
            @Override
            public int compare(Tournament t1, Tournament t2) {
                Date date1 = parseTournamentDateTime(t1);
                Date date2 = parseTournamentDateTime(t2);

                if (date1 == null && date2 == null) return 0;
                if (date1 == null) return 1;
                if (date2 == null) return -1;

                if (reverseOrder) {
                    return date2.compareTo(date1);
                } else {
                    return date1.compareTo(date2);
                }
            }
        });
    }

    private Date parseTournamentDateTime(Tournament tournament) {
        try {
            String dateStr = tournament.getDate();
            String timeStr = tournament.getStartTime();

            if (dateStr == null || timeStr == null) {
                return null;
            }

            if (dateStr.contains("T")) {
                dateStr = dateStr.substring(0, dateStr.indexOf("T"));
            }

            String dateTimeStr = dateStr + " " + timeStr;

            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
                    new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US),
                    new SimpleDateFormat("dd-MM-yyyy h:mm a", Locale.US),
                    new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US),
                    new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US),
                    new SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US),
                    new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
            };

            for (SimpleDateFormat format : formats) {
                try {
                    format.setLenient(false);
                    Date parsed = format.parse(dateTimeStr);
                    if (parsed != null) {
                        return parsed;
                    }
                } catch (ParseException e) {
                    // Try next format
                }
            }

            Log.w(TAG, "Could not parse date/time: " + dateTimeStr);
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing tournament date/time", e);
            return null;
        }
    }
    // ========== END TOURNAMENT SORTING ==========

    private void renderTournaments(List<Tournament> tournaments) {
        if (!isAdded() || tournamentsContainer == null) {
            return;
        }

        tournamentsContainer.removeAllViews();

        for (Tournament tournament : tournaments) {
            View cardView = createTournamentCard(tournament);
            if (cardView != null) {
                tournamentsContainer.addView(cardView);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SILENT REFRESH (join_success only)
    //
    // Uses ApiClient.getSilentClient() → GlobalLoadingInterceptor is bypassed.
    // After fetching fresh data, ONLY patches existing card fields in-place.
    // Does NOT add new cards (silentUpdateCards is NOT called here — we use
    // silentUpdateExistingCardsOnly instead to guarantee no card is ever created
    // from this path).
    // ─────────────────────────────────────────────────────────────────────────
    private void silentRefreshTournamentsFieldsOnly() {
        if (!isAdded()) return;

        // Guard: if initial load is still in-flight, skip — it will already bring fresh data.
        if (!tournamentsLoaded) {
            Log.d(TAG, "silentRefreshTournamentsFieldsOnly: skipped — initial load still in flight");
            return;
        }

        if ("LW".equals(currentMode)) {
            return;
        }

        String role = ProfileCacheManager.getRole(requireContext());
        ApiService silentApi = ApiClient.getSilentClient(requireContext()).create(ApiService.class);

        if ("host".equalsIgnoreCase(role)) {
            silentApi.getHostTournaments(currentStatus, currentMode)
                    .enqueue(new Callback<HostTournamentsListResponse>() {
                        @Override
                        public void onResponse(Call<HostTournamentsListResponse> call, Response<HostTournamentsListResponse> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().data != null
                                    && response.body().data.tournaments != null) {

                                List<Tournament> fresh = removeDuplicateTournaments(
                                        response.body().data.tournaments);
                                fresh = filterBySubMode(fresh);
                                sortTournamentsByTime(fresh);
                                // ✅ Only patch fields — never create new cards
                                silentUpdateExistingCardsOnly(fresh);
                            }
                        }

                        @Override
                        public void onFailure(Call<HostTournamentsListResponse> call, Throwable t) {
                            Log.w(TAG, "silentRefreshTournamentsFieldsOnly (host) failed: " + t.getMessage());
                        }
                    });
        } else {
            silentApi.getTournaments(currentStatus, currentMode)
                    .enqueue(new Callback<TournamentResponse>() {
                        @Override
                        public void onResponse(Call<TournamentResponse> call, Response<TournamentResponse> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().data != null
                                    && response.body().data.tournaments != null) {

                                List<Tournament> fresh = removeDuplicateTournaments(
                                        response.body().data.tournaments);
                                fresh = filterBySubMode(fresh);
                                sortTournamentsByTime(fresh);
                                // ✅ Only patch fields — never create new cards
                                silentUpdateExistingCardsOnly(fresh);
                            }
                        }

                        @Override
                        public void onFailure(Call<TournamentResponse> call, Throwable t) {
                            Log.w(TAG, "silentRefreshTournamentsFieldsOnly (user) failed: " + t.getMessage());
                        }
                    });
        }
    }

    /**
     * Patches ONLY the existing card views already in tournamentsContainer.
     * Called exclusively from the join_success path.
     *
     * Key difference from silentUpdateCards():
     *   • Does NOT add cards for tournaments that have no existing view.
     *   • Does NOT remove cards that are absent from the fresh list.
     *   → The card set is frozen; only text/button fields are refreshed.
     *
     * This guarantees joining a tournament never recreates or reshuffles cards.
     */
    private void silentUpdateExistingCardsOnly(List<Tournament> freshTournaments) {
        if (!isAdded() || tournamentsContainer == null) return;

        // Build lookup map for O(1) access
        Map<String, Tournament> freshMap = new LinkedHashMap<>();
        for (Tournament t : freshTournaments) {
            if (t != null && t.getId() != null) {
                freshMap.put(t.getId(), t);
            }
        }

        // Walk existing cards and patch only those present in the fresh response
        for (int i = 0; i < tournamentsContainer.getChildCount(); i++) {
            View card = tournamentsContainer.getChildAt(i);
            Object tag = card.getTag();
            if (tag instanceof String) {
                String cardId = (String) tag;
                Tournament freshTournament = freshMap.get(cardId);
                if (freshTournament != null) {
                    updateCardFields(card, freshTournament);
                }
                // If not in freshMap, leave the card untouched — do NOT remove it.
                // A full loadTournaments() (e.g. from TOURNAMENT_STATUS_UPDATED) will
                // clean up stale cards with a proper reload.
            }
        }

        Log.d(TAG, "silentUpdateExistingCardsOnly: patched " + tournamentsContainer.getChildCount() + " existing cards");
    }

    /**
     * Full in-place card update — adds/removes cards as needed.
     * Used by the broader silentRefreshTournaments() path if ever needed.
     * (Currently only silentUpdateExistingCardsOnly is used.)
     */
    private void silentUpdateCards(List<Tournament> freshTournaments) {
        if (!isAdded() || tournamentsContainer == null) return;

        Map<String, Tournament> freshMap = new LinkedHashMap<>();
        for (Tournament t : freshTournaments) {
            if (t != null && t.getId() != null) {
                freshMap.put(t.getId(), t);
            }
        }

        Map<String, View> existingCards = new LinkedHashMap<>();
        for (int i = 0; i < tournamentsContainer.getChildCount(); i++) {
            View card = tournamentsContainer.getChildAt(i);
            Object tag = card.getTag();
            if (tag instanceof String) {
                existingCards.put((String) tag, card);
            }
        }

        // Update or remove existing cards
        for (Map.Entry<String, View> entry : existingCards.entrySet()) {
            String cardId = entry.getKey();
            View card = entry.getValue();
            if (freshMap.containsKey(cardId)) {
                updateCardFields(card, freshMap.get(cardId));
            } else {
                tournamentsContainer.removeView(card);
                Log.d(TAG, "silentUpdateCards: removed card for tournament " + cardId);
            }
        }

        // Append cards for genuinely new tournaments
        for (Tournament t : freshTournaments) {
            if (t == null || t.getId() == null) continue;
            if (!existingCards.containsKey(t.getId())) {
                View newCard = createTournamentCard(t);
                if (newCard != null) {
                    tournamentsContainer.addView(newCard);
                    Log.d(TAG, "silentUpdateCards: added new card for tournament " + t.getId());
                }
            }
        }
    }

    /**
     * Updates only the mutable display fields of an already-inflated card view.
     * Does NOT re-inflate or alter click listeners already set by createTournamentCard().
     */
    private void updateCardFields(View card, Tournament t) {
        if (card == null || t == null) return;

        TextView txtTitle       = card.findViewById(R.id.txtT1Title);
        TextView txtExpectedPP  = card.findViewById(R.id.txtExpectedPP);
        TextView txtCurrentPP   = card.findViewById(R.id.txtCurrentPP);
        TextView txtSub         = card.findViewById(R.id.txtT1Sub);
        TextView txtTime        = card.findViewById(R.id.txtT1Time);
        TextView txtMode        = card.findViewById(R.id.txtT1Mode);
        LinearLayout mapRotationContainer = card.findViewById(R.id.mapRotationContainer);
        TextView txtMapRotation = card.findViewById(R.id.txtMapRotation);
        View btnJoin            = card.findViewById(R.id.btnT1Join);

        if (txtTitle != null)      txtTitle.setText(t.getTitle());
        if (txtExpectedPP != null) txtExpectedPP.setText(t.getExpectedPP() + " GC");
        if (txtCurrentPP != null)  txtCurrentPP.setText("(" + t.getCurrentPP() + "/" + t.getExpectedPP() + ") GC");
        if (txtSub != null) {
            txtSub.setText("Entry GC " + t.getEntryFee()
                    + " • Slots " + t.getUsedSlots() + " / " + t.getTotalSlots());
        }
        if (txtTime != null) txtTime.setText(t.getFormattedDateTime());
        if (txtMode != null) txtMode.setText("Mode: " + t.getDisplayMode());

        if (mapRotationContainer != null && txtMapRotation != null) {
            String mapRotation = t.getMapRotationShort();
            if (mapRotation != null && !mapRotation.isEmpty()) {
                mapRotationContainer.setVisibility(View.VISIBLE);
                txtMapRotation.setText(mapRotation);
            } else {
                mapRotationContainer.setVisibility(View.GONE);
            }
        }

        // Refresh join-button state so slot counts / joined status stay accurate.
        // Does NOT re-attach click listeners.
        if (btnJoin != null) {
            String myUserId = ProfileCacheManager.getUserId(requireContext());

            if (!"upcoming".equalsIgnoreCase(t.getStatus())) {
                btnJoin.setVisibility(View.GONE);
            } else {
                btnJoin.setVisibility(View.VISIBLE);

                boolean isJoinedLocally = TournamentJoinStateManager.hasJoined(
                        requireContext(), myUserId, t.getId());
                boolean isJoinedAPI = t.isJoinedDerived(myUserId);

                if (isJoinedLocally || isJoinedAPI) {
                    btnJoin.setEnabled(false);
                    btnJoin.setAlpha(0.5f);
                    ((TextView) btnJoin).setText("Joined");
                } else {
                    String currentText = ((TextView) btnJoin).getText().toString();
                    if ("Joined".equalsIgnoreCase(currentText)) {
                        btnJoin.setEnabled(true);
                        btnJoin.setAlpha(1f);
                        ((TextView) btnJoin).setText("Join");
                    }
                }
            }
        }

        Log.d(TAG, "updateCardFields: patched card for tournament " + t.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void showComingSoon() {
        if (!isAdded() || tournamentsContainer == null) return;

        tournamentsContainer.removeAllViews();

        TextView comingSoon = new TextView(requireContext());
        comingSoon.setText("Coming Soon");
        comingSoon.setTextSize(18f);
        comingSoon.setTextColor(android.graphics.Color.WHITE);
        comingSoon.setTypeface(comingSoon.getTypeface(), android.graphics.Typeface.BOLD);
        comingSoon.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        comingSoon.setLayoutParams(params);

        tournamentsContainer.addView(comingSoon);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void markTournamentAsJoined(String tournamentId) {
        if (tournamentId == null || !isAdded() || tournamentsContainer == null) {
            return;
        }

        for (int i = 0; i < tournamentsContainer.getChildCount(); i++) {
            View card = tournamentsContainer.getChildAt(i);
            View btnJoin = card.findViewById(R.id.btnT1Join);

            if (btnJoin != null && btnJoin.getTag() != null && btnJoin.getTag().equals(tournamentId)) {
                btnJoin.setEnabled(false);
                btnJoin.setAlpha(0.5f);
                ((TextView) btnJoin).setText("Joined");
            }
        }
    }

    private View createTournamentCard(Tournament t) {
        if (t == null || t.getId() == null) {
            return null;
        }

        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_tournament_card, tournamentsContainer, false);

        // Tag the card root with tournament id — used by silentUpdateExistingCardsOnly()
        // and silentUpdateCards() to locate existing cards for in-place data updates.
        card.setTag(t.getId());

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

            String role = ProfileCacheManager.getRole(requireContext());
            String myUserId = ProfileCacheManager.getUserId(requireContext());

            if ("host".equalsIgnoreCase(role)
                    && "upcoming".equalsIgnoreCase(t.getStatus())) {

                if (t.getHostId() != null
                        && myUserId != null
                        && myUserId.equals(t.getHostId().id)) {

                    ((TextView) btnJoin).setText("You are Host");
                    btnJoin.setEnabled(false);
                    btnJoin.setAlpha(0.5f);
                }
                else if (t.getHostId() != null) {
                    btnJoin.setVisibility(View.GONE);
                }
                else {
                    if (t.hasApplied != null && t.hasApplied) {
                        String appStatus = t.applicationStatus != null ? t.applicationStatus : "pending";

                        if ("pending".equalsIgnoreCase(appStatus)) {
                            ((TextView) btnJoin).setText("Applied");
                            btnJoin.setEnabled(false);
                            btnJoin.setAlpha(0.5f);
                        } else if ("approved".equalsIgnoreCase(appStatus)) {
                            ((TextView) btnJoin).setText("You are Host");
                            btnJoin.setEnabled(false);
                            btnJoin.setAlpha(0.5f);
                        } else if ("rejected".equalsIgnoreCase(appStatus)) {
                            ((TextView) btnJoin).setText("Rejected");
                            btnJoin.setEnabled(false);
                            btnJoin.setAlpha(0.5f);
                        }
                    } else {
                        ((TextView) btnJoin).setText("Apply");
                        btnJoin.setEnabled(true);
                        btnJoin.setAlpha(1f);
                        btnJoin.setBackgroundResource(R.drawable.neon_button);

                        btnJoin.setOnClickListener(v ->
                                applyForHost(t.getId(), btnJoin)
                        );
                    }
                }

                return card;
            } else {
                btnJoin.setOnClickListener(v -> {
                    ProfileResponse.Data profile = ProfileCacheManager.getProfile(requireContext());

                    if (profile == null || profile.ign == null || profile.ign.trim().isEmpty()) {
                        Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                        startActivity(intent);
                    } else {
                        JoinTournamentDialog dialog = JoinTournamentDialog.newInstance(t);
                        dialog.show(getParentFragmentManager(), "JoinTournamentDialog");
                    }
                });

                if (!"upcoming".equalsIgnoreCase(t.getStatus())) {
                    btnJoin.setVisibility(View.GONE);
                } else {
                    boolean isJoinedLocally = TournamentJoinStateManager.hasJoined(
                            requireContext(),
                            myUserId,
                            t.getId()
                    );
                    boolean isJoinedAPI = t.isJoinedDerived(myUserId);

                    if (isJoinedLocally || isJoinedAPI) {
                        btnJoin.setEnabled(false);
                        btnJoin.setAlpha(0.5f);
                        ((TextView) btnJoin).setText("Joined");
                    }
                }
            }
        }

        return card;
    }

    private void applyForHost(String tournamentId, View btnApply) {
        btnApply.setEnabled(false);
        btnApply.setAlpha(0.6f);

        HostApplyRequest request = new HostApplyRequest(
                "2+ years hosting experience",
                "Interested in hosting this tournament",
                "Can manage lobby and results"
        );

        api.applyForHostTournament(tournamentId, request)
                .enqueue(new Callback<HostApplyResponse>() {
                    @Override
                    public void onResponse(Call<HostApplyResponse> call, Response<HostApplyResponse> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success) {

                            ((TextView) btnApply).setText("Applied");
                            btnApply.setAlpha(0.5f);
                        } else {
                            resetApply(btnApply);
                        }
                    }

                    @Override
                    public void onFailure(Call<HostApplyResponse> call, Throwable t) {
                        resetApply(btnApply);
                    }
                });
    }

    private void resetApply(View btn) {
        btn.setEnabled(true);
        btn.setAlpha(1f);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkAccountSwitch();

        // Refresh bell icon on resume — red dot shows correctly when returning to this screen
        updateNotificationIcon();
    }
}