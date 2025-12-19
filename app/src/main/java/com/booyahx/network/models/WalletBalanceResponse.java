package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class WalletBalanceResponse {

    @SerializedName("data")
    public Data data;

    public static class Data {

        // 🔥 FIX: backend uses balanceGC
        @SerializedName("balanceGC")
        public double balanceGC;
    }
}