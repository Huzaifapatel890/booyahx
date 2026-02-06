package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LiveResultResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public int getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        @SerializedName("tournamentId")
        private String tournamentId;

        @SerializedName("game")
        private String game;

        @SerializedName("mode")
        private String mode;

        @SerializedName("subMode")
        private String subMode;

        @SerializedName("date")
        private String date;

        @SerializedName("startTime")
        private String startTime;

        @SerializedName("status")
        private String status;

        @SerializedName("totalMatches")
        private int totalMatches;

        @SerializedName("matchResults")
        private List<MatchResult> matchResults;

        @SerializedName("standings")
        private List<Standing> standings;

        public String getTournamentId() {
            return tournamentId;
        }

        public String getGame() {
            return game;
        }

        public String getMode() {
            return mode;
        }

        public String getSubMode() {
            return subMode;
        }

        public String getDate() {
            return date;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getStatus() {
            return status;
        }

        public int getTotalMatches() {
            return totalMatches;
        }

        public List<MatchResult> getMatchResults() {
            return matchResults;
        }

        public List<Standing> getStandings() {
            return standings;
        }
    }

    public static class MatchResult {
        @SerializedName("matchIndex")
        private int matchIndex;

        @SerializedName("teams")
        private List<TeamResult> teams;

        public int getMatchIndex() {
            return matchIndex;
        }

        public List<TeamResult> getTeams() {
            return teams;
        }
    }

    public static class TeamResult {
        @SerializedName("teamName")
        private String teamName;

        @SerializedName("booyah")
        private int booyah;

        @SerializedName("kills")
        private int kills;

        @SerializedName("position")
        private int position;

        @SerializedName("totalPoint")
        private int totalPoint;

        public String getTeamName() {
            return teamName;
        }

        public int getBooyah() {
            return booyah;
        }

        public int getKills() {
            return kills;
        }

        public int getPosition() {
            return position;
        }

        public int getTotalPoint() {
            return totalPoint;
        }
    }

    public static class Standing {
        @SerializedName("teamName")
        private String teamName;

        @SerializedName("totalPoint")
        private int totalPoint;

        @SerializedName("kills")
        private int kills;

        @SerializedName("booyah")
        private int booyah;

        @SerializedName("position")
        private int position;

        public String getTeamName() {
            return teamName;
        }

        public int getTotalPoint() {
            return totalPoint;
        }

        public int getKills() {
            return kills;
        }

        public int getBooyah() {
            return booyah;
        }

        public int getPosition() {
            return position;
        }
    }
}