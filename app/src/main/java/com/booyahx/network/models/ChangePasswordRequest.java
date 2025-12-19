package com.booyahx.network.models;

public class ChangePasswordRequest {

    private String oldPassword;
    private String newPassword;
    private String confirmPassword;

    public ChangePasswordRequest(String oldPassword, String newPassword, String confirmPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}