package com.booyahx.Host;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.booyahx.R;
import java.util.ArrayList;
import java.util.List;
import com.booyahx.Host.HostPointsHelper;
import com.booyahx.network.models.HostMatchResult;
public class HostSubmitResultDialog {

    private Context context;
    private Dialog dialog;
    private HostPointsHelper pointsHelper;
    private String tournamentId;
    private int totalMatches;
    private List<String> teamNames;
    private int currentMatchNumber = 1;
    private LinearLayout teamsContainer;
    private TextView submitFinalBtn;
    private List<TeamInputView> teamInputViews;

    public HostSubmitResultDialog(Context context, String tournamentId,
                                  int totalMatches, List<String> teamNames) {
        this.context = context;
        this.tournamentId = tournamentId;
        this.totalMatches = totalMatches;
        this.teamNames = teamNames;
        this.pointsHelper = new HostPointsHelper(context);
        this.teamInputViews = new ArrayList<>();
    }

    public void show() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_submit_result);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        initializeViews();
        setupMatchSelector();
        createTeamInputs();
        setupButtons();

        dialog.show();
    }

    private void initializeViews() {
        teamsContainer = dialog.findViewById(R.id.teamsContainer);
        submitFinalBtn = dialog.findViewById(R.id.submitFinalBtn);
    }

    private void setupMatchSelector() {
        Spinner matchSelector = dialog.findViewById(R.id.matchSelector);
        List<String> matches = new ArrayList<>();
        for (int i = 1; i <= totalMatches; i++) {
            matches.add("Match " + i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_item, matches) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(Color.WHITE);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(Color.WHITE);
                tv.setBackgroundColor(Color.parseColor("#111111"));
                tv.setPadding(20, 20, 20, 20);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        matchSelector.setAdapter(adapter);

        matchSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMatchNumber = position + 1;
                loadMatchData(currentMatchNumber);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void createTeamInputs() {
        teamsContainer.removeAllViews();
        teamInputViews.clear();

        for (String teamName : teamNames) {
            TeamInputView teamView = new TeamInputView(context, teamName);
            teamsContainer.addView(teamView.getView());
            teamInputViews.add(teamView);
        }

        loadMatchData(currentMatchNumber);
    }

    private void loadMatchData(int matchNumber) {
        List<HostMatchResult> savedResults = pointsHelper.getMatchResults(tournamentId, matchNumber);

        for (TeamInputView teamView : teamInputViews) {
            teamView.reset();
            for (HostMatchResult result : savedResults) {
                if (result.getTeamName().equals(teamView.getTeamName())) {
                    teamView.setKillPoints(result.getKillPoints());
                    teamView.setPosition(result.getPositionPoints());
                    break;
                }
            }
        }

        checkAllMatchesCompleted();
    }

    private void setupButtons() {
        TextView cancelBtn = dialog.findViewById(R.id.cancelBtn);
        TextView saveMatchBtn = dialog.findViewById(R.id.saveMatchBtn);

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        saveMatchBtn.setOnClickListener(v -> {
            saveCurrentMatch();
            Toast.makeText(context, "Match " + currentMatchNumber + " saved!", Toast.LENGTH_SHORT).show();
            checkAllMatchesCompleted();
        });

        submitFinalBtn.setOnClickListener(v -> {
            submitFinalPoints();
        });
    }

    private void saveCurrentMatch() {
        List<HostMatchResult> results = new ArrayList<>();

        for (TeamInputView teamView : teamInputViews) {
            results.add(teamView.getResult());
        }

        pointsHelper.saveMatchResults(tournamentId, currentMatchNumber, results);
    }

    private void checkAllMatchesCompleted() {
        boolean allCompleted = true;
        for (int i = 1; i <= totalMatches; i++) {
            if (pointsHelper.getMatchResults(tournamentId, i).isEmpty()) {
                allCompleted = false;
                break;
            }
        }

        submitFinalBtn.setVisibility(allCompleted ? View.VISIBLE : View.GONE);
    }

    private void submitFinalPoints() {
        // Calculate final points
        List<HostMatchResult> finalPoints = pointsHelper.calculateFinalPoints(tournamentId, totalMatches);
        pointsHelper.saveFinalPoints(tournamentId, finalPoints);

        // TODO: Upload to backend API
        // POST /api/host/tournaments/{tournamentId}/final-points
        // Body: finalPoints list

        Toast.makeText(context, "Final points table submitted!", Toast.LENGTH_LONG).show();
        dialog.dismiss();
    }

    // Inner class for team input view
    private class TeamInputView {
        private View view;
        private TextView teamNameTv;
        private EditText killPointsInput;
        private Spinner positionSpinner;
        private TextView totalPointsTv;
        private String teamName;

        public TeamInputView(Context context, String teamName) {
            this.teamName = teamName;
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.item_host_points_input, null);

            teamNameTv = view.findViewById(R.id.teamName);
            killPointsInput = view.findViewById(R.id.killPointsInput);
            positionSpinner = view.findViewById(R.id.positionSpinner);
            totalPointsTv = view.findViewById(R.id.totalPoints);

            teamNameTv.setText(teamName);
            setupPositionSpinner();
            setupListeners();
        }

        private void setupPositionSpinner() {
            List<String> positions = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                positions.add("#" + i);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, positions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(adapter);
        }

        private void setupListeners() {
            killPointsInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateTotal();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            positionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateTotal();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        private void updateTotal() {
            int kp = getKillPoints();
            int pp = HostPointsHelper.getPositionPoints(positionSpinner.getSelectedItemPosition() + 1);
            totalPointsTv.setText(String.valueOf(kp + pp));
        }

        public void reset() {
            killPointsInput.setText("");
            positionSpinner.setSelection(0);
            totalPointsTv.setText("0");
        }

        public void setKillPoints(int kp) {
            killPointsInput.setText(String.valueOf(kp));
        }

        public void setPosition(int pp) {
            // Find position by points
            for (int i = 1; i <= 12; i++) {
                if (HostPointsHelper.getPositionPoints(i) == pp) {
                    positionSpinner.setSelection(i - 1);
                    break;
                }
            }
        }

        public int getKillPoints() {
            String text = killPointsInput.getText().toString();
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        }

        public HostMatchResult getResult() {
            int kp = getKillPoints();
            int pp = HostPointsHelper.getPositionPoints(positionSpinner.getSelectedItemPosition() + 1);
            return new HostMatchResult(teamName, kp, pp);
        }

        public String getTeamName() {
            return teamName;
        }

        public View getView() {
            return view;
        }
    }
}
