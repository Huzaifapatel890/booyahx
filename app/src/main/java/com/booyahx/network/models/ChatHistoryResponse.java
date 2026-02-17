package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatHistoryResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ChatData data;

    public static class ChatData {
        @SerializedName("tournamentId")
        private String tournamentId;

        @SerializedName("messages")
        private List<MessageData> messages;

        @SerializedName("limit")
        private int limit;

        @SerializedName("skip")
        private int skip;

        // Getters
        public List<MessageData> getMessages() { return messages; }
        public String getTournamentId() { return tournamentId; }
        public int getLimit() { return limit; }
        public int getSkip() { return skip; }
    }

    public static class MessageData {
        @SerializedName("userId")
        private String userId;

        @SerializedName("username")
        private String username;

        @SerializedName("message")
        private String message;

        @SerializedName("timestamp")
        private long timestamp;

        @SerializedName("isHost")
        private boolean isHost;

        // Getters
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public boolean isHost() { return isHost; }
    }

    // Getters
    public boolean isSuccess() { return success; }
    public ChatData getData() { return data; }
}