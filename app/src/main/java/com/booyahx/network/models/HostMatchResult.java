package com.booyahx.network.models;

public class HostMatchResult {
    private String teamName;
    private int killPoints;
    private int positionPoints;
    private int totalPoints;

    public HostMatchResult(String teamName, int killPoints, int positionPoints) {
        this.teamName = teamName;
        this.killPoints = killPoints;
        this.positionPoints = positionPoints;
        this.totalPoints = killPoints + positionPoints;
    }

    public String getTeamName() { return teamName; }
    public int getKillPoints() { return killPoints; }
    public int getPositionPoints() { return positionPoints; }
    public int getTotalPoints() { return totalPoints; }

    public void setKillPoints(int kp) {
        this.killPoints = kp;
        this.totalPoints = killPoints + positionPoints;
    }

    public void setPositionPoints(int pp) {
        this.positionPoints = pp;
        this.totalPoints = killPoints + positionPoints;
    }
}