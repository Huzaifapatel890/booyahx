package com.booyahx.network.models;

public class RegisterRequest {
    private String email;
    private String name;

    public RegisterRequest(String email, String name) {
        this.email = email;
        this.name = name;
    }
}