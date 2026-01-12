package com.booyahx;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.booyahx.HostTournamentAdapter;
import com.booyahx.R;
import com.booyahx.network.models.HostTournament;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HostTournamentActivity extends AppCompatActivity {

    private RecyclerView tournamentRecyclerView;
    private HostTournamentAdapter adapter;
    private List<HostTournament> tournamentList;
    private Spinner statusSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_panel);

        initViews();
        setupStatusSpinner();
        loadTournaments();
    }

    private void initViews() {
        tournamentRecyclerView = findViewById(R.id.tournamentRecyclerView);
        statusSpinner = findViewById(R.id.spinnerTournamentStatus);

        tournamentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tournamentList = new ArrayList<>();
        adapter = new HostTournamentAdapter(this, tournamentList, new HostTournamentAdapter.OnItemClickListener() {
            @Override
            public void onEditRoom(HostTournament tournament) {
                showUpdateRoomDialog(tournament);
            }

            @Override
            public void onSubmitResult(HostTournament tournament) {
                showSubmitResultDialog(tournament);
            }

            @Override
            public void onEndTournament(HostTournament tournament) {
                showEndTournamentDialog(tournament);
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

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, statuses) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.parseColor("#00FFFF"));
                textView.setTextSize(14);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.WHITE);
                textView.setBackgroundColor(Color.parseColor("#0a0a0a"));
                textView.setPadding(30, 30, 30, 30);
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
        tournamentList.add(new HostTournament("1", "Free Fire", "BR - squad", "150 GC", "127 GC", "0/12", "CANCEL", "UPDA", "TING", "#0"));
        tournamentList.add(new HostTournament("2", "Free Fire", "LW - duo", "50 GC", "85 GC", "0/24", "CANCEL", "UPDA", "TING", "#0"));
        adapter.notifyDataSetChanged();
    }

    private void filterTournaments(String status) {
        Toast.makeText(this, "Filtering: " + status, Toast.LENGTH_SHORT).show();
        // TODO: API call to fetch tournaments by status
    }

    private void showUpdateRoomDialog(HostTournament tournament) {
        Dialog dialog = new Dialog(this);
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
            String newRoomId = roomIdInput.getText().toString().trim();
            String newPassword = passwordInput.getText().toString().trim();

            if (newRoomId.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            tournament.setRoomId(newRoomId);
            tournament.setPassword(newPassword);
            adapter.notifyDataSetChanged();

            Toast.makeText(this, "Room info updated successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setLayout(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void showSubmitResultDialog(HostTournament tournament) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_submit_result);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText screenshotUrl = dialog.findViewById(R.id.screenshotUrlInput);
        TextView cancelBtn = dialog.findViewById(R.id.cancelSubmitBtn);
        TextView submitBtn = dialog.findViewById(R.id.submitAllBtn);

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        submitBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Results submitted successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setLayout(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void showEndTournamentDialog(HostTournament tournament) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_end_tournament_bg);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText reasonInput = dialog.findViewById(R.id.reasonInput);
        TextView cancelBtn = dialog.findViewById(R.id.cancelEndBtn);
        TextView endBtn = dialog.findViewById(R.id.endNowBtn);

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        endBtn.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("Are you absolutely sure you want to end this tournament?")
                    .setPositiveButton("Yes, End Now", (d, which) -> {
                        String reason = reasonInput.getText().toString().trim();
                        Toast.makeText(this, "Tournament ended successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadTournaments();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
        dialog.getWindow().setLayout(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}