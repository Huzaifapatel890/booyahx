package com.booyahx;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.text.InputType;
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
import com.booyahx.tournament.HostRulesBottomSheet;
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
import com.booyahx.network.models.EndTournamentRequest;
import com.booyahx.network.models.EndTournamentResponse;
import com.booyahx.network.models.HostTournament;
import com.booyahx.network.models.HostTournamentResponse;
import com.booyahx.network.models.Tournament;
import com.booyahx.network.models.UpdateRoomRequest;
import com.booyahx.network.models.UpdateRoomResponse;
import com.booyahx.tournament.RulesBottomSheet;

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
                        showRulesBottomSheet(tournament);
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

                        if (progressBar != null)
                            progressBar.setVisibility(View.GONE);

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

                        // Get current selected position to maintain filter
                        int currentPosition = statusSpinner.getSelectedItemPosition();
                        String currentStatus = (String) statusSpinner.getSelectedItem();

                        // Only set to Live on first load, otherwise maintain current filter
                        if (currentStatus != null) {
                            filterTournaments(currentStatus);
                        } else {
                            statusSpinner.setSelection(0);
                            filterTournaments("Live Tournaments");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<HostTournamentResponse> call,
                            Throwable t) {
                        if (progressBar != null)
                            progressBar.setVisibility(View.GONE);
                    }
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

        // Set numeric keyboard
        roomIdInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Get current values or set N/A
        String currentRoomId = tournament.getRoomIdDisplay();
        String currentPassword = tournament.getPasswordDisplay();

        if (currentRoomId == null || currentRoomId.isEmpty()) {
            currentRoomId = "N/A";
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            currentPassword = "N/A";
        }

        // Set hint and text color for N/A
        setupEditTextWithNA(roomIdInput, currentRoomId);
        setupEditTextWithNA(passwordInput, currentPassword);

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        updateBtn.setOnClickListener(v -> {
            String newRoomId = roomIdInput.getText().toString().trim();
            String newPassword = passwordInput.getText().toString().trim();

            if (newRoomId.isEmpty() || newRoomId.equals("N/A") ||
                    newPassword.isEmpty() || newPassword.equals("N/A")) {
                Toast.makeText(requireContext(),
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            updateRoomAPI(tournament.getId(), newRoomId, newPassword, dialog);
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void setupEditTextWithNA(EditText editText, String currentValue) {
        if (currentValue.equals("N/A")) {
            editText.setText("N/A");
            editText.setTextColor(Color.parseColor("#808080")); // Gray color
        } else {
            editText.setText(currentValue);
            editText.setTextColor(Color.WHITE);
        }

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (editText.getText().toString().equals("N/A")) {
                    editText.setText("");
                    editText.setTextColor(Color.WHITE);
                }
            } else {
                if (editText.getText().toString().trim().isEmpty()) {
                    editText.setText("N/A");
                    editText.setTextColor(Color.parseColor("#808080"));
                }
            }
        });
    }

    private void updateRoomAPI(String tournamentId, String roomId, String password, Dialog dialog) {
        UpdateRoomRequest request = new UpdateRoomRequest(roomId, password);

        apiService.updateRoom(tournamentId, request)
                .enqueue(new Callback<UpdateRoomResponse>() {
                    @Override
                    public void onResponse(Call<UpdateRoomResponse> call,
                                           Response<UpdateRoomResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UpdateRoomResponse updateResponse = response.body();

                            Toast.makeText(requireContext(),
                                    updateResponse.getMessage(),
                                    Toast.LENGTH_LONG).show();

                            // Update the tournament data locally without API call
                            for (HostTournament tournament : tournamentList) {
                                if (tournament.getId().equals(tournamentId)) {
                                    if (tournament.getRoom() != null) {
                                        tournament.getRoom().setRoomId(roomId);
                                        tournament.getRoom().setPassword(password);
                                    }
                                    break;
                                }
                            }

                            adapter.notifyDataSetChanged();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to update room",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UpdateRoomResponse> call,
                                          Throwable t) {
                        Toast.makeText(requireContext(),
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("UpdateRoom", "API Error", t);
                    }
                });
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

    private List<String> getTeamNamesFromTournament(HostTournament tournament) {
        List<String> teamNames = new ArrayList<>();

        if (tournament.getTeams() != null && !tournament.getTeams().isEmpty()) {
            for (HostTournament.Team team : tournament.getTeams()) {
                String teamName = team.getTeamName();
                if (teamName != null && !teamName.trim().isEmpty()) {
                    teamNames.add(teamName.trim());
                } else {
                    teamNames.add("Team " + (teamNames.size() + 1));
                }
            }
        } else {
            if (tournament.getParticipants() != null) {
                for (HostTournament.Participant p : tournament.getParticipants()) {
                    teamNames.add(p.getDisplayName());
                }
            }
        }

        return teamNames;
    }

    private void showRulesBottomSheet(HostTournament tournament) {
        HostRulesBottomSheet rulesSheet = HostRulesBottomSheet.newInstance(tournament);
        rulesSheet.show(getParentFragmentManager(), "HostRulesBottomSheet");
    }

    private Tournament createTournamentFromParcel(HostTournament hostTournament) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeString(hostTournament.getId());
            parcel.writeString(hostTournament.getGame());
            parcel.writeString(hostTournament.getMode());
            parcel.writeString(hostTournament.getSubMode());
            parcel.writeInt(hostTournament.getEntryFee());
            parcel.writeInt(hostTournament.getMaxPlayers());
            parcel.writeString(hostTournament.getDate());
            parcel.writeString(hostTournament.getStartTime());
            parcel.writeString(hostTournament.getLockTime());
            parcel.writeInt(hostTournament.getPrizePool());
            parcel.writeString(hostTournament.getStatus());

            parcel.setDataPosition(0);

            return Tournament.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
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

        // Set default reason
        String defaultReason = "Tournament finished.";
        reasonInput.setText(defaultReason);
        reasonInput.setTextColor(Color.parseColor("#808080")); // Gray color

        // Handle focus change for default text
        reasonInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (reasonInput.getText().toString().equals(defaultReason)) {
                    reasonInput.setText("");
                    reasonInput.setTextColor(Color.WHITE);
                }
            } else {
                if (reasonInput.getText().toString().trim().isEmpty()) {
                    reasonInput.setText(defaultReason);
                    reasonInput.setTextColor(Color.parseColor("#808080"));
                }
            }
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        endBtn.setOnClickListener(v -> {
            String reason = reasonInput.getText().toString().trim();

            // If empty or still default, use default message
            if (reason.isEmpty() || reason.equals(defaultReason)) {
                reason = defaultReason;
            }

            endTournamentAPI(tournament.getId(), reason, dialog);
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void endTournamentAPI(String tournamentId, String reason, Dialog dialog) {
        EndTournamentRequest request = new EndTournamentRequest(reason);

        apiService.endTournament(tournamentId, request)
                .enqueue(new Callback<EndTournamentResponse>() {
                    @Override
                    public void onResponse(Call<EndTournamentResponse> call,
                                           Response<EndTournamentResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            EndTournamentResponse endResponse = response.body();

                            Toast.makeText(requireContext(),
                                    endResponse.getMessage(),
                                    Toast.LENGTH_LONG).show();

                            dialog.dismiss();

                            // Remove ended tournament from current list
                            for (int i = 0; i < tournamentList.size(); i++) {
                                if (tournamentList.get(i).getId().equals(tournamentId)) {
                                    tournamentList.remove(i);
                                    adapter.notifyItemRemoved(i);
                                    break;
                                }
                            }

                            // Also remove from allLive list
                            for (int i = 0; i < allLive.size(); i++) {
                                if (allLive.get(i).getId().equals(tournamentId)) {
                                    allLive.remove(i);
                                    break;
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to end tournament",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<EndTournamentResponse> call,
                                          Throwable t) {
                        Toast.makeText(requireContext(),
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("EndTournament", "API Error", t);
                    }
                });
    }
}