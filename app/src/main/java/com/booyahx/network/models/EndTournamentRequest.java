package com.booyahx.network.models;

public class EndTournamentRequest {
    private String reason;

    public EndTournamentRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}