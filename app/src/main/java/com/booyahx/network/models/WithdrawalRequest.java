package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class WithdrawalRequest {

    @SerializedName("amountGC")
    private int amountGC;

    @SerializedName("description")
    private String description;

    public WithdrawalRequest(int amountGC, String description) {
        this.amountGC = amountGC;
        this.description = description;
    }

    // Getters
    public int getAmountGC() {
        return amountGC;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setAmountGC(int amountGC) {
        this.amountGC = amountGC;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}