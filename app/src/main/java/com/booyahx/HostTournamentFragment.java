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

import com.booyahx.network.models.HostTournament;
import com.booyahx.network.models.HostTournamentResponse;
import com.booyahx.network.models.UpdateRoomRequest;
import com.booyahx.network.models.UpdateRoomResponse;
import com.booyahx.socket.SocketManager;

import java.text.ParseException;
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
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.activity_host_panel, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

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

        if (tournamentRecyclerView != null) {
            tournamentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

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
                    public void onOpenChat(HostTournament tournament) {
                        openTournamentChat(tournament);
                    }

                    @Override
                    public void onViewRules(HostTournament tournament) {
                        showRulesBottomSheet(tournament);
                    }
                });

        if (tournamentRecyclerView != null) {
            tournamentRecyclerView.setAdapter(adapter);
        }
    }

    private void setupStatusSpinner() {
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

            statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                    String selectedStatus = statusAdapter.getItem(position);
                    if (selectedStatus != null) {
                        filterTournaments(selectedStatus);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void loadTournamentsFromAPI() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        apiService.getHostMyLobbies()
                .enqueue(new Callback<HostTournamentResponse>() {

                    @Override
                    public void onResponse(
                            Call<HostTournamentResponse> call,
                            Response<HostTournamentResponse> response) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (!response.isSuccessful() || response.body() == null) {
                            return;
                        }

                        HostTournamentResponse.Lobbies l =
                                response.body().getData().getLobbies();

                        allUpcoming.clear();
                        allLive.clear();
                        allResultPending.clear();
                        allCompleted.clear();
                        allCancelled.clear();

                        if (l.getUpcoming() != null) allUpcoming.addAll(l.getUpcoming());
                        if (l.getLive() != null) allLive.addAll(l.getLive());
                        if (l.getResultPending() != null) allResultPending.addAll(l.getResultPending());

                        List<HostTournament> completedList = l.getCompleted();
                        if (completedList != null) {
                            for (int i = 0; i < completedList.size(); i++) {
                                HostTournament tournament = completedList.get(i);
                                String status = tournament.getStatus();
                                if (status != null && status.equalsIgnoreCase("cancelled")) {
                                    allCancelled.add(tournament);
                                } else {
                                    allCompleted.add(tournament);
                                }
                            }
                        }

                        if (l.getCancelled() != null && !l.getCancelled().isEmpty()) {
                            allCancelled.addAll(l.getCancelled());
                        }

                        // Subscribe host to realtime socket updates for all their tournaments
                        List<HostTournament> allHostTournaments = new ArrayList<>();
                        allHostTournaments.addAll(allUpcoming);
                        allHostTournaments.addAll(allLive);
                        allHostTournaments.addAll(allResultPending);
                        allHostTournaments.addAll(allCompleted);
                        allHostTournaments.addAll(allCancelled);
                        for (HostTournament tournament : allHostTournaments) {
                            SocketManager.subscribeToTournament(tournament.getId());
                        }

                        sortTournamentsByTime(allUpcoming, false);  // Ascending (closest upcoming first: 7 PM → 8 PM → 9 PM)
                        sortTournamentsByTime(allLive, false);      // Ascending (closest upcoming first)
                        sortTournamentsByTime(allResultPending, true);  // Descending (most recent past first)
                        sortTournamentsByTime(allCompleted, true);  // Descending (most recent past first)
                        sortTournamentsByTime(allCancelled, true);  // Descending (most recent past first)

                        int currentPosition = statusSpinner.getSelectedItemPosition();
                        String currentStatus = statusAdapter.getItem(currentPosition);

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

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (tournamentList.isEmpty()) {
            String message = "No " + status.toLowerCase();
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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

            updateRoomAPI(tournament, newRoomId, newPassword, dialog);
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

    private void updateRoomAPI(HostTournament tournament, String roomId, String password, Dialog dialog) {
        UpdateRoomRequest request = new UpdateRoomRequest(roomId, password);

        apiService.updateRoom(tournament.getId(), request)
                .enqueue(new Callback<UpdateRoomResponse>() {
                    @Override
                    public void onResponse(Call<UpdateRoomResponse> call,
                                           Response<UpdateRoomResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UpdateRoomResponse updateResponse = response.body();

                            Toast.makeText(requireContext(),
                                    updateResponse.getMessage(),
                                    Toast.LENGTH_LONG).show();

                            dialog.dismiss();

                            // Update the tournament object locally with new room ID and password
                            tournament.setRoomId(roomId);
                            tournament.setPassword(password);

                            // Find and update in all lists
                            updateTournamentInList(allUpcoming, tournament.getId(), roomId, password);
                            updateTournamentInList(allLive, tournament.getId(), roomId, password);
                            updateTournamentInList(allResultPending, tournament.getId(), roomId, password);
                            updateTournamentInList(allCompleted, tournament.getId(), roomId, password);
                            updateTournamentInList(allCancelled, tournament.getId(), roomId, password);

                            // Refresh the adapter to show updated values
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
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
                    }
                });
    }

    private void updateTournamentInList(List<HostTournament> list, String tournamentId, String roomId, String password) {
        for (HostTournament t : list) {
            if (t.getId().equals(tournamentId)) {
                t.setRoomId(roomId);
                t.setPassword(password);
                break;
            }
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
                teamNames,
                apiService).show();
    }


    private void showFinalResultDialog(HostTournament tournament) {


        new EnhancedFinalResultDialog(
                requireContext(),
                new ArrayList<>(),
                tournament.getId(),
                tournament.getStatus() != null ? tournament.getStatus() : "finished"
        ).show();
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
        }

        return teamNames;
    }

    private void showRulesBottomSheet(HostTournament tournament) {
        HostRulesBottomSheet rulesSheet = HostRulesBottomSheet.newInstance(tournament);
        rulesSheet.show(getParentFragmentManager(), "HostRulesBottomSheet");
    }

    private void openTournamentChat(HostTournament tournament) {
        // Get user info from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        // Launch TournamentChatActivity
        android.content.Intent intent = new android.content.Intent(requireContext(), TournamentChatActivity.class);
        intent.putExtra("tournament_id", tournament.getId());
        intent.putExtra("tournament_name", tournament.getHeaderTitle());
        intent.putExtra("is_host", true); // Host is opening the chat
        intent.putExtra("tournament_status", tournament.getStatus()); // ✅ FIX: Pass tournament status

        startActivity(intent);
    }

    private void sortTournamentsByTime(List<HostTournament> tournaments, boolean reverseOrder) {
        Collections.sort(tournaments, new Comparator<HostTournament>() {
            @Override
            public int compare(HostTournament t1, HostTournament t2) {
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

    private Date parseTournamentDateTime(HostTournament tournament) {
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

            // Try formats with AM/PM first (most common in your app based on screenshots)
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.cancelAllTimers();
        }
    }
}