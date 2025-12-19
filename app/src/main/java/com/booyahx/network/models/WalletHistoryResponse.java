package com.booyahx.network.models;

import java.util.List;

public class WalletHistoryResponse {

    public int status;
    public boolean success;
    public String message;
    public Data data;

    public static class Data {
        public List<HistoryItem> history;
        public int total;
        public int limit;
        public int skip;
    }

    public static class HistoryItem {
        public String userId;
        public String type;
        public double amountGC;
        public String description;
        public String timestamp;
        public String tournamentId;
    }
}