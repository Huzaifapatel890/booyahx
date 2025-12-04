package com.booyahx.network.models;

public class GoogleLoginRequest {
    public String idToken;

    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
    }
}