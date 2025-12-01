package com.booyahx.network.models;

public class GoogleLoginResponse {
    public boolean success;
    public String message;
    public String jwt;

    public User user;

    public static class User {
        public String id;
        public String name;
        public String email;
        public String profilePic;
    }
}