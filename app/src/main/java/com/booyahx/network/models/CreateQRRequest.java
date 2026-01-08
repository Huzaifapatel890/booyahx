package com.booyahx.network.models;

public class CreateQRRequest {
    public int amountINR;
    public boolean fixedAmount;
    public String description;

    public CreateQRRequest(int amountINR, boolean fixedAmount, String description) {
        this.amountINR = amountINR;
        this.fixedAmount = fixedAmount;
        this.description = description;
    }
}