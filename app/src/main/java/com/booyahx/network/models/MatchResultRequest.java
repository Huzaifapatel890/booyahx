package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Request model for submitting individual match results
 * Endpoint: POST /api/tournament/submit-match-result
 */
public class MatchResultRequest {

    @SerializedName("tournamentId")
    private String tournamentId;

    @SerializedName("matchIndex")
    private int matchIndex;

    @SerializedName("teams")
    private List<TeamMatchData> teams;

    public MatchResultRequest(String tournamentId, int matchIndex, List<TeamMatchData> teams) {
        this.tournamentId = tournamentId;
        this.matchIndex = matchIndex;
        this.teams = teams;
    }

    // Getters and Setters
    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex = matchIndex;
    }

    public List<TeamMatchData> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamMatchData> teams) {
        this.teams = teams;
    }

    /**
     * Inner class for team data in a match
     */
    public static class TeamMatchData {

        @SerializedName("teamName")
        private String teamName;

        @SerializedName("booyah")
        private int booyah;  // 0 or 1

        @SerializedName("kills")
        private int kills;

        @SerializedName("position")
        private int position;

        @SerializedName("totalPoint")
        private int totalPoint;

        public TeamMatchData(String teamName, int booyah, int kills, int position, int totalPoint) {
            this.teamName = teamName;
            this.booyah = booyah;
            this.kills = kills;
            this.position = position;
            this.totalPoint = totalPoint;
        }

        // Getters and Setters
        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public int getBooyah() {
            return booyah;
        }

        public void setBooyah(int booyah) {
            this.booyah = booyah;
        }

        public int getKills() {
            return kills;
        }

        public void setKills(int kills) {
            this.kills = kills;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getTotalPoint() {
            return totalPoint;
        }

        public void setTotalPoint(int totalPoint) {
            this.totalPoint = totalPoint;
        }
    }
}