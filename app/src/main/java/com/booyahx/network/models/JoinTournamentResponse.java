package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class JoinTournamentResponse {

    @SerializedName("status")
    public int status;

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("tournamentId")
        public String tournamentId;

        @SerializedName("entryFee")
        public int entryFee;
    }
}