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
import com.booyahx.network.ApiService;
import com.booyahx.network.models.FinalResultRequest;
import com.booyahx.network.models.FinalResultResponse;
import com.booyahx.network.models.MatchResultRequest;
import com.booyahx.network.models.MatchResultResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HostSubmitResultDialog extends Dialog {

    private static final String PREFS = "host_match_storage";
    private static final long EXPIRY_MS = 60L * 24 * 60 * 60 * 1000;

    private final Context context;
    private final int totalMatches;
    private final List<String> teamsList;
    private final String tournamentId;
    private final ApiService api;

    /**
     * [0] KP (Kill Points)
     * [1] POSITION
     * [2] PP (Position Points)
     * [3] TOTAL
     * [4] BOOYAH
     */
    private final List<List<int[]>> matchScores = new ArrayList<>();
    private final boolean[] matchSaved;

    private int currentMatch = 0;
    private boolean internalChange = false;
    private boolean finalCalculationDone = false;

    private Spinner matchSelector;
    private LinearLayout teamsContainer;
    private TextView saveMatchBtn, submitFinalBtn, cancelBtn;
    private TextView matchStatusIndicator;

    public HostSubmitResultDialog(
            @NonNull Context context,
            String tournamentId,
            int totalMatches,
            List<String> teams,
            ApiService api
    ) {
        super(context);
        this.context = context;
        this.tournamentId = tournamentId;
        this.totalMatches = totalMatches;
        this.teamsList = teams;
        this.api = api;

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
        matchStatusIndicator = findViewById(R.id.matchStatusIndicator);

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

            // HARD BLOCK QUICK FILL / AUTOFILL / SUGGESTIONS
            killInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            killInput.setAutofillHints(new String[]{});
            killInput.setLongClickable(false);
            killInput.setTextIsSelectable(false);
            killInput.setSaveEnabled(false);
            killInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

            teamName.setText((i + 1) + ". " + teamsList.get(i));

            List<String> posList = new ArrayList<>();
            posList.add("Select");
            for (int p = 1; p <= teamsList.size(); p++) {
                posList.add(String.valueOf(p));
            }

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
                            int parsed = Integer.parseInt(val);
                            if (parsed > 100) parsed = 100;
                            data[0] = parsed;
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

    // ⭐ MODIFIED: Calls API after saving, calculates on Match 6
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

        // Disable button during API call
        saveMatchBtn.setEnabled(false);
        saveMatchBtn.setText("Submitting to server...");

        // ⭐ Call API to submit this match result
        submitMatchToAPI(currentMatch);
    }

    // ⭐ NEW: API call for submitting individual match
    private void submitMatchToAPI(int matchIndex) {
        // Prepare teams data for API
        List<MatchResultRequest.TeamMatchData> teamsData = new ArrayList<>();

        List<int[]> matchData = matchScores.get(matchIndex);
        for (int i = 0; i < teamsList.size(); i++) {
            int[] teamScores = matchData.get(i);

            MatchResultRequest.TeamMatchData teamData = new MatchResultRequest.TeamMatchData(
                    teamsList.get(i),    // teamName
                    teamScores[4],       // booyah (0 or 1)
                    teamScores[0],       // kills
                    teamScores[1],       // position
                    teamScores[3]        // totalPoint
            );
            teamsData.add(teamData);
        }

        // Create request
        MatchResultRequest request = new MatchResultRequest(
                tournamentId,
                matchIndex,
                teamsData
        );

        // Make API call
        api.submitMatchResult(request).enqueue(new Callback<MatchResultResponse>() {
            @Override
            public void onResponse(Call<MatchResultResponse> call, Response<MatchResultResponse> response) {
                saveMatchBtn.setEnabled(true);
                saveMatchBtn.setText("Save Match " + (matchIndex + 1));

                if (response.isSuccessful() && response.body() != null) {
                    MatchResultResponse apiResponse = response.body();

                    Toast.makeText(context,
                            "Match " + (matchIndex + 1) + " submitted successfully!",
                            Toast.LENGTH_SHORT).show();

                    // ✅ CHECK IF THIS IS MATCH 6 (matchIndex == 5)
                    if (matchIndex == 5) {
                        // When match 6 is submitted, instantly open EnhancedFinalResultDialog
                        // The dialog will fetch live results via API

                        EnhancedFinalResultDialog enhancedDialog = new EnhancedFinalResultDialog(
                                context,
                                new ArrayList<>(), // Empty list, will be populated by API
                                tournamentId,
                                "finished" // Match 6 means tournament is finished
                        );

                        // ✅ SET CALLBACK - When host clicks close, reopen this HostSubmitResultDialog
                        enhancedDialog.setOnHostCloseListener(new EnhancedFinalResultDialog.OnHostCloseListener() {
                            @Override
                            public void onHostClose() {
                                // Reopen HostSubmitResultDialog
                                HostSubmitResultDialog.this.show();
                            }
                        });

                        dismiss(); // Close submit dialog first
                        enhancedDialog.show(); // Show enhanced dialog

                        return; // Exit early, don't do anything else
                    }

                    // ⭐ If this is match 6 (last match), calculate final points
                    if (matchIndex == totalMatches - 1 && !finalCalculationDone) {
                        calculateAndSaveFinalPoints();
                    }

                    setupMatchSelector();
                    matchSelector.setSelection(matchIndex);
                    updateUIState();

                    // Auto-navigate to next unsaved match
                    if (matchIndex < totalMatches - 1) {
                        for (int i = matchIndex + 1; i < totalMatches; i++) {
                            if (!matchSaved[i]) {
                                matchSelector.setSelection(i);
                                return;
                            }
                        }
                    }
                } else {
                    // API call failed
                    saveMatchBtn.setText("Save Match " + (matchIndex + 1));
                    matchSaved[matchIndex] = false;
                    saveToStorage();

                    Toast.makeText(context,
                            "Failed to submit match " + (matchIndex + 1) + ". Please try again.",
                            Toast.LENGTH_LONG).show();

                    updateUIState();
                }
            }

            @Override
            public void onFailure(Call<MatchResultResponse> call, Throwable t) {
                saveMatchBtn.setEnabled(true);
                saveMatchBtn.setText("Save Match " + (matchIndex + 1));
                matchSaved[matchIndex] = false;
                saveToStorage();

                Toast.makeText(context,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();

                updateUIState();
            }
        });
    }

    // ⭐ Calculate and save final points (happens on Match 6 save)
    private void calculateAndSaveFinalPoints() {
        try {
            // Calculate final standings
            List<FinalRow> finalTable =
                    FinalResultCalculator.calculate(matchScores, teamsList);

            // Save to SharedPrefs
            FinalResultStore.save(context, tournamentId, finalTable);

            finalCalculationDone = true;

            Toast.makeText(context,
                    "Final points calculated successfully!",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(context,
                    "Error calculating points: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
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

        // Show submit final button only when all matches saved
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

        // ⭐ Submit final result button - just calls API
        submitFinalBtn.setOnClickListener(v -> {
            // Validate all matches saved
            for (int m = 0; m < totalMatches; m++) {
                if (!matchSaved[m]) {
                    Toast.makeText(context,
                            "Please save all matches before submitting final result!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }



            // Disable button during API call
            submitFinalBtn.setEnabled(false);
            submitFinalBtn.setText("Submitting Final Result...");

            // ⭐ Call API to submit final result
            submitFinalResultToAPI();
        });

        cancelBtn.setOnClickListener(v -> dismiss());
    }

    // ⭐ NEW: API call for submitting final result
    private void submitFinalResultToAPI() {
        FinalResultRequest request = new FinalResultRequest(tournamentId);

        api.submitFinalResult(request).enqueue(new Callback<FinalResultResponse>() {
            @Override
            public void onResponse(Call<FinalResultResponse> call, Response<FinalResultResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FinalResultResponse apiResponse = response.body();

                    Toast.makeText(context,
                            "Final result submitted successfully!",
                            Toast.LENGTH_LONG).show();

                    // ⭐ Close dialog on success
                    dismiss();
                } else {
                    // API call failed
                    submitFinalBtn.setEnabled(true);
                    submitFinalBtn.setText("Submit Final Result");

                    Toast.makeText(context,
                            "Failed to submit final result. Please try again.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<FinalResultResponse> call, Throwable t) {
                submitFinalBtn.setEnabled(true);
                submitFinalBtn.setText("Submit Final Result");

                Toast.makeText(context,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}