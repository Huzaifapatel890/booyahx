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

        @SerializedName("pagination")
        public Pagination pagination;
    }

    public static class Pagination {
        @SerializedName("currentPage")
        public int currentPage;

        @SerializedName("totalPages")
        public int totalPages;

        @SerializedName("totalItems")
        public int totalItems;

        @SerializedName("itemsPerPage")
        public int itemsPerPage;

        @SerializedName("hasNextPage")
        public boolean hasNextPage;

        @SerializedName("hasPrevPage")
        public boolean hasPrevPage;
    }
}