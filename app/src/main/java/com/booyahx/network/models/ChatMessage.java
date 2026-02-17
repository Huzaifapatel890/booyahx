package com.booyahx.network.models;

/**
 * Chat Message Model
 * Represents a single chat message in tournament chat
 */
public class ChatMessage {

    public static final int TYPE_SENT = 1;
    public static final int TYPE_RECEIVED = 2;
    public static final int TYPE_HOST = 3;

    private String messageId;
    private String userId;
    private String username;
    private String message;
    private long timestamp;
    private boolean isHost;
    private int messageType;

    public ChatMessage() {
    }

    public ChatMessage(String userId, String username, String message,
                       long timestamp, boolean isHost, int messageType) {
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
        this.isHost = isHost;
        this.messageType = messageType;
        this.messageId = userId + "_" + timestamp;
    }

    // Getters and Setters

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }
}