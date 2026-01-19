package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HostTournament {

    // ========================
    // CORE FIELDS
    // ========================

    @SerializedName("_id")
    private String id;

    @SerializedName("game")
    private String game;

    @SerializedName("mode")
    private String mode;

    @SerializedName("subMode")
    private String subMode;

    @SerializedName("entryFee")
    private int entryFee;

    @SerializedName("maxPlayers")
    private int maxPlayers;

    @SerializedName("date")
    private String date;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("lockTime")
    private String lockTime;

    @SerializedName("participants")
    private List<Participant> participants;

    @SerializedName("hostId")
    private Host host;

    @SerializedName("room")
    private Room room;

    @SerializedName("prizePool")
    private int prizePool;

    @SerializedName("status")
    private String status;

    @SerializedName("results")
    private List<Result> results;

    // ========================
    // INNER MODELS
    // ========================

    public static class Host {
        @SerializedName("_id")
        private String id;

        @SerializedName("email")
        private String email;

        @SerializedName("name")
        private String name;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
    }

    public static class Room {
        @SerializedName("roomId")
        private String roomId;

        @SerializedName("password")
        private String password;

        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Participant {

        @SerializedName("_id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("ign")
        private String ign;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getIgn() { return ign; }

        public String getDisplayName() {
            return ign != null && !ign.isEmpty() ? ign : name;
        }
    }

    public static class Result {

        @SerializedName("userId")
        private String userId;

        @SerializedName("position")
        private int position;

        @SerializedName("kills")
        private int kills;

        @SerializedName("rewardGC")
        private int rewardGC;

        @SerializedName("claimed")
        private boolean claimed;

        public String getUserId() { return userId; }
        public int getPosition() { return position; }
        public int getKills() { return kills; }
        public int getRewardGC() { return rewardGC; }
        public boolean isClaimed() { return claimed; }
    }

    // ========================
    // GETTERS
    // ========================

    public String getId() { return id; }
    public String getGame() { return game; }
    public String getMode() { return mode; }
    public String getSubMode() { return subMode; }
    public int getEntryFee() { return entryFee; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getLockTime() { return lockTime; }
    public List<Participant> getParticipants() { return participants; }
    public Host getHost() { return host; }
    public Room getRoom() { return room; }
    public int getPrizePool() { return prizePool; }
    public String getStatus() { return status; }
    public List<Result> getResults() { return results; }

    // ========================
    // UI HELPERS
    // ========================

    public String getTitle() {
        return game != null && mode != null
                ? game + " - " + mode
                : "Unknown Tournament";
    }

    public String getModeDisplay() {
        return (mode != null ? mode : "-") +
                " - " +
                (subMode != null ? subMode : "-");
    }

    public String getEntryFeeDisplay() {
        return entryFee + " GC";
    }

    public String getPrizePoolDisplay() {
        return prizePool + " GC";
    }

    public String getSlotsDisplay() {
        int current = participants != null ? participants.size() : 0;
        return current + "/" + maxPlayers;
    }

    public String getTimeStatusDisplay() {
        return status != null ? status.toUpperCase() : "UNKNOWN";
    }

    public String getRoomIdDisplay() {
        return room != null && room.getRoomId() != null
                ? room.getRoomId()
                : "N/A";
    }

    public String getPasswordDisplay() {
        return room != null && room.getPassword() != null
                ? room.getPassword()
                : "N/A";
    }

    public String getHostName() {
        return host != null ? host.getName() : "Unknown Host";
    }
}