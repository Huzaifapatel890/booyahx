package com.booyahx.network.models;

public class AuthResponse {
    private int status;
    private boolean success;
    private String message;
    private AuthData data;

    public int getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public AuthData getData() {
        return data;
    }

    public static class AuthData {
        private String accessToken;
        private String refreshToken;

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}