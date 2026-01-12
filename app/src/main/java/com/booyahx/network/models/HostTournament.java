package com.booyahx.network.models;

public class HostTournament {
    private String id;
    private String title;
    private String mode;
    private String entryFee;
    private String prizePool;
    private String slots;
    private String timeStatus;
    private String roomId;
    private String password;
    private String slotBadge;

    public HostTournament(String id, String title, String mode, String entryFee, String prizePool,
                          String slots, String timeStatus, String roomId, String password, String slotBadge) {
        this.id = id;
        this.title = title;
        this.mode = mode;
        this.entryFee = entryFee;
        this.prizePool = prizePool;
        this.slots = slots;
        this.timeStatus = timeStatus;
        this.roomId = roomId;
        this.password = password;
        this.slotBadge = slotBadge;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMode() { return mode; }
    public String getEntryFee() { return entryFee; }
    public String getPrizePool() { return prizePool; }
    public String getSlots() { return slots; }
    public String getTimeStatus() { return timeStatus; }
    public String getRoomId() { return roomId; }
    public String getPassword() { return password; }
    public String getSlotBadge() { return slotBadge; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setMode(String mode) { this.mode = mode; }
    public void setEntryFee(String entryFee) { this.entryFee = entryFee; }
    public void setPrizePool(String prizePool) { this.prizePool = prizePool; }
    public void setSlots(String slots) { this.slots = slots; }
    public void setTimeStatus(String timeStatus) { this.timeStatus = timeStatus; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setPassword(String password) { this.password = password; }
    public void setSlotBadge(String slotBadge) { this.slotBadge = slotBadge; }
}