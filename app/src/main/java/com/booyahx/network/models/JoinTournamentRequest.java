package com.booyahx.network.models;

public class JoinTournamentRequest {

    public String tournamentId;
    public String teamName;

    public JoinTournamentRequest(String tournamentId, String teamName) {
        this.tournamentId = tournamentId;
        this.teamName = teamName;
    }
}