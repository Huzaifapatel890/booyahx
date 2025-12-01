package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    // This will work even if backend returns token OR access_token OR accessToken
    @SerializedName(value = "token", alternate = {"access_token", "accessToken"})
    private String token;

    public boolean isSuccess() {
        return success;
    }

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}