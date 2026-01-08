package com.booyahx.network.models;

public class QRStatusResponse {
    public boolean success;
    public String qrCodeId;
    public String status;         // pending | completed | failed | expired
    public int amountINR;
    public String description;
    public String utr;
    public String createdAt;
    public String updatedAt;
}