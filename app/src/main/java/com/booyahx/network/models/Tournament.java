package com.booyahx.network.models;

public class Tournament {
    private String title;
    private int expectedPP;      // Expected Prize Pool in GC
    private int currentPP;       // Current Prize Pool in GC
    private int entryFee;
    private int currentSlots;
    private int totalSlots;
    private String dateTime;
    private String mode;

    public Tournament(String title, int expectedPP, int currentPP, int entryFee,
                      int currentSlots, int totalSlots, String dateTime, String mode) {
        this.title = title;
        this.expectedPP = expectedPP;
        this.currentPP = currentPP;
        this.entryFee = entryFee;
        this.currentSlots = currentSlots;
        this.totalSlots = totalSlots;
        this.dateTime = dateTime;
        this.mode = mode;
    }

    // Getters
    public String getTitle() { return title; }
    public int getExpectedPP() { return expectedPP; }
    public int getCurrentPP() { return currentPP; }
    public int getEntryFee() { return entryFee; }
    public int getCurrentSlots() { return currentSlots; }
    public int getTotalSlots() { return totalSlots; }
    public String getDateTime() { return dateTime; }
    public String getMode() { return mode; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setExpectedPP(int expectedPP) { this.expectedPP = expectedPP; }
    public void setCurrentPP(int currentPP) { this.currentPP = currentPP; }
    public void setEntryFee(int entryFee) { this.entryFee = entryFee; }
    public void setCurrentSlots(int currentSlots) { this.currentSlots = currentSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setMode(String mode) { this.mode = mode; }
}