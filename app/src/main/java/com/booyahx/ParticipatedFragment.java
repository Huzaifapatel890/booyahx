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
        return inflater.inflate(R.layout.fragment_joined_tournaments, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
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
                    Log.d("JoinedTrace", "🔄 joined_refresh received, refetching tournaments");
                    fetchJoinedTournaments();
                }
        );
    }

    private void setupStatusSpinner() {

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

        spinnerTournamentStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TournamentStatusAdapter.StatusItem selected = statusAdapter.getItem(position);
                if (selected != null && !selected.apiValue.equals(currentStatus)) {
                    currentStatus = selected.apiValue;
                    filterTournaments();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void fetchJoinedTournaments() {

        ApiService api = ApiClient.getClient(requireContext())
                .create(ApiService.class);

        api.getJoinedTournaments().enqueue(new Callback<JoinedTournamentResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<JoinedTournamentResponse> call,
                    @NonNull Response<JoinedTournamentResponse> response
            ) {
                if (response.body() == null
                        || response.body().getData() == null
                        || response.body().getData().getTournaments() == null) {
                    return;
                }

                allTournaments = response.body().getData().getTournaments();
                filterTournaments();
            }

            @Override
            public void onFailure(
                    @NonNull Call<JoinedTournamentResponse> call,
                    @NonNull Throwable t
            ) {
                Log.e(TAG, "API FAILED", t);
            }
        });
    }

    private void filterTournaments() {

        List<JoinedTournament> filtered = new ArrayList<>();

        for (JoinedTournament t : allTournaments) {
            String status = t.getStatus();
            if (status == null) continue;

            // ✅ LIVE = upcoming + started
            if (currentStatus.equals("live")) {
                if (status.equalsIgnoreCase("upcoming") && hasStarted(t)) {
                    filtered.add(t);
                }
                continue;
            }

            // ✅ UPCOMING = upcoming BUT NOT started (🔥 FIX)
            if (currentStatus.equals("upcoming")) {
                if (status.equalsIgnoreCase("upcoming") && !hasStarted(t)) {
                    filtered.add(t);
                }
                continue;
            }

            // ✅ NORMAL STATUS MATCH
            if (status.equalsIgnoreCase(currentStatus)) {
                filtered.add(t);
            }
        }

        Log.d(TAG, "Filtered = " + filtered.size() + " | status = " + currentStatus);
        adapter.updateData(filtered);
    }

    // ✅ SAFE TIME CHECK
    private boolean hasStarted(JoinedTournament t) {
        try {
            String dateTime = t.getDate() + " " + t.getStartTime();
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date start = sdf.parse(dateTime);
            return System.currentTimeMillis() >= start.getTime();
        } catch (Exception e) {
            return false;
        }
    }
}