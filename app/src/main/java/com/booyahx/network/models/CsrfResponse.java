package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class CsrfResponse {

    private int status;
    private boolean success;
    private String message;
    private Data data;

    public Data getData() {
        return data;
    }

    public static class Data {

        @SerializedName("csrfToken")   // 🔥 EXACT KEY FROM SERVER
        private String csrfToken;

        public String getCsrfToken() {
            return csrfToken;
        }
    }
}