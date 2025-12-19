package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
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
}