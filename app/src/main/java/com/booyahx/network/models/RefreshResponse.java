package com.booyahx.network.models;

public class RefreshResponse {

    public boolean success;
    public String message;
    public Data data;

    public static class Data {
        public String accessToken;
        public String refreshToken;
    }
}