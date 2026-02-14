package com.booyahx.network.models;

import java.util.List;

/**
 * Response model for GET /api/tournament/{tournamentId}/live-results
 * This matches the actual API response structure from your backend
 */
public class LiveResultResponse {
    private int status;
    private boolean success;
    private String message;
    private Data data;

    public static class Data {
        private String tournamentId;
        private String game;
        private String mode;
        private String subMode;
        private String date;
        private String startTime;
        private String status;
        private int totalMatches;
        private List<Standing> standings;

        // Getters
        public String getTournamentId() { return tournamentId; }
        public String getGame() { return game; }
        public String getMode() { return mode; }
        public String getSubMode() { return subMode; }
        public String getDate() { return date; }
        public String getStartTime() { return startTime; }
        public String getStatus() { return status; }
        public int getTotalMatches() { return totalMatches; }
        public List<Standing> getStandings() { return standings; }
    }

    /**
     * Standing model - matches the API response fields exactly
     */
    public static class Standing {
        private String teamName;
        private int totalPoint;          // Total points for the team
        private int kills;               // Total kills across all matches
        private int booyah;              // Number of booyahs (1st place finishes)
        private int totalPositionPoints; // Total position points earned
        private int position;            // Current standing position (rank)

        // Getters
        public String getTeamName() { return teamName; }
        public int getTotalPoint() { return totalPoint; }
        public int getKills() { return kills; }
        public int getBooyah() { return booyah; }
        public int getTotalPositionPoints() { return totalPositionPoints; }
        public int getPosition() { return position; }
    }

    // Getters
    public int getStatus() { return status; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Data getData() { return data; }
}