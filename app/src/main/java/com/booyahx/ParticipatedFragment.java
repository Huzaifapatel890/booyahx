package com.booyahx;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;
import com.booyahx.adapters.TournamentStatusAdapter;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.JoinedTournament;
import com.booyahx.network.models.JoinedTournamentResponse;
import com.booyahx.tournament.JoinedTournamentAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParticipatedFragment extends Fragment {

    private static final String TAG = "JoinedTrace";

    RecyclerView rvTournaments;
    JoinedTournamentAdapter adapter;
    TextView tvEmptyState;

    private Spinner spinnerMode;
    private TournamentStatusAdapter modeAdapter;

    // ✅ NEW: Second spinner for game mode (BR / CS / Special)
    private Spinner spinnerGameMode;
    private TournamentStatusAdapter gameModeAdapter;

    // All modes is default
    private String currentSubMode = "all";

    // ✅ NEW: Game mode filter — "all" means no filter applied
    private String currentGameMode = "all";

    private List<JoinedTournament> allTournaments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_joined_tournaments, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        Log.d(TAG, "onViewCreated called");

        rvTournaments = view.findViewById(R.id.rvTournaments);
        spinnerMode = view.findViewById(R.id.spinnerMode);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        // ✅ NEW: Bind game mode spinner (placed after spinnerMode in the same row in layout)
        spinnerGameMode = view.findViewById(R.id.spinnerGameMode);

        rvTournaments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new JoinedTournamentAdapter(null);
        rvTournaments.setAdapter(adapter);

        setupModeSpinner();
        // ✅ NEW: Set up game mode spinner
        setupGameModeSpinner();
        fetchJoinedTournaments();

        getParentFragmentManager().setFragmentResultListener(
                "joined_refresh",
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    Log.d(TAG, "joined_refresh received, refetching tournaments");
                    fetchJoinedTournaments();
                }
        );

        getParentFragmentManager().setFragmentResultListener(
                "tournament_status_changed",
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    Log.d(TAG, "tournament_status_changed received, refetching tournaments");
                    fetchJoinedTournaments();
                }
        );
    }

    private void setupModeSpinner() {
        Log.d(TAG, "Setting up mode spinner");

        List<TournamentStatusAdapter.StatusItem> modeItems = new ArrayList<>();
        modeItems.add(new TournamentStatusAdapter.StatusItem("all", "All Modes"));
        modeItems.add(new TournamentStatusAdapter.StatusItem("solo", "Solo"));
        modeItems.add(new TournamentStatusAdapter.StatusItem("duo", "Duo"));
        modeItems.add(new TournamentStatusAdapter.StatusItem("squad", "Squad"));

        modeAdapter = new TournamentStatusAdapter(requireContext(), modeItems);
        spinnerMode.setAdapter(modeAdapter);

        // All modes default
        spinnerMode.setSelection(0);
        Log.d(TAG, "Mode spinner set to default: all");

        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TournamentStatusAdapter.StatusItem selected = modeAdapter.getItem(position);
                if (selected != null && !selected.apiValue.equals(currentSubMode)) {
                    Log.d(TAG, "SubMode changed from " + currentSubMode + " to " + selected.apiValue);
                    currentSubMode = selected.apiValue;
                    filterAndSortTournaments();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    // ✅ NEW: Game mode spinner — BR / CS / Special (LW), placed in same row as subMode spinner
    // ✅ FIX: When CS is selected → subMode spinner auto-resets to "All Modes" and is fully locked
    //         (no dropdown can open). When non-CS is selected → subMode spinner is re-enabled.
    private void setupGameModeSpinner() {
        Log.d(TAG, "Setting up game mode spinner");

        List<TournamentStatusAdapter.StatusItem> gameModeItems = new ArrayList<>();
        gameModeItems.add(new TournamentStatusAdapter.StatusItem("all", "All"));
        gameModeItems.add(new TournamentStatusAdapter.StatusItem("BR",  "BR"));
        gameModeItems.add(new TournamentStatusAdapter.StatusItem("CS",  "CS"));
        gameModeItems.add(new TournamentStatusAdapter.StatusItem("LW",  "Special"));

        gameModeAdapter = new TournamentStatusAdapter(requireContext(), gameModeItems);
        spinnerGameMode.setAdapter(gameModeAdapter);

        // Default: All
        spinnerGameMode.setSelection(0);
        Log.d(TAG, "Game mode spinner set to default: all");

        spinnerGameMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TournamentStatusAdapter.StatusItem selected = gameModeAdapter.getItem(position);
                if (selected != null && !selected.apiValue.equals(currentGameMode)) {
                    Log.d(TAG, "GameMode changed from " + currentGameMode + " to " + selected.apiValue);
                    currentGameMode = selected.apiValue;

                    // ✅ FIX: CS has no subMode — auto-reset subMode to "All Modes" and lock spinner
                    if ("CS".equals(currentGameMode)) {
                        Log.d(TAG, "CS selected → resetting subMode spinner to 'All Modes' and locking it");
                        currentSubMode = "all";
                        spinnerMode.setSelection(0);       // force back to "All Modes"
                        spinnerMode.setEnabled(false);     // block touch interaction
                        spinnerMode.setClickable(false);   // prevent click from opening dropdown
                        spinnerMode.setFocusable(false);   // prevent focus-based dropdown
                        spinnerMode.setAlpha(0.4f);        // visual cue: dimmed = locked/unavailable
                    } else {
                        // ✅ Non-CS selected → restore subMode spinner to fully interactive
                        Log.d(TAG, "Non-CS selected → unlocking subMode spinner");
                        spinnerMode.setEnabled(true);
                        spinnerMode.setClickable(true);
                        spinnerMode.setFocusable(true);
                        spinnerMode.setAlpha(1.0f);
                    }

                    filterAndSortTournaments();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void fetchJoinedTournaments() {
        Log.d(TAG, "Fetching joined tournaments from API...");

        ApiService api = ApiClient.getClient(requireContext())
                .create(ApiService.class);

        api.getJoinedTournaments().enqueue(new Callback<JoinedTournamentResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<JoinedTournamentResponse> call,
                    @NonNull Response<JoinedTournamentResponse> response
            ) {
                Log.d(TAG, "API Response received: " + response.code());

                if (response.body() == null) {
                    Log.e(TAG, "Response body is NULL");
                    return;
                }

                if (response.body().getData() == null) {
                    Log.e(TAG, "Response data is NULL");
                    return;
                }

                if (response.body().getData().getTournaments() == null) {
                    Log.e(TAG, "Tournaments list is NULL");
                    return;
                }

                allTournaments = response.body().getData().getTournaments();
                Log.d(TAG, "Successfully fetched " + allTournaments.size() + " tournaments");

                // Debug: Print first tournament's participants
                if (!allTournaments.isEmpty()) {
                    JoinedTournament firstTournament = allTournaments.get(0);
                    Log.d(TAG, "First tournament: " + firstTournament.getLobbyName());

                    if (firstTournament.getParticipants() != null) {
                        Log.d(TAG, "Participants count: " + firstTournament.getParticipants().size());
                        for (int i = 0; i < firstTournament.getParticipants().size(); i++) {
                            JoinedTournament.Participant p = firstTournament.getParticipants().get(i);
                            Log.d(TAG, "Participant[" + i + "]: userId=" + p.userId + ", name=" + p.name + ", ign=" + p.ign);
                        }
                    } else {
                        Log.e(TAG, "First tournament has NULL participants");
                    }
                }

                filterAndSortTournaments();
            }

            @Override
            public void onFailure(
                    @NonNull Call<JoinedTournamentResponse> call,
                    @NonNull Throwable t
            ) {
                Log.e(TAG, "API FAILED", t);
                Log.e(TAG, "Error message: " + t.getMessage());
            }
        });
    }

    private void filterAndSortTournaments() {
        Log.d(TAG, "Filtering and sorting tournaments for subMode: " + currentSubMode + ", gameMode: " + currentGameMode);

        List<JoinedTournament> filtered = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        for (JoinedTournament t : allTournaments) {
            // Filter 1: Only current date tournaments (no older tournaments)
            String tournamentDate = t.getDate();
            if (tournamentDate == null || tournamentDate.isEmpty()) {
                Log.w(TAG, "Tournament " + t.getId() + " has NULL or empty date");
                continue;
            }

            // Compare dates - only include today and future tournaments
            if (tournamentDate.compareTo(todayDate) < 0) {
                Log.d(TAG, "Skipping old tournament: " + t.getLobbyName() + " (date: " + tournamentDate + ")");
                continue;
            }

            // Filter 2: SubMode filter (solo, duo, squad, or all)
            if (!currentSubMode.equals("all")) {
                String subMode = t.getSubMode();
                if (subMode == null || !subMode.equalsIgnoreCase(currentSubMode)) {
                    Log.d(TAG, "Skipping tournament " + t.getLobbyName() + " - subMode mismatch (has: " + subMode + ", need: " + currentSubMode + ")");
                    continue;
                }
            }

            // ✅ NEW Filter 3: Game mode filter (BR, CS, LW/Special, or all)
            if (!currentGameMode.equals("all")) {
                String gameMode = t.getMode();
                if (gameMode == null || !gameMode.equalsIgnoreCase(currentGameMode)) {
                    Log.d(TAG, "Skipping tournament " + t.getLobbyName() + " - gameMode mismatch (has: " + gameMode + ", need: " + currentGameMode + ")");
                    continue;
                }
            }

            // Add tournament to filtered list
            filtered.add(t);
            Log.d(TAG, "Added tournament: " + t.getLobbyName() + " (date: " + tournamentDate + ", mode: " + t.getMode() + ", subMode: " + t.getSubMode() + ", status: " + t.getStatus() + ")");
        }

        // Sorting logic: Status priority + Time within each status
        Collections.sort(filtered, new Comparator<JoinedTournament>() {
            @Override
            public int compare(JoinedTournament t1, JoinedTournament t2) {
                int priority1 = getStatusPriority(t1.getStatus());
                int priority2 = getStatusPriority(t2.getStatus());

                Log.d(TAG, "Comparing: " + t1.getLobbyName() + " (priority=" + priority1 + ", status=" + t1.getStatus() + ") vs " +
                        t2.getLobbyName() + " (priority=" + priority2 + ", status=" + t2.getStatus() + ")");

                if (priority1 != priority2) {
                    return Integer.compare(priority1, priority2);
                }

                try {
                    String dateTime1 = t1.getDate() + " " + t1.getStartTime();
                    String dateTime2 = t2.getDate() + " " + t2.getStartTime();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                    Date date1 = sdf.parse(dateTime1);
                    Date date2 = sdf.parse(dateTime2);

                    if (date1 == null || date2 == null) return 0;
                    return date1.compareTo(date2);
                } catch (Exception e) {
                    Log.e(TAG, "Error comparing tournament times: " + e.getMessage());
                    return 0;
                }
            }
        });

        Log.d(TAG, "FINAL SORTED ORDER:");
        for (int i = 0; i < filtered.size(); i++) {
            JoinedTournament t = filtered.get(i);
            Log.d(TAG, (i + 1) + ". " + t.getLobbyName() + " | Status: " + t.getStatus() + " | Time: " + t.getStartTime());
        }

        Log.d(TAG, "Filtered and sorted = " + filtered.size() + " tournaments | subMode = " + currentSubMode + " | gameMode = " + currentGameMode);

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvTournaments.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvTournaments.setVisibility(View.VISIBLE);
        }

        adapter.updateData(filtered);
    }

    private int getStatusPriority(String status) {
        if (status == null || status.isEmpty()) {
            return 1; // Treat null/empty as upcoming
        }

        String statusLower = status.toLowerCase(Locale.getDefault());

        if (statusLower.contains("running") || statusLower.contains("live")) {
            Log.d(TAG, "Status '" + status + "' classified as RUNNING (priority 0)");
            return 0;
        }
        if (statusLower.contains("upcoming")) {
            Log.d(TAG, "Status '" + status + "' classified as UPCOMING (priority 1)");
            return 1;
        }
        if (statusLower.contains("pending")) {
            Log.d(TAG, "Status '" + status + "' classified as PENDING (priority 2)");
            return 2;
        }
        if (statusLower.contains("finished") || statusLower.contains("completed")) {
            Log.d(TAG, "Status '" + status + "' classified as FINISHED (priority 3)");
            return 3;
        }
        if (statusLower.contains("cancel")) {
            Log.d(TAG, "Status '" + status + "' classified as CANCELLED (priority 4)");
            return 4;
        }

        Log.d(TAG, "Status '" + status + "' not recognized, treating as UPCOMING (priority 1)");
        return 1;
    }
}