package com.booyahx;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.booyahx.R;
import com.booyahx.tournament.HostRulesBottomSheet;
import com.booyahx.adapters.HostStatusSpinnerAdapter;
import com.booyahx.Host.HostSubmitResultDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.Host.EnhancedFinalResultDialog;
import com.booyahx.Host.FinalResultStore;
import com.booyahx.Host.FinalRow;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.EndTournamentRequest;
import com.booyahx.network.models.EndTournamentResponse;
import com.booyahx.network.models.HostTournament;
import com.booyahx.network.models.HostTournamentResponse;
import com.booyahx.network.models.UpdateRoomRequest;
import com.booyahx.network.models.UpdateRoomResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HostTournamentFragment extends Fragment {

    private static final String TAG = "HostFragment_DEBUG";

    private RecyclerView tournamentRecyclerView;
    private HostTournamentAdapter adapter;
    private List<HostTournament> tournamentList;
    private Spinner statusSpinner;
    private HostStatusSpinnerAdapter statusAdapter;
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
        Log.d(TAG, "🔥 onCreateView called");
        View view = inflater.inflate(R.layout.activity_host_panel, container, false);
        Log.d(TAG, "✅ Layout inflated successfully");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "🔥 onViewCreated called");

        apiService = ApiClient.getClient(requireContext())
                .create(ApiService.class);
        Log.d(TAG, "✅ ApiService created");

        initViews(view);
        setupStatusSpinner();
        loadTournamentsFromAPI();
    }

    private void initViews(View view) {
        Log.d(TAG, "🔥 initViews called");

        tournamentRecyclerView = view.findViewById(R.id.tournamentRecyclerView);
        statusSpinner = view.findViewById(R.id.spinnerTournamentStatus);
        progressBar = view.findViewById(R.id.progressBar);

        Log.d(TAG, "RecyclerView null? " + (tournamentRecyclerView == null));
        Log.d(TAG, "Spinner null? " + (statusSpinner == null));
        Log.d(TAG, "ProgressBar null? " + (progressBar == null));

        if (tournamentRecyclerView != null) {
            tournamentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            Log.d(TAG, "✅ LayoutManager set");
        }

        tournamentList = new ArrayList<>();
        Log.d(TAG, "✅ Tournament list initialized: " + tournamentList.size());

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

        Log.d(TAG, "✅ Adapter created");

        if (tournamentRecyclerView != null) {
            tournamentRecyclerView.setAdapter(adapter);
            Log.d(TAG, "✅ Adapter set to RecyclerView");
        }
    }

    private void setupStatusSpinner() {
        Log.d(TAG, "🔥 setupStatusSpinner called");

        List<String> statusItems = new ArrayList<>();
        statusItems.add("Live Tournaments");
        statusItems.add("Upcoming Tournaments");
        statusItems.add("Completed Tournaments");
        statusItems.add("Pending Result Tournaments");
        statusItems.add("Cancelled Tournaments");

        statusAdapter = new HostStatusSpinnerAdapter(requireContext(), statusItems);

        if (statusSpinner != null) {
            statusSpinner.setAdapter(statusAdapter);
            statusSpinner.setSelection(0);
            Log.d(TAG, "✅ Spinner setup complete, default: Live Tournaments");

            statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                    String selectedStatus = statusAdapter.getItem(position);
                    Log.d(TAG, "📱 Spinner selection changed to: " + selectedStatus);
                    if (selectedStatus != null) {
                        filterTournaments(selectedStatus);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.d(TAG, "⚠️ Spinner: nothing selected");
                }
            });
        } else {
            Log.e(TAG, "❌ statusSpinner is NULL!");
        }
    }

    private void loadTournamentsFromAPI() {
        Log.d(TAG, "🔥 loadTournamentsFromAPI called");

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "✅ ProgressBar shown");
        }

        apiService.getHostMyLobbies()
                .enqueue(new Callback<HostTournamentResponse>() {

                    @Override
                    public void onResponse(
                            Call<HostTournamentResponse> call,
                            Response<HostTournamentResponse> response) {

                        Log.d(TAG, "🌐 API Response received");
                        Log.d(TAG, "Response code: " + response.code());
                        Log.d(TAG, "Response successful: " + response.isSuccessful());
                        Log.d(TAG, "Response body null? " + (response.body() == null));

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                            Log.d(TAG, "✅ ProgressBar hidden");
                        }

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e(TAG, "❌ Response unsuccessful or body is null");
                            return;
                        }

                        HostTournamentResponse.Lobbies l =
                                response.body().getData().getLobbies();

                        Log.d(TAG, "📦 Raw API data:");
                        Log.d(TAG, "  - Upcoming: " + (l.getUpcoming() != null ? l.getUpcoming().size() : "null"));
                        Log.d(TAG, "  - Live: " + (l.getLive() != null ? l.getLive().size() : "null"));
                        Log.d(TAG, "  - ResultPending: " + (l.getResultPending() != null ? l.getResultPending().size() : "null"));
                        Log.d(TAG, "  - Completed: " + (l.getCompleted() != null ? l.getCompleted().size() : "null"));
                        Log.d(TAG, "  - Cancelled: " + (l.getCancelled() != null ? l.getCancelled().size() : "null"));

                        allUpcoming.clear();
                        allLive.clear();
                        allResultPending.clear();
                        allCompleted.clear();
                        allCancelled.clear();

                        // Add tournaments from API response
                        if (l.getUpcoming() != null) allUpcoming.addAll(l.getUpcoming());
                        if (l.getLive() != null) allLive.addAll(l.getLive());
                        if (l.getResultPending() != null) allResultPending.addAll(l.getResultPending());

                        // Separate completed and cancelled tournaments
                        List<HostTournament> completedList = l.getCompleted();
                        if (completedList != null) {
                            Log.d(TAG, "🔍 Processing " + completedList.size() + " completed tournaments");

                            for (int i = 0; i < completedList.size(); i++) {
                                HostTournament tournament = completedList.get(i);
                                String status = tournament.getStatus();

                                Log.d(TAG, "  Tournament #" + (i+1) + ": status = '" + status + "', title = " + tournament.getHeaderTitle());

                                if (status != null && status.equalsIgnoreCase("cancelled")) {
                                    allCancelled.add(tournament);
                                    Log.d(TAG, "    ➡️ Added to CANCELLED");
                                } else {
                                    allCompleted.add(tournament);
                                    Log.d(TAG, "    ➡️ Added to COMPLETED");
                                }
                            }
                        }

                        // Also add from cancelled list if API provides it
                        if (l.getCancelled() != null && !l.getCancelled().isEmpty()) {
                            Log.d(TAG, "➕ Adding " + l.getCancelled().size() + " from API cancelled list");
                            allCancelled.addAll(l.getCancelled());
                        }

                        Log.d(TAG, "📊 Final counts after processing:");
                        Log.d(TAG, "  - Live: " + allLive.size());
                        Log.d(TAG, "  - Upcoming: " + allUpcoming.size());
                        Log.d(TAG, "  - Completed: " + allCompleted.size());
                        Log.d(TAG, "  - Cancelled: " + allCancelled.size());
                        Log.d(TAG, "  - Pending: " + allResultPending.size());

                        // Get current selected position to maintain filter
                        int currentPosition = statusSpinner.getSelectedItemPosition();
                        String currentStatus = statusAdapter.getItem(currentPosition);

                        Log.d(TAG, "Current spinner position: " + currentPosition);
                        Log.d(TAG, "Current spinner status: " + currentStatus);

                        // Filter based on current selection
                        if (currentStatus != null) {
                            filterTournaments(currentStatus);
                        } else {
                            Log.d(TAG, "⚠️ Current status is null, defaulting to Live");
                            statusSpinner.setSelection(0);
                            filterTournaments("Live Tournaments");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<HostTournamentResponse> call,
                            Throwable t) {
                        Log.e(TAG, "❌ API CALL FAILED!");
                        Log.e(TAG, "Error: " + t.getMessage(), t);

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        Toast.makeText(requireContext(),
                                "Failed to load tournaments: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filterTournaments(String status) {
        Log.d(TAG, "🔥 filterTournaments called with: " + status);

        int previousSize = tournamentList.size();
        tournamentList.clear();
        Log.d(TAG, "  Cleared list (was " + previousSize + " items)");

        switch (status) {
            case "Live Tournaments":
                tournamentList.addAll(allLive);
                Log.d(TAG, "  Added " + allLive.size() + " live tournaments");
                break;
            case "Upcoming Tournaments":
                tournamentList.addAll(allUpcoming);
                Log.d(TAG, "  Added " + allUpcoming.size() + " upcoming tournaments");
                break;
            case "Completed Tournaments":
                tournamentList.addAll(allCompleted);
                Log.d(TAG, "  Added " + allCompleted.size() + " completed tournaments");
                break;
            case "Pending Result Tournaments":
                tournamentList.addAll(allResultPending);
                Log.d(TAG, "  Added " + allResultPending.size() + " pending tournaments");
                break;
            case "Cancelled Tournaments":
                tournamentList.addAll(allCancelled);
                Log.d(TAG, "  Added " + allCancelled.size() + " cancelled tournaments");

                // Extra debug for cancelled
                for (int i = 0; i < allCancelled.size(); i++) {
                    HostTournament t = allCancelled.get(i);
                    Log.d(TAG, "    Cancelled #" + (i+1) + ": " + t.getHeaderTitle() + " | Status: " + t.getStatus());
                }
                break;
        }

        Log.d(TAG, "📋 Tournament list now has " + tournamentList.size() + " items");
        Log.d(TAG, "Adapter null? " + (adapter == null));

        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.d(TAG, "✅ Adapter.notifyDataSetChanged() called");
        } else {
            Log.e(TAG, "❌ ADAPTER IS NULL!");
        }

        if (tournamentList.isEmpty()) {
            String message = "No " + status.toLowerCase();
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "⚠️ List empty, showing toast: " + message);
        } else {
            Log.d(TAG, "✅ List has items, should be rendering now!");
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

        roomIdInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        String currentRoomId = tournament.getRoomIdDisplay();
        String currentPassword = tournament.getPasswordDisplay();

        if (currentRoomId == null || currentRoomId.isEmpty()) {
            currentRoomId = "N/A";
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            currentPassword = "N/A";
        }

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
            editText.setTextColor(Color.parseColor("#808080"));
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
                        Log.e(TAG, "UpdateRoom API Error", t);
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

    // ✅ FIXED: Updated to work with List<String> participants
    private List<String> getTeamNamesFromTournament(HostTournament tournament) {
        List<String> teamNames = new ArrayList<>();

        // Get team names from teams array (this is the primary source)
        if (tournament.getTeams() != null && !tournament.getTeams().isEmpty()) {
            for (HostTournament.Team team : tournament.getTeams()) {
                String teamName = team.getTeamName();
                if (teamName != null && !teamName.trim().isEmpty()) {
                    teamNames.add(teamName.trim());
                } else {
                    teamNames.add("Team " + (teamNames.size() + 1));
                }
            }
        }
        // Note: participants is now just a List<String> of IDs
        // If you need actual participant names, you'd need to fetch them separately

        return teamNames;
    }

    private void showRulesBottomSheet(HostTournament tournament) {
        HostRulesBottomSheet rulesSheet = HostRulesBottomSheet.newInstance(tournament);
        rulesSheet.show(getParentFragmentManager(), "HostRulesBottomSheet");
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

        String defaultReason = "Tournament finished.";
        reasonInput.setText(defaultReason);
        reasonInput.setTextColor(Color.parseColor("#808080"));

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

                            for (int i = 0; i < tournamentList.size(); i++) {
                                if (tournamentList.get(i).getId().equals(tournamentId)) {
                                    tournamentList.remove(i);
                                    adapter.notifyItemRemoved(i);
                                    break;
                                }
                            }

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
                        Log.e(TAG, "EndTournament API Error", t);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "🔥 onDestroyView called");
        if (adapter != null) {
            adapter.cancelAllTimers();
            Log.d(TAG, "✅ All timers cancelled");
        }
    }
}