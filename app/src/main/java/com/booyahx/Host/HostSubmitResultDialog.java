package com.booyahx.Host;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.booyahx.R;

import java.util.ArrayList;
import java.util.List;

public class HostSubmitResultDialog extends Dialog {

    private static final String PREFS = "host_match_storage";
    private static final long EXPIRY_MS = 60L * 24 * 60 * 60 * 1000;

    private final Context context;
    private final int totalMatches;
    private final List<String> teamsList;
    private final String tournamentId;

    /**
     * [0] KP
     * [1] POSITION
     * [2] PP
     * [3] TOTAL
     * [4] BOOYAH
     */
    private final List<List<int[]>> matchScores = new ArrayList<>();
    private final boolean[] matchSaved;

    private int currentMatch = 0;
    private boolean internalChange = false;

    private Spinner matchSelector;
    private LinearLayout teamsContainer;
    private TextView saveMatchBtn, submitFinalBtn, cancelBtn;
    private TextView matchStatusIndicator;

    public HostSubmitResultDialog(
            @NonNull Context context,
            String tournamentId,
            int totalMatches,
            List<String> teams
    ) {
        super(context);
        this.context = context;
        this.tournamentId = tournamentId;
        this.totalMatches = totalMatches;
        this.teamsList = teams;

        matchSaved = new boolean[totalMatches];

        for (int m = 0; m < totalMatches; m++) {
            List<int[]> match = new ArrayList<>();
            for (int t = 0; t < teams.size(); t++) {
                match.add(new int[]{0, 0, 0, 0, 0});
            }
            matchScores.add(match);
        }

        loadFromStorage();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_submit_result);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        matchSelector = findViewById(R.id.matchSelector);
        teamsContainer = findViewById(R.id.teamsContainer);
        saveMatchBtn = findViewById(R.id.saveMatchBtn);
        submitFinalBtn = findViewById(R.id.submitFinalBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        matchStatusIndicator = findViewById(R.id.matchStatusIndicator); // FIX ADDED

        setupMatchSelector();
        loadTeamsUI();
        setupButtons();
        updateUIState();
    }

    private void setupMatchSelector() {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= totalMatches; i++) {
            list.add("Match " + i + (matchSaved[i - 1] ? " ✓" : ""));
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        matchSelector.setAdapter(adapter);
        matchSelector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                currentMatch = pos;
                loadTeamsUI();
                updateUIState();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadTeamsUI() {
        teamsContainer.removeAllViews();
        List<int[]> scores = matchScores.get(currentMatch);

        for (int i = 0; i < teamsList.size(); i++) {

            View row = LayoutInflater.from(context)
                    .inflate(R.layout.item_host_points_input, teamsContainer, false);

            TextView teamName = row.findViewById(R.id.teamName);
            EditText killInput = row.findViewById(R.id.killPointsInput);
            Spinner posSpinner = row.findViewById(R.id.positionSpinner);
            TextView totalView = row.findViewById(R.id.totalPoints);

            teamName.setText((i + 1) + ". " + teamsList.get(i));

            killInput.setHint("0");
            killInput.setHintTextColor(Color.parseColor("#777777"));

            List<String> posList = new ArrayList<>();
            posList.add("Select");
            for (int p = 1; p <= teamsList.size(); p++) posList.add(String.valueOf(p));

            ArrayAdapter<String> spinnerAdapter =
                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, posList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            posSpinner.setAdapter(spinnerAdapter);

            int[] data = scores.get(i);

            killInput.setText(data[0] == 0 ? "" : String.valueOf(data[0]));
            posSpinner.setSelection(data[1]);
            totalView.setText(String.valueOf(data[3]));

            int teamIndex = i;

            killInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        String val = s.toString().trim();
                        if (val.isEmpty()) {
                            data[0] = 0;
                        } else {
                            long parsed = Long.parseLong(val);
                            if (parsed > 100) parsed = 100;
                            data[0] = (int) parsed;
                        }
                    } catch (Exception e) {
                        data[0] = 0;
                    }
                    calculateTotals();
                    updateUIState();
                }
            });

            posSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                    if (internalChange) return;

                    if (pos == 0) {
                        data[1] = 0;
                        calculateTotals();
                        updateUIState();
                        return;
                    }

                    if (isDuplicatePosition(pos, teamIndex)) {
                        internalChange = true;
                        Toast.makeText(context,
                                "Position already used in this match!",
                                Toast.LENGTH_SHORT).show();
                        posSpinner.setSelection(0);
                        internalChange = false;
                        return;
                    }

                    data[1] = pos;
                    calculateTotals();
                    updateUIState();
                }

                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            teamsContainer.addView(row);
        }

        calculateTotals();
    }

    private void saveCurrentMatch() {
        for (int[] row : matchScores.get(currentMatch)) {
            if (row[1] == 0) {
                Toast.makeText(context,
                        "Please select all positions before saving!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        matchSaved[currentMatch] = true;
        saveToStorage();

        Toast.makeText(context,
                "Match " + (currentMatch + 1) + " saved!",
                Toast.LENGTH_SHORT).show();

        setupMatchSelector();
        matchSelector.setSelection(currentMatch);

        updateUIState();

        if (currentMatch < totalMatches - 1) {
            for (int i = currentMatch + 1; i < totalMatches; i++) {
                if (!matchSaved[i]) {
                    matchSelector.setSelection(i);
                    return;
                }
            }
        }
    }

    private void updateUIState() {
        int savedCount = 0;
        for (boolean saved : matchSaved) {
            if (saved) savedCount++;
        }

        if (matchStatusIndicator != null) {
            matchStatusIndicator.setText("Matches Saved: " + savedCount + "/" + totalMatches);
            if (savedCount == totalMatches) {
                matchStatusIndicator.setTextColor(Color.parseColor("#00FF00"));
            } else {
                matchStatusIndicator.setTextColor(Color.parseColor("#FFFFFF"));
            }
        }

        // FIXED VISIBILITY LOGIC
        if (savedCount == totalMatches) {
            submitFinalBtn.setVisibility(View.VISIBLE);
            submitFinalBtn.setEnabled(true);
            submitFinalBtn.setAlpha(1f);
        } else {
            submitFinalBtn.setVisibility(View.GONE);
            submitFinalBtn.setEnabled(false);
            submitFinalBtn.setAlpha(0.5f);
        }

        if (matchSaved[currentMatch]) {
            saveMatchBtn.setText("Match " + (currentMatch + 1) + " Saved ✓");
            saveMatchBtn.setBackgroundResource(R.drawable.neon_green);
        } else {
            saveMatchBtn.setText("Save Match " + (currentMatch + 1));
            saveMatchBtn.setBackgroundResource(R.drawable.neon_green);
        }
    }

    private void saveToStorage() {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        long now = System.currentTimeMillis();

        for (int m = 0; m < totalMatches; m++) {
            for (int t = 0; t < teamsList.size(); t++) {
                int[] d = matchScores.get(m).get(t);
                String key = tournamentId + m + t;
                ed.putString(key,
                        d[0] + "," + d[1] + "," + d[2] + "," + d[3] + "," + d[4] + "," + now);
            }
        }

        for (int m = 0; m < totalMatches; m++) {
            ed.putBoolean(tournamentId + "match_saved" + m, matchSaved[m]);
        }

        ed.apply();
    }

    private void loadFromStorage() {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();

        for (int m = 0; m < totalMatches; m++) {
            for (int t = 0; t < teamsList.size(); t++) {

                String key = tournamentId + m + t;
                String val = sp.getString(key, null);
                if (val == null) continue;

                String[] p = val.split(",");

                try {
                    int kp = Integer.parseInt(p[0]);
                    int pos = Integer.parseInt(p[1]);
                    int pp = Integer.parseInt(p[2]);
                    int total = Integer.parseInt(p[3]);
                    int booyah = Integer.parseInt(p[4]);
                    long ts = Long.parseLong(p[5]);

                    if (now - ts > EXPIRY_MS) continue;

                    matchScores.get(m).set(t, new int[]{
                            kp, pos, pp, total, booyah
                    });

                } catch (Exception ignored) {}
            }

            matchSaved[m] = sp.getBoolean(tournamentId + "match_saved" + m, false);
        }
    }

    private boolean isDuplicatePosition(int pos, int currentTeam) {
        for (int i = 0; i < matchScores.get(currentMatch).size(); i++) {
            if (i == currentTeam) continue;
            if (matchScores.get(currentMatch).get(i)[1] == pos) return true;
        }
        return false;
    }

    private void calculateTotals() {
        for (int i = 0; i < teamsContainer.getChildCount(); i++) {
            TextView totalView = teamsContainer.getChildAt(i).findViewById(R.id.totalPoints);
            int[] data = matchScores.get(currentMatch).get(i);

            data[2] = getPositionPoints(data[1]);
            data[4] = (data[1] == 1) ? 1 : 0;
            data[3] = data[0] + data[2];

            totalView.setText(String.valueOf(data[3]));
        }
    }

    private int getPositionPoints(int pos) {
        switch (pos) {
            case 1: return 12;
            case 2: return 9;
            case 3: return 8;
            case 4: return 7;
            case 5: return 6;
            case 6: return 5;
            case 7: return 4;
            case 8: return 3;
            case 9: return 2;
            case 10:
            case 11:
            case 12: return 1;
            default: return 0;
        }
    }

    private void setupButtons() {
        saveMatchBtn.setOnClickListener(v -> saveCurrentMatch());

        submitFinalBtn.setOnClickListener(v -> {
            for (int m = 0; m < totalMatches; m++) {
                if (!matchSaved[m]) {
                    Toast.makeText(context,
                            "Please save all matches before submitting final result!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            List<FinalRow> finalTable =
                    FinalResultCalculator.calculate(matchScores, teamsList);

            FinalResultStore.save(context, tournamentId, finalTable);

            Toast.makeText(context,
                    "Final result calculated & saved successfully!",
                    Toast.LENGTH_LONG).show();

            dismiss();
        });

        cancelBtn.setOnClickListener(v -> dismiss());
    }
}