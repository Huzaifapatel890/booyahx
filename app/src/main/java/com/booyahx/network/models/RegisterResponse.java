package com.booyahx.network.models;

public class RegisterResponse {
    private int status;
    private boolean success;
    private String message;
    private UserData data;

    public int getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserData getData() {
        return data;
    }

    public static class UserData {
        private String userId;
        private String email;
        private String name;

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }
    }
}