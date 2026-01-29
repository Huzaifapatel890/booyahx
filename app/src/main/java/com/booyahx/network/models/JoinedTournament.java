package com.booyahx.network.models;

import android.util.Log;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class JoinedTournament implements Serializable {

    private static final String TAG = "SlotDebug";

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

    // ✅ THIS IS THE ACTUAL FIELD FROM API
    @SerializedName("joinedTeamsList")
    private List<JoinedTeamInfo> joinedTeamsList;

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
       ✅ SLOT NUMBER METHODS - USING ACTUAL API STRUCTURE
       ===================================================== */

    /**
     * Get user's slot/team number by matching userId (UID) with leaderUserId in joinedTeamsList
     * @param userId The user ID to match against team leaders
     * @return Team slot number (1-based index) or 0 if not found
     */
    public int getUserSlotNumberByUid(String userId) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "getUserSlotNumberByUid() called");
        Log.d(TAG, "Input userId: " + userId);
        Log.d(TAG, "Tournament ID: " + id);
        Log.d(TAG, "Lobby Name: " + lobbyName);

        if (joinedTeamsList == null) {
            Log.e(TAG, "❌ joinedTeamsList is NULL");
            return 0;
        }

        if (joinedTeamsList.isEmpty()) {
            Log.e(TAG, "❌ joinedTeamsList is EMPTY");
            return 0;
        }

        Log.d(TAG, "✅ joinedTeamsList size: " + joinedTeamsList.size());

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ Input userId is NULL or EMPTY");
            return 0;
        }

        for (int i = 0; i < joinedTeamsList.size(); i++) {
            JoinedTeamInfo team = joinedTeamsList.get(i);

            if (team == null) {
                Log.w(TAG, "⚠️ Team at index " + i + " is NULL");
                continue;
            }

            Log.d(TAG, "Checking team [" + i + "]:");
            Log.d(TAG, "  - teamName: " + team.teamName);
            Log.d(TAG, "  - leaderUserId: " + team.leaderUserId);
            Log.d(TAG, "  - playerCount: " + team.playerCount);

            if (userId.equals(team.leaderUserId)) {
                int slot = i + 1;
                Log.d(TAG, "✅✅✅ MATCH FOUND! Slot = " + slot);
                Log.d(TAG, "========================================");
                return slot; // SLOT FOUND (1-based index)
            } else {
                Log.d(TAG, "  ❌ No match ('" + userId + "' != '" + team.leaderUserId + "')");
            }
        }

        Log.e(TAG, "❌❌❌ NO MATCH FOUND after checking all teams");
        Log.d(TAG, "========================================");
        return 0; // NOT FOUND
    }

    /**
     * Get user's slot number by matching name or IGN (if participants exist)
     * @param myNameOrIgn The name or IGN to match
     * @return Slot number (1-based index) or 0 if not found
     */
    public int getUserSlotNumberByName(String myNameOrIgn) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "getUserSlotNumberByName() called");
        Log.d(TAG, "Input name/ign: " + myNameOrIgn);

        if (participants == null || participants.isEmpty() || myNameOrIgn == null) {
            Log.e(TAG, "❌ participants null/empty or input null");
            return 0;
        }

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            if (p == null) continue;

            Log.d(TAG, "Checking participant [" + i + "]: name=" + p.name + ", ign=" + p.ign);

            if (
                    (p.ign != null && p.ign.equalsIgnoreCase(myNameOrIgn)) ||
                            (p.name != null && p.name.equalsIgnoreCase(myNameOrIgn))
            ) {
                int slot = i + 1;
                Log.d(TAG, "✅ MATCH FOUND! Slot = " + slot);
                Log.d(TAG, "========================================");
                return slot; // SLOT FOUND
            }
        }

        Log.e(TAG, "❌ NO MATCH FOUND");
        Log.d(TAG, "========================================");
        return 0;
    }

    /**
     * Get team name for a given userId
     * @param userId The user ID to match
     * @return Team name or null if not found
     */
    public String getTeamNameByUid(String userId) {
        if (joinedTeamsList == null || userId == null || userId.isEmpty()) {
            return null;
        }

        for (JoinedTeamInfo team : joinedTeamsList) {
            if (team != null && userId.equals(team.leaderUserId)) {
                return team.teamName;
            }
        }
        return null;
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
    public List<Participant> getParticipants() { return participants; }
    public List<Team> getTeams() { return teams; }
    public List<JoinedTeamInfo> getJoinedTeamsList() { return joinedTeamsList; }

    /* ================= INNER MODELS ================= */

    // ✅ NEW: This is the actual structure from API
    public static class JoinedTeamInfo implements Serializable {
        @SerializedName("teamName")
        public String teamName;

        @SerializedName("leaderUserId")
        public String leaderUserId;

        @SerializedName("playerCount")
        public int playerCount;
    }

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