package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    public boolean success;
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        public String accessToken;
        public String refreshToken;
        public User user;
        public String role;
    }

    public static class User {
        public String id;
        public String email;
        public String name;
        public boolean isEmailVerified;
    }
}