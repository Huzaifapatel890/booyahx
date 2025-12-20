package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Tournament {

    @SerializedName("_id")
    private String id;

    @SerializedName("game")
    private String game;

    @SerializedName("mode")
    private String mode;

    @SerializedName("subMode")
    private String subMode;

    @SerializedName("entryFee")
    private int entryFee;

    @SerializedName("maxPlayers")
    private int maxPlayers;

    @SerializedName("joinedCount")
    private int joinedCount;

    @SerializedName("prizePool")
    private int prizePool;

    @SerializedName("date")
    private String date;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("lobbyName")
    private String lobbyName;

    @SerializedName("rules")
    private TournamentRules rules;

    /* ================= DISPLAY HELPERS ================= */

    public String getTitle() {
        return lobbyName != null && !lobbyName.isEmpty()
                ? lobbyName
                : game + " " + subMode;
    }

    // 🔥 WEBSITE LOGIC
    public int getExpectedPP() {
        return prizePool;
    }

    public int getCurrentPP() {
        return joinedCount * entryFee;
    }

    /* ================= SLOT LOGIC ================= */

    private int getPlayersPerTeam() {
        if ("squad".equalsIgnoreCase(subMode)) return 4;
        if ("duo".equalsIgnoreCase(subMode)) return 2;
        return 1; // solo / 1v1
    }

    public int getTotalSlots() {
        return maxPlayers / getPlayersPerTeam();
    }

    public int getUsedSlots() {
        return joinedCount / getPlayersPerTeam();
    }

    public int getEntryFee() {
        return entryFee;
    }

    public String getDisplayMode() {
        return mode + " " + subMode;
    }

    public String getFormattedDateTime() {
        try {
            SimpleDateFormat api =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            SimpleDateFormat ui =
                    new SimpleDateFormat("dd MMM yyyy", Locale.US);

            Date d = api.parse(date);
            return ui.format(d) + " • " + startTime;
        } catch (Exception e) {
            return startTime;
        }
    }

    /* ================= MAP ROTATION ================= */

    public String getMapRotationShort() {
        if (rules == null || rules.mapRotation == null || rules.mapRotation.isEmpty()) {
            return null;
        }

        StringBuilder rotation = new StringBuilder();
        for (int i = 0; i < rules.mapRotation.size(); i++) {
            String map = rules.mapRotation.get(i);
            rotation.append(getMapInitial(map));
            if (i < rules.mapRotation.size() - 1) {
                rotation.append("/");
            }
        }
        return rotation.toString();
    }

    private String getMapInitial(String mapName) {
        if (mapName == null) return "";

        String lower = mapName.toLowerCase();
        if (lower.contains("bermuda")) return "B";
        if (lower.contains("purgatory")) return "P";
        if (lower.contains("alpine")) return "A";
        if (lower.contains("nexterra")) return "N";
        if (lower.contains("kalahari")) return "K";
        if (lower.contains("solarag")) return "S";

        return mapName.substring(0, 1).toUpperCase();
    }

    /* ================= INNER CLASS FOR RULES ================= */

    public static class TournamentRules {
        @SerializedName("mapRotation")
        public List<String> mapRotation;
    }
}