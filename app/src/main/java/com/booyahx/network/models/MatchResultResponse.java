package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for match result submission
 * Endpoint: POST /api/tournament/submit-match-result
 */
public class MatchResultResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private MatchResultData data;

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MatchResultData getData() {
        return data;
    }

    public void setData(MatchResultData data) {
        this.data = data;
    }

    /**
     * Inner class for response data
     */
    public static class MatchResultData {

        @SerializedName("tournamentId")
        private String tournamentId;

        @SerializedName("matchIndex")
        private int matchIndex;

        @SerializedName("matchResultCount")
        private int matchResultCount;

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

        public int getMatchResultCount() {
            return matchResultCount;
        }

        public void setMatchResultCount(int matchResultCount) {
            this.matchResultCount = matchResultCount;
        }
    }
}