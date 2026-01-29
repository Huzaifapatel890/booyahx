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
    private String rawDate;

    private String startTime;
    private String lockTime;

    @SerializedName("participants")
    private List<Participant> participants;

    @SerializedName("teams")
    private List<Team> teams;

    @SerializedName("joinedTeams")
    private int joinedTeams;

    @SerializedName("maxTeams")
    private int maxTeams;

    @SerializedName("playersPerTeam")
    private int playersPerTeam;

    @SerializedName("hostId")
    private Host host;

    private Room room;
    private int prizePool;
    private String status;

    @SerializedName("lobbyName")
    private String lobbyName;

    @SerializedName("rules")
    private JoinedRules rules;

    /* ================= EXISTING METHODS ================= */

    public int getParticipantCount() {
        return participants == null ? 0 : participants.size();
    }

    public String getDate() {
        if (rawDate == null) return "";
        return rawDate.split("T")[0];
    }

    /* =====================================================
       ✅ NEW SIMPLE SLOT LOGIC (ONLY ADDITION)
       ===================================================== */

    public int getUserSlotNumberByName(String myNameOrIgn) {

        if (participants == null || participants.isEmpty() || myNameOrIgn == null)
            return 0;

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            if (p == null) continue;

            if (
                    (p.ign != null && p.ign.equalsIgnoreCase(myNameOrIgn)) ||
                            (p.name != null && p.name.equalsIgnoreCase(myNameOrIgn))
            ) {
                return i + 1; // SLOT FOUND
            }
        }

        return 0;
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

    public int getJoinedTeams() { return joinedTeams; }
    public int getMaxTeams() { return maxTeams; }
    public int getPlayersPerTeam() { return playersPerTeam; }

    public String getHostId() {
        return host == null ? null : host.id;
    }

    public Host getHost() { return host; }
    public Room getRoom() { return room; }
    public int getPrizePool() { return prizePool; }
    public String getStatus() { return status; }
    public String getLobbyName() { return lobbyName; }
    public JoinedRules getRules() { return rules; }

    /* ================= INNER MODELS ================= */

    public static class Participant implements Serializable {
        @SerializedName("userId")
        public String userId;

        @SerializedName("name")
        public String name;

        @SerializedName("ign")
        public String ign;
    }

    public static class Team implements Serializable {
        @SerializedName("_id")
        public String id;

        @SerializedName("leaderUserId")
        public String leaderUserId;

        @SerializedName("teamName")
        public String teamName;

        @SerializedName("members")
        public List<String> members;
    }

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