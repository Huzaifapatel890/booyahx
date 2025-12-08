package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class GoogleLoginResponse {
    public boolean success;
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        public String accessToken;
        public String refreshToken;
        public User user;
    }

    public static class User {
        public String id;
        public String name;
        public String email;
        public String profilePic;
    }
}