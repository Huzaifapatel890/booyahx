package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class JoinedTournament implements Serializable {

    @SerializedName("_id")
    private String id;

    private String game;
    private String mode;
    private String subMode;
    private int entryFee;
    private int maxPlayers;

    @SerializedName("date")
    private String rawDate; // ISO string from backend

    private String startTime;
    private String lockTime;

    @SerializedName("participants")
    private List<Object> participants;

    // ✅ FIX: hostId is NOT always a String
    @SerializedName("hostId")
    private Host host;

    private Room room;
    private int prizePool;
    private String status;

    // ✅ ADDED — rules from joined tournaments API
    @SerializedName("rules")
    private JoinedRules rules;

    /* ================= DERIVED / SAFE ================= */

    public int getParticipantCount() {
        return participants == null ? 0 : participants.size();
    }

    // 🔥 Adapter expects yyyy-MM-dd
    public String getDate() {
        if (rawDate == null) return "";
        return rawDate.split("T")[0];
    }

    /* ================= GETTERS ================= */

    public String getId() { return id; }
    public String getGame() { return game; }
    public String getMode() { return mode; }
    public String getSubMode() { return subMode; }
    public int getEntryFee() { return entryFee; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getStartTime() { return startTime; }
    public String getLockTime() { return lockTime; }

    // ✅ Backward-safe: keep same method name
    public String getHostId() {
        return host == null ? null : host.id;
    }

    public Host getHost() {
        return host;
    }

    public Room getRoom() { return room; }
    public int getPrizePool() { return prizePool; }
    public String getStatus() { return status; }

    // ✅ NEW — rules getter
    public JoinedRules getRules() { return rules; }

    /* ================= INNER MODELS ================= */

    public static class Host implements Serializable {
        @SerializedName("_id")
        private String id;
        private String name;
        private String ign;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getIgn() { return ign; }
    }

    public static class Room implements Serializable {
        private String roomId;
        private String password;

        public String getRoomId() { return roomId; }
        public String getPassword() { return password; }
    }

    /* ================= RULES MODEL (JOINED) ================= */

    public static class JoinedRules implements Serializable {

        public String title;
        public String description;

        @SerializedName("rules")
        public List<String> rules;

        public List<String> generalRules;
        public List<String> mapRotation;

        public int numberOfMatches;
        public int maxPlayers;
        public int minTeamsToStart;

        public int playersPerTeam;
        public int minPlayersPerTeam;
        public int maxPlayersPerTeam;
        public int maxTeams;

        public Map<String, Integer> positionPoints;
    }
}