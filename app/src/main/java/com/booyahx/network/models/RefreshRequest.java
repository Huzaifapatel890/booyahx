package com.booyahx.network.models;

public class RefreshRequest {
    private String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}