package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for submitting final tournament result
 * Endpoint: POST /api/tournament/submit-final-result
 */
public class FinalResultRequest {

    @SerializedName("tournamentId")
    private String tournamentId;

    public FinalResultRequest(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    // Getters and Setters
    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }
}