package com.booyahx;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.booyahx.network.models.HostTournament;

import java.util.ArrayList;
import java.util.List;

public class HostTournamentFragment extends Fragment {

    private RecyclerView tournamentRecyclerView;
    private HostTournamentAdapter adapter;
    private List<HostTournament> tournamentList;
    private Spinner statusSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_host_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupStatusSpinner();
        loadTournaments();
    }

    private void initViews(View view) {
        tournamentRecyclerView = view.findViewById(R.id.tournamentRecyclerView);
        statusSpinner = view.findViewById(R.id.spinnerTournamentStatus);

        tournamentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tournamentList = new ArrayList<>();

        adapter = new HostTournamentAdapter(
                requireContext(),
                tournamentList,
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
                        Toast.makeText(
                                requireContext(),
                                "Rules: Coming Soon!",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

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
                        android.R.layout.simple_spinner_item,
                        statuses) {

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        tv.setTextColor(Color.parseColor("#00FFFF"));
                        tv.setTextSize(14);
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        tv.setTextColor(Color.WHITE);
                        tv.setBackgroundColor(Color.parseColor("#0a0a0a"));
                        tv.setPadding(30, 30, 30, 30);
                        return view;
                    }
                };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTournaments(statuses.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTournaments() {
        tournamentList.clear();
        tournamentList.add(new HostTournament(
                "1", "Free Fire", "BR - squad",
                "150 GC", "127 GC", "0/12",
                "CANCEL", "UPDA", "TING", "#0"
        ));
        tournamentList.add(new HostTournament(
                "2", "Free Fire", "LW - duo",
                "50 GC", "85 GC", "0/24",
                "CANCEL", "UPDA", "TING", "#0"
        ));
        adapter.notifyDataSetChanged();
    }

    private void filterTournaments(String status) {
        Toast.makeText(requireContext(), "Filtering: " + status, Toast.LENGTH_SHORT).show();
    }

    private void showUpdateRoomDialog(HostTournament tournament) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_host_update_room);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText roomIdInput = dialog.findViewById(R.id.roomIdInput);
        EditText passwordInput = dialog.findViewById(R.id.passwordInput);
        TextView cancelBtn = dialog.findViewById(R.id.cancelBtn);
        TextView updateBtn = dialog.findViewById(R.id.UpdateBtn);

        roomIdInput.setText(tournament.getRoomId());
        passwordInput.setText(tournament.getPassword());

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        updateBtn.setOnClickListener(v -> {
            if (roomIdInput.getText().toString().isEmpty()
                    || passwordInput.getText().toString().isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            tournament.setRoomId(roomIdInput.getText().toString());
            tournament.setPassword(passwordInput.getText().toString());
            adapter.notifyDataSetChanged();

            Toast.makeText(requireContext(), "Room updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showSubmitResultDialog(HostTournament tournament) {
        List<String> teamNames = getTeamNamesFromTournament(tournament.getId());
        int totalMatches = tournament.getMode().contains("3") ? 3 : 6;

        HostSubmitResultDialog dialog =
                new HostSubmitResultDialog(
                        requireContext(),
                        tournament.getId(),
                        totalMatches,
                        teamNames
                );

        dialog.show();
    }

    private void showFinalResultDialog(HostTournament tournament) {
        List<FinalRow> rows =
                FinalResultStore.load(requireContext(), tournament.getId());

        if (rows == null || rows.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Result not submitted yet!",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        EnhancedFinalResultDialog dialog = new EnhancedFinalResultDialog(requireContext(), rows);
        dialog.show();
    }

    private List<String> getTeamNamesFromTournament(String tournamentId) {
        List<String> teams = new ArrayList<>();
        teams.add("Bharuch esports");
        teams.add("Pine airlines");
        teams.add("NG Pros");
        teams.add("Iqoo Tg");
        teams.add("S8Ul ESP");
        teams.add("Team Zeta");
        teams.add("Team Eta");
        teams.add("Team Theta");
        teams.add("Team Iota");
        teams.add("Team Kappa");
        teams.add("Team Lambda");
        teams.add("Team Mu");
        return teams;
    }

    private void showEndTournamentDialog(HostTournament tournament) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_end_tournament_bg);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText reasonInput = dialog.findViewById(R.id.reasonInput);
        TextView cancelBtn = dialog.findViewById(R.id.cancelEndBtn);
        TextView endBtn = dialog.findViewById(R.id.endNowBtn);

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        endBtn.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Confirm")
                        .setMessage("End this tournament?")
                        .setPositiveButton("Yes", (d, w) -> {
                            Toast.makeText(requireContext(), "Tournament ended", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadTournaments();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        dialog.show();
    }
}