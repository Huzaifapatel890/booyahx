package com.booyahx;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.Host.EnhancedFinalResultDialog;
import com.booyahx.Host.FinalResultStore;
import com.booyahx.Host.FinalRow;
import com.booyahx.Host.HostSubmitResultDialog;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.HostTournament;
import com.booyahx.network.models.HostTournamentResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HostTournamentFragment extends Fragment {

    private RecyclerView tournamentRecyclerView;
    private HostTournamentAdapter adapter;
    private List<HostTournament> tournamentList;
    private Spinner statusSpinner;
    private ProgressBar progressBar;

    private ApiService apiService;
    private List<HostTournament> allUpcoming = new ArrayList<>();
    private List<HostTournament> allLive = new ArrayList<>();
    private List<HostTournament> allResultPending = new ArrayList<>();
    private List<HostTournament> allCompleted = new ArrayList<>();
    private List<HostTournament> allCancelled = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_host_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient(requireContext())
                .create(ApiService.class);
        initViews(view);
        setupStatusSpinner();
        loadTournamentsFromAPI();
    }

    private void initViews(View view) {
        tournamentRecyclerView = view.findViewById(R.id.tournamentRecyclerView);
        statusSpinner = view.findViewById(R.id.spinnerTournamentStatus);
        progressBar = view.findViewById(R.id.progressBar);

        tournamentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tournamentList = new ArrayList<>();

        adapter = new HostTournamentAdapter(requireContext(), tournamentList,
                new HostTournamentAdapter.OnItemClickListener() {

                    @Override
                    public void onEditRoom(HostTournament tournament) {
                        showUpdateRoomDialog(tournament);
                    }

                    @Override
                    public void onSubmitResult(HostTournament tournament) {
                        showSubmitResultDialog(tournament);
                    }

                    @Override
                    public void onViewResult(HostTournament tournament) {
                        showFinalResultDialog(tournament);
                    }

                    @Override
                    public void onEndTournament(HostTournament tournament) {
                        showEndTournamentDialog(tournament);
                    }

                    @Override
                    public void onViewRules(HostTournament tournament) {
                        Toast.makeText(requireContext(),
                                "Rules: Coming Soon!",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        tournamentRecyclerView.setAdapter(adapter);
    }

    private void setupStatusSpinner() {
        List<String> statuses = new ArrayList<>();
        statuses.add("Live Tournaments");
        statuses.add("Upcoming Tournaments");
        statuses.add("Completed Tournaments");
        statuses.add("Pending Result Tournaments");
        statuses.add("Cancelled Tournaments");

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<String>(requireContext(),
                        android.R.layout.simple_spinner_item, statuses) {

                    @NonNull
                    @Override
                    public View getView(int position,
                                        View convertView,
                                        @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        tv.setTextColor(Color.parseColor("#00FFFF"));
                        tv.setTextSize(14);
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position,
                                                View convertView,
                                                @NonNull ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        tv.setTextColor(Color.WHITE);
                        tv.setBackgroundColor(Color.parseColor("#0a0a0a"));
                        tv.setPadding(30, 30, 30, 30);
                        return view;
                    }
                };

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        statusSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {
                        filterTournaments(statuses.get(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
    }

    private void loadTournamentsFromAPI() {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        apiService.getHostMyLobbies()
                .enqueue(new Callback<HostTournamentResponse>() {

                    @Override
                    public void onResponse(
                            Call<HostTournamentResponse> call,
                            Response<HostTournamentResponse> response) {

                        if (!response.isSuccessful()
                                || response.body() == null) return;

                        HostTournamentResponse.Lobbies l =
                                response.body().getData().getLobbies();

                        allUpcoming.clear();
                        allLive.clear();
                        allResultPending.clear();
                        allCompleted.clear();
                        allCancelled.clear();

                        allUpcoming.addAll(l.getUpcoming());
                        allLive.addAll(l.getLive());
                        allResultPending.addAll(l.getResultPending());
                        allCompleted.addAll(l.getCompleted());
                        allCancelled.addAll(l.getCancelled());

                        statusSpinner.setSelection(0);
                        filterTournaments("Live Tournaments");
                    }

                    @Override
                    public void onFailure(
                            Call<HostTournamentResponse> call,
                            Throwable t) {}
                });
    }

    private String getAuthToken() {
        SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        "AppPrefs", Context.MODE_PRIVATE);
        return "Bearer " + prefs.getString("auth_token", "");
    }

    private void filterTournaments(String status) {
        tournamentList.clear();

        switch (status) {
            case "Live Tournaments":
                tournamentList.addAll(allLive);
                break;
            case "Upcoming Tournaments":
                tournamentList.addAll(allUpcoming);
                break;
            case "Completed Tournaments":
                tournamentList.addAll(allCompleted);
                break;
            case "Pending Result Tournaments":
                tournamentList.addAll(allResultPending);
                break;
            case "Cancelled Tournaments":
                tournamentList.addAll(allCancelled);
                break;
        }

        adapter.notifyDataSetChanged();

        if (tournamentList.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No " + status.toLowerCase(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showUpdateRoomDialog(HostTournament tournament) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_host_update_room);
        if (dialog.getWindow() != null) {
            dialog.getWindow()
                    .setBackgroundDrawable(
                            new ColorDrawable(Color.TRANSPARENT));
        }

        EditText roomIdInput = dialog.findViewById(R.id.roomIdInput);
        EditText passwordInput = dialog.findViewById(R.id.passwordInput);
        TextView cancelBtn = dialog.findViewById(R.id.cancelBtn);
        TextView updateBtn = dialog.findViewById(R.id.UpdateBtn);

        roomIdInput.setText(tournament.getRoomIdDisplay());
        passwordInput.setText(tournament.getPasswordDisplay());

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        updateBtn.setOnClickListener(v -> {
            String newRoomId = roomIdInput.getText().toString().trim();
            String newPassword = passwordInput.getText().toString().trim();

            if (newRoomId.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (tournament.getRoom() != null) {
                tournament.getRoom().setRoomId(newRoomId);
                tournament.getRoom().setPassword(newPassword);
            }

            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(),
                    "Room updated",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showSubmitResultDialog(HostTournament tournament) {
        List<String> teamNames = getTeamNamesFromTournament(tournament);
        int totalMatches =
                tournament.getMode() != null
                        && tournament.getMode().contains("3")
                        ? 3 : 6;

        new HostSubmitResultDialog(requireContext(),
                tournament.getId(),
                totalMatches,
                teamNames).show();
    }

    private void showFinalResultDialog(HostTournament tournament) {
        List<FinalRow> rows =
                FinalResultStore.load(requireContext(),
                        tournament.getId());

        if (rows == null || rows.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Result not submitted yet!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new EnhancedFinalResultDialog(
                requireContext(), rows).show();
    }

    // ✅ COMPLETELY FIXED: Extract team names from teams array, NOT participants
    private List<String> getTeamNamesFromTournament(HostTournament tournament) {
        List<String> teamNames = new ArrayList<>();

        // ✅ Use the teams array which contains actual team names
        if (tournament.getTeams() != null && !tournament.getTeams().isEmpty()) {
            for (HostTournament.Team team : tournament.getTeams()) {
                String teamName = team.getTeamName();
                if (teamName != null && !teamName.trim().isEmpty()) {
                    teamNames.add(teamName.trim());
                } else {
                    // Fallback if team name is somehow empty
                    teamNames.add("Team " + (teamNames.size() + 1));
                }
            }
        } else {
            // Fallback to participants if teams array is missing (shouldn't happen)
            if (tournament.getParticipants() != null) {
                for (HostTournament.Participant p : tournament.getParticipants()) {
                    teamNames.add(p.getDisplayName());
                }
            }
        }

        return teamNames;
    }

    private void showEndTournamentDialog(HostTournament tournament) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_end_tournament_bg);
        if (dialog.getWindow() != null) {
            dialog.getWindow()
                    .setBackgroundDrawable(
                            new ColorDrawable(Color.TRANSPARENT));
        }

        EditText reasonInput = dialog.findViewById(R.id.reasonInput);
        TextView cancelBtn = dialog.findViewById(R.id.cancelEndBtn);
        TextView endBtn = dialog.findViewById(R.id.endNowBtn);

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        endBtn.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(
                        requireContext())
                        .setTitle("Confirm")
                        .setMessage("End this tournament?")
                        .setPositiveButton("Yes",
                                (d, w) -> {
                                    Toast.makeText(requireContext(),
                                            "Tournament ended",
                                            Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadTournamentsFromAPI();
                                })
                        .setNegativeButton("Cancel", null)
                        .show());

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}