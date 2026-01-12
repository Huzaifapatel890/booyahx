package com.booyahx.Host;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.booyahx.network.models.HostMatchResult;
public class HostPointsHelper {
    private static final String PREFS_NAME = "HostTournamentPoints";
    private SharedPreferences prefs;
    private Gson gson;

    public HostPointsHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Save match results for a specific tournament and match number
    public void saveMatchResults(String tournamentId, int matchNumber, List<HostMatchResult> results) {
        String key = tournamentId + "match" + matchNumber;
        String json = gson.toJson(results);
        prefs.edit().putString(key, json).apply();
    }

    // Get match results
    public List<HostMatchResult> getMatchResults(String tournamentId, int matchNumber) {
        String key = tournamentId + "match" + matchNumber;
        String json = prefs.getString(key, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<HostMatchResult>>(){}.getType();
        return gson.fromJson(json, type);
    }

    // Calculate final points across all matches
    public List<HostMatchResult> calculateFinalPoints(String tournamentId, int totalMatches) {
        Map<String, HostMatchResult> finalResults = new HashMap<>();

        for (int i = 1; i <= totalMatches; i++) {
            List<HostMatchResult> matchResults = getMatchResults(tournamentId, i);
            for (HostMatchResult result : matchResults) {
                if (finalResults.containsKey(result.getTeamName())) {
                    HostMatchResult existing = finalResults.get(result.getTeamName());
                    existing.setKillPoints(existing.getKillPoints() + result.getKillPoints());
                    existing.setPositionPoints(existing.getPositionPoints() + result.getPositionPoints());
                } else {
                    finalResults.put(result.getTeamName(),
                            new HostMatchResult(result.getTeamName(),
                                    result.getKillPoints(),
                                    result.getPositionPoints()));
                }
            }
        }

        return new ArrayList<>(finalResults.values());
    }

    // Save final points table
    public void saveFinalPoints(String tournamentId, List<HostMatchResult> finalResults) {
        String key = tournamentId + "_final";
        String json = gson.toJson(finalResults);
        prefs.edit().putString(key, json).apply();
    }

    // Get final points
    public List<HostMatchResult> getFinalPoints(String tournamentId) {
        String key = tournamentId + "_final";
        String json = prefs.getString(key, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<HostMatchResult>>(){}.getType();
        return gson.fromJson(json, type);
    }

    // Clear tournament data
    public void clearTournamentData(String tournamentId) {
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allEntries = prefs.getAll();
        for (String key : allEntries.keySet()) {
            if (key.startsWith(tournamentId)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    // Get position points based on rank (fixed points table)
    public static int getPositionPoints(int position) {
        switch (position) {
            case 1: return 10;
            case 2: return 6;
            case 3: return 5;
            case 4: return 4;
            case 5: return 3;
            case 6: return 2;
            case 7: return 1;
            case 8: return 1;
            default: return 0;
        }
    }
}