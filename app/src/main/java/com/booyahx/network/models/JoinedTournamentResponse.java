package com.booyahx.network.models;

import com.booyahx.network.models.JoinedTournament;
import java.util.List;

public class JoinedTournamentResponse {

    private int status;
    private boolean success;
    private String message;
    private Data data;

    public Data getData() {
        return data;
    }

    public static class Data {
        private List<JoinedTournament> tournaments;

        public List<JoinedTournament> getTournaments() {
            return tournaments;
        }
    }
}