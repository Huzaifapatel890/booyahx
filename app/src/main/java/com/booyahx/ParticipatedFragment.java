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

    // ✅ ALL MODES IS DEFAULT
    private String currentSubMode = "all";

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

        rvTournaments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new JoinedTournamentAdapter(null);
        rvTournaments.setAdapter(adapter);

        setupModeSpinner();
        fetchJoinedTournaments();

        getParentFragmentManager().setFragmentResultListener(
                "joined_refresh",
                this,
                (requestKey, bundle) -> {
                    Log.d(TAG, "🔄 joined_refresh received, refetching tournaments");
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

        // ✅ ALL MODES DEFAULT
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

    private void fetchJoinedTournaments() {
        Log.d(TAG, "🌐 Fetching joined tournaments from API...");

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
                    Log.e(TAG, "❌ Response body is NULL");
                    return;
                }

                if (response.body().getData() == null) {
                    Log.e(TAG, "❌ Response data is NULL");
                    return;
                }

                if (response.body().getData().getTournaments() == null) {
                    Log.e(TAG, "❌ Tournaments list is NULL");
                    return;
                }

                allTournaments = response.body().getData().getTournaments();
                Log.d(TAG, "✅ Successfully fetched " + allTournaments.size() + " tournaments");

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
                        Log.e(TAG, "❌ First tournament has NULL participants");
                    }
                }

                filterAndSortTournaments();
            }

            @Override
            public void onFailure(
                    @NonNull Call<JoinedTournamentResponse> call,
                    @NonNull Throwable t
            ) {
                Log.e(TAG, "❌❌❌ API FAILED", t);
                Log.e(TAG, "Error message: " + t.getMessage());
            }
        });
    }

    private void filterAndSortTournaments() {
        Log.d(TAG, "Filtering and sorting tournaments for subMode: " + currentSubMode);

        List<JoinedTournament> filtered = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        for (JoinedTournament t : allTournaments) {
            // ✅ FILTER 1: Only current date tournaments (no older tournaments)
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

            // ✅ FILTER 2: SubMode filter (solo, duo, squad, or all)
            if (!currentSubMode.equals("all")) {
                String subMode = t.getSubMode();
                if (subMode == null || !subMode.equalsIgnoreCase(currentSubMode)) {
                    Log.d(TAG, "Skipping tournament " + t.getLobbyName() + " - subMode mismatch (has: " + subMode + ", need: " + currentSubMode + ")");
                    continue;
                }
            }

            // Add tournament to filtered list
            filtered.add(t);
            Log.d(TAG, "Added tournament: " + t.getLobbyName() + " (date: " + tournamentDate + ", mode: " + t.getMode() + ", subMode: " + t.getSubMode() + ")");
        }

        // ✅ SORT BY TIME: Closest tournament first (ascending order)
        Collections.sort(filtered, new Comparator<JoinedTournament>() {
            @Override
            public int compare(JoinedTournament t1, JoinedTournament t2) {
                try {
                    String dateTime1 = t1.getDate() + " " + t1.getStartTime();
                    String dateTime2 = t2.getDate() + " " + t2.getStartTime();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                    Date date1 = sdf.parse(dateTime1);
                    Date date2 = sdf.parse(dateTime2);

                    if (date1 == null || date2 == null) return 0;

                    // Ascending order - closest tournament first
                    return date1.compareTo(date2);
                } catch (Exception e) {
                    Log.e(TAG, "Error comparing tournament times: " + e.getMessage());
                    return 0;
                }
            }
        });

        Log.d(TAG, "Filtered and sorted = " + filtered.size() + " tournaments | subMode = " + currentSubMode);

        // ✅ SHOW EMPTY STATE if no tournaments
        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvTournaments.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvTournaments.setVisibility(View.VISIBLE);
        }

        adapter.updateData(filtered);
    }
}