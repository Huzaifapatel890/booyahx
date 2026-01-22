package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HostTournamentsListResponse {

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

        @SerializedName("pagination")
        public Pagination pagination;
    }

    public static class Pagination {
        @SerializedName("page")
        public int page;

        @SerializedName("limit")
        public int limit;

        @SerializedName("total")
        public int total;

        @SerializedName("totalPages")
        public int totalPages;
    }
}