package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TournamentResponse {

    @SerializedName("status")
    public int status;

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {

        @SerializedName("tournaments")
        public List<Tournament> tournaments;

        @SerializedName("rules")
        public Object rules; // will use later
    }
}