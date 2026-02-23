package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {

    @SerializedName("fcmToken")
    public String fcmToken;

    public FcmTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}