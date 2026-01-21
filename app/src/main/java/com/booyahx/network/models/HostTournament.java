package com.booyahx.network.models;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HostTournament {

    // =====================
    // CORE FIELDS (API)
    // =====================

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

    @SerializedName("date")
    private String date;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("lockTime")
    private String lockTime;

    @SerializedName("participants")
    private List<Participant> participants;

    @SerializedName("hostId")
    private Object hostId;

    @SerializedName("room")
    private Room room;

    @SerializedName("prizePool")
    private int prizePool;

    @SerializedName("status")
    private String status;

    @SerializedName("results")
    private List<Result> results;

    @SerializedName("teams")
    private List<Team> teams;

    // ✅ NEW: Rules object from API
    @SerializedName("rules")
    private JsonObject rules;

    // =====================
    // INNER MODELS
    // =====================

    public static class Room {
        @SerializedName("roomId")
        private String roomId;

        @SerializedName("password")
        private String password;

        public String getRoomId() { return roomId; }
        public String getPassword() { return password; }

        public void setRoomId(String roomId) { this.roomId = roomId; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Team {
        @SerializedName("_id")
        private String id;

        @SerializedName("leaderUserId")
        private String leaderUserId;

        @SerializedName("teamName")
        private String teamName;

        @SerializedName("players")
        private List<Player> players;

        public String getId() { return id; }
        public String getLeaderUserId() { return leaderUserId; }
        public String getTeamName() { return teamName; }
        public List<Player> getPlayers() { return players; }

        public static class Player {
            @SerializedName("name")
            private String name;

            @SerializedName("_id")
            private String id;

            public String getName() { return name; }
            public String getId() { return id; }
        }
    }

    public static class Participant {

        @SerializedName("_id")
        private String id;

        @SerializedName("teamName")
        private String teamName;

        @SerializedName("name")
        private String name;

        @SerializedName("ign")
        private String ign;

        public String getDisplayName() {
            if (teamName != null && !teamName.trim().isEmpty()) {
                return teamName.trim();
            }
            if (ign != null && !ign.trim().isEmpty()) {
                return ign.trim();
            }
            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
            return "Team";
        }

        public String getTeamName() { return teamName; }
        public String getName() { return name; }
        public String getIgn() { return ign; }
        public String getId() { return id; }
    }

    public static class Result {
        @SerializedName("userId")
        private String userId;

        @SerializedName("position")
        private int position;

        @SerializedName("kills")
        private int kills;

        @SerializedName("rewardGC")
        private int rewardGC;

        @SerializedName("claimed")
        private boolean claimed;

        public String getUserId() { return userId; }
        public int getPosition() { return position; }
        public int getKills() { return kills; }
        public int getRewardGC() { return rewardGC; }
        public boolean isClaimed() { return claimed; }
    }

    // =====================
    // BASIC GETTERS
    // =====================

    public String getId() { return id; }
    public String getGame() { return game; }
    public String getMode() { return mode; }
    public String getSubMode() { return subMode; }
    public int getEntryFee() { return entryFee; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getLockTime() { return lockTime; }
    public List<Participant> getParticipants() { return participants; }
    public Room getRoom() { return room; }
    public int getPrizePool() { return prizePool; }
    public String getStatus() { return status; }
    public List<Result> getResults() { return results; }
    public List<Team> getTeams() { return teams; }
    public JsonObject getRules() { return rules; } // ✅ NEW GETTER

    // =====================
    // UI HELPERS
    // =====================

    public String getHeaderTitle() {
        String lobbyDisplay = "Lobby";
        String timeDisplay = startTime != null ? startTime : "--";
        return lobbyDisplay + " • " + timeDisplay;
    }

    public String getModeDisplay() {
        return (mode != null ? mode : "-") + " - " +
                (subMode != null ? subMode : "-");
    }

    public String getEntryFeeDisplay() {
        return entryFee + " GC";
    }

    public String getPrizePoolDisplay() {
        return prizePool + " GC";
    }

    public String getSlotsDisplay() {
        int joined = 0;
        int totalSlots = maxPlayers;

        // Calculate joined teams/players
        if (teams != null && !teams.isEmpty()) {
            joined = teams.size();
        } else if (participants != null) {
            joined = participants.size();
        }

        // Get total slots from rules if available
        if (rules != null && rules.has("maxTeams")) {
            totalSlots = rules.get("maxTeams").getAsInt();
        } else {
            // Fallback calculation based on subMode
            if (subMode != null) {
                String mode = subMode.toLowerCase();

                if (mode.contains("squad")) {
                    totalSlots = maxPlayers / 4;
                } else if (mode.contains("duo")) {
                    totalSlots = maxPlayers / 2;
                } else {
                    totalSlots = maxPlayers; // solo or unknown
                }
            }
        }

        return joined + "/" + totalSlots;
    }
    public String getRoomIdDisplay() {
        return room != null && room.getRoomId() != null
                ? room.getRoomId()
                : "N/A";
    }

    public String getPasswordDisplay() {
        return room != null && room.getPassword() != null
                ? room.getPassword()
                : "N/A";
    }

    // =====================
    // TIME HELPERS
    // =====================

    public long getStartTimeMillis() {
        if (lockTime == null || lockTime.isEmpty()) return 0;

        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = sdf.parse(lockTime);
            return date != null ? date.getTime() : 0;

        } catch (Exception e) {
            return 0;
        }
    }

    public String getTimeDisplay() {
        if ("upcoming".equals(status)) {
            long startMillis = getStartTimeMillis();
            long diff = startMillis - System.currentTimeMillis();

            if (diff > 0) {
                long hours = diff / (1000 * 60 * 60);
                long mins = (diff % (1000 * 60 * 60)) / (1000 * 60);
                return String.format(Locale.getDefault(), "%02d:%02d", hours, mins);
            }
            return "Starting Soon";
        }

        if ("live".equals(status)) return "LIVE NOW";
        if ("completed".equals(status)) return "COMPLETED";
        if ("resultPending".equals(status)) return "RESULT PENDING";

        return "UPCOMING";
    }
}