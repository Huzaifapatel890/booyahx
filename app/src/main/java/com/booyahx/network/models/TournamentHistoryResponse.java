package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TournamentHistoryResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Data getData() { return data; }

    public static class Data {

        @SerializedName("history")
        private List<TournamentHistory> history;

        @SerializedName("total")
        private int total;

        @SerializedName("limit")
        private int limit;

        @SerializedName("offset")
        private int offset;

        @SerializedName("fromDate")
        private String fromDate;

        @SerializedName("toDate")
        private String toDate;

        public List<TournamentHistory> getHistory() { return history; }
        public int getTotal() { return total; }
        public int getLimit() { return limit; }
        public int getOffset() { return offset; }
    }

    public static class TournamentHistory {

        @SerializedName("_id")
        private String id;

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

        @SerializedName("lobbyName")
        private String lobbyName;

        @SerializedName("entryFee")
        private int entryFee;

        @SerializedName("prizePool")
        private int prizePool;

        @SerializedName("joinedTeams")
        private int joinedTeams;

        @SerializedName("myResult")
        private MyResult myResult;

        @SerializedName("myTeam")
        private String myTeam;

        // 🔥 ADDED: Full standings array — key confirmed from API logs as "standings"
        @SerializedName("standings")
        private List<Standing> standings;

        public String getId() { return id; }
        public String getGame() { return game; }
        public String getMode() { return mode; }
        public String getSubMode() { return subMode; }
        public String getDate() { return date; }
        public String getStartTime() { return startTime; }
        public String getStatus() { return status; }
        public String getLobbyName() { return lobbyName; }
        public int getEntryFee() { return entryFee; }
        public int getPrizePool() { return prizePool; }
        public int getJoinedTeams() { return joinedTeams; }
        public MyResult getMyResult() { return myResult; }
        public String getMyTeam() { return myTeam; }
        public List<Standing> getStandings() { return standings; } // 🔥 ADDED
    }

    public static class MyResult {

        @SerializedName("position")
        private int position;

        @SerializedName("kills")
        private int kills;

        @SerializedName("rewardGC")
        private int rewardGC;

        @SerializedName("claimed")
        private boolean claimed;

        public int getPosition() { return position; }
        public int getKills() { return kills; }
        public int getRewardGC() { return rewardGC; }
        public boolean isClaimed() { return claimed; }
    }

    // 🔥 ADDED: Matches exact API response structure confirmed from logs:
    // {"teamName":"GLAM 4's team","totalPoint":145,"kills":92,"booyah":2,"totalPositionPoints":53,"position":1}
    public static class Standing {

        @SerializedName("teamName")
        private String teamName;

        @SerializedName("totalPoint")
        private int totalPoint;

        @SerializedName("kills")
        private int kills;

        @SerializedName("booyah")
        private int booyah;

        @SerializedName("totalPositionPoints")
        private int totalPositionPoints;

        @SerializedName("position")
        private int position;

        public String getTeamName() { return teamName != null ? teamName : "—"; }
        public int getTotalPoint() { return totalPoint; }
        public int getKills() { return kills; }
        public int getBooyah() { return booyah; }
        public int getTotalPositionPoints() { return totalPositionPoints; }
        public int getPosition() { return position; }
    }
}