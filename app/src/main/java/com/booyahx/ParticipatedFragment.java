package com.booyahx;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.adapters.TournamentStatusAdapter;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.JoinedTournament;
import com.booyahx.network.models.JoinedTournamentResponse;
import com.booyahx.tournament.JoinedTournamentAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private Spinner spinnerTournamentStatus;
    private TournamentStatusAdapter statusAdapter;

    // ✅ LIVE IS DEFAULT (DERIVED LOCALLY)
    private String currentStatus = "live";

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
        spinnerTournamentStatus = view.findViewById(R.id.spinnerTournamentStatus);

        rvTournaments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new JoinedTournamentAdapter(null);
        rvTournaments.setAdapter(adapter);

        setupStatusSpinner();
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

    private void setupStatusSpinner() {
        Log.d(TAG, "Setting up status spinner");

        List<TournamentStatusAdapter.StatusItem> statusItems = new ArrayList<>();
        statusItems.add(new TournamentStatusAdapter.StatusItem("live", "Live Tournaments"));
        statusItems.add(new TournamentStatusAdapter.StatusItem("upcoming", "Upcoming Tournaments"));
        statusItems.add(new TournamentStatusAdapter.StatusItem("completed", "Completed Tournaments"));
        statusItems.add(new TournamentStatusAdapter.StatusItem("pending", "Pending Result Tournaments"));
        statusItems.add(new TournamentStatusAdapter.StatusItem("cancelled", "Cancelled Tournaments"));

        statusAdapter = new TournamentStatusAdapter(requireContext(), statusItems);
        spinnerTournamentStatus.setAdapter(statusAdapter);

        // ✅ LIVE DEFAULT
        spinnerTournamentStatus.setSelection(0);
        Log.d(TAG, "Status spinner set to default: live");

        spinnerTournamentStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TournamentStatusAdapter.StatusItem selected = statusAdapter.getItem(position);
                if (selected != null && !selected.apiValue.equals(currentStatus)) {
                    Log.d(TAG, "Status changed from " + currentStatus + " to " + selected.apiValue);
                    currentStatus = selected.apiValue;
                    filterTournaments();
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

                filterTournaments();
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

    private void filterTournaments() {
        Log.d(TAG, "Filtering tournaments for status: " + currentStatus);

        List<JoinedTournament> filtered = new ArrayList<>();

        for (JoinedTournament t : allTournaments) {
            String status = t.getStatus();
            if (status == null) {
                Log.w(TAG, "Tournament " + t.getId() + " has NULL status");
                continue;
            }

            // ✅ LIVE = upcoming + started
            if (currentStatus.equals("live")) {
                if (status.equalsIgnoreCase("upcoming") && hasStarted(t)) {
                    filtered.add(t);
                    Log.d(TAG, "Added to LIVE: " + t.getLobbyName());
                }
                continue;
            }

            // ✅ UPCOMING = upcoming BUT NOT started (🔥 FIX)
            if (currentStatus.equals("upcoming")) {
                if (status.equalsIgnoreCase("upcoming") && !hasStarted(t)) {
                    filtered.add(t);
                    Log.d(TAG, "Added to UPCOMING: " + t.getLobbyName());
                }
                continue;
            }

            // ✅ NORMAL STATUS MATCH
            if (status.equalsIgnoreCase(currentStatus)) {
                filtered.add(t);
                Log.d(TAG, "Added to " + currentStatus.toUpperCase() + ": " + t.getLobbyName());
            }
        }

        Log.d(TAG, "Filtered = " + filtered.size() + " tournaments | status = " + currentStatus);
        adapter.updateData(filtered);
    }

    // ✅ SAFE TIME CHECK
    private boolean hasStarted(JoinedTournament t) {
        try {
            String dateTime = t.getDate() + " " + t.getStartTime();
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date start = sdf.parse(dateTime);
            boolean started = System.currentTimeMillis() >= start.getTime();
            Log.d(TAG, "Tournament " + t.getLobbyName() + " hasStarted: " + started);
            return started;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if tournament started: " + e.getMessage());
            return false;
        }
    }
}