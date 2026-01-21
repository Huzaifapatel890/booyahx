package com.booyahx.network.models;

public class UpdateRoomRequest {
    private String roomId;
    private String password;

    public UpdateRoomRequest(String roomId, String password) {
        this.roomId = roomId;
        this.password = password;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}