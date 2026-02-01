package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TopUpHistoryResponse {
    @SerializedName("status")
    public int status;

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public TopUpHistoryData data;

    public static class TopUpHistoryData {
        @SerializedName("history")
        public List<Transaction> history;

        @SerializedName("total")
        public int total;

        @SerializedName("limit")
        public int limit;

        @SerializedName("skip")
        public int skip;
    }
}