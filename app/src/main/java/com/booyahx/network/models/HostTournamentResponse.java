package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;

public class HostTournamentResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public static class Data {

        @SerializedName("lobbies")
        private Lobbies lobbies;

        @SerializedName("total")
        private int total;

        @SerializedName("counts")
        private Counts counts;

        public Lobbies getLobbies() {
            return lobbies;
        }
    }

    public static class Lobbies {
        @SerializedName("upcoming")
        private List<HostTournament> upcoming;

        @SerializedName("live")
        private List<HostTournament> live;

        @SerializedName("resultPending")
        private List<HostTournament> resultPending;

        @SerializedName("completed")
        private List<HostTournament> completed;

        @SerializedName("cancelled")
        private List<HostTournament> cancelled;

        public List<HostTournament> getUpcoming() {
            return upcoming != null ? upcoming : new ArrayList<>();
        }

        public List<HostTournament> getLive() {
            return live != null ? live : new ArrayList<>();
        }

        public List<HostTournament> getResultPending() {
            return resultPending != null ? resultPending : new ArrayList<>();
        }

        public List<HostTournament> getCompleted() {
            return completed != null ? completed : new ArrayList<>();
        }

        public List<HostTournament> getCancelled() {
            return cancelled != null ? cancelled : new ArrayList<>();
        }
    }

    public static class Counts {
        public int upcoming, live, resultPending, completed;
    }

    public boolean isSuccess() { return success; }
    public Data getData() { return data; }
}