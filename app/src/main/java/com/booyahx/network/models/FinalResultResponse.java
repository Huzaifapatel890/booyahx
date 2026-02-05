package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for final result submission
 * Endpoint: POST /api/tournament/submit-final-result
 */
public class FinalResultResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private FinalResultData data;

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

    public FinalResultData getData() {
        return data;
    }

    public void setData(FinalResultData data) {
        this.data = data;
    }

    /**
     * Inner class for response data
     */
    public static class FinalResultData {

        @SerializedName("tournamentId")
        private String tournamentId;

        // Getters and Setters
        public String getTournamentId() {
            return tournamentId;
        }

        public void setTournamentId(String tournamentId) {
            this.tournamentId = tournamentId;
        }
    }
}