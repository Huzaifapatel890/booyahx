package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class JoinedTournament {

    @SerializedName("_id")
    private String id;

    private String game;
    private String mode;
    private String subMode;
    private int entryFee;
    private int maxPlayers;

    @SerializedName("date")
    private String rawDate; // ISO string from backend

    private String startTime;
    private String lockTime;

    @SerializedName("participants")
    private List<Object> participants;

    // ✅ FIX: hostId is NOT always a String
    @SerializedName("hostId")
    private Host host;

    private Room room;
    private int prizePool;
    private String status;

    /* ================= DERIVED / SAFE ================= */

    public int getParticipantCount() {
        return participants == null ? 0 : participants.size();
    }

    // 🔥 Adapter expects yyyy-MM-dd
    public String getDate() {
        if (rawDate == null) return "";
        return rawDate.split("T")[0];
    }

    /* ================= GETTERS ================= */

    public String getId() { return id; }
    public String getGame() { return game; }
    public String getMode() { return mode; }
    public String getSubMode() { return subMode; }
    public int getEntryFee() { return entryFee; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getStartTime() { return startTime; }
    public String getLockTime() { return lockTime; }

    // ✅ Backward-safe: keep same method name
    public String getHostId() {
        return host == null ? null : host.id;
    }

    public Host getHost() {
        return host;
    }

    public Room getRoom() { return room; }
    public int getPrizePool() { return prizePool; }
    public String getStatus() { return status; }

    /* ================= INNER MODELS ================= */

    public static class Host {
        @SerializedName("_id")
        private String id;
        private String name;
        private String ign;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getIgn() { return ign; }
    }

    public static class Room {
        private String roomId;
        private String password;

        public String getRoomId() { return roomId; }
        public String getPassword() { return password; }
    }
}