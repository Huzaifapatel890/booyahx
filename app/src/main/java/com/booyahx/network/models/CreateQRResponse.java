package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class CreateQRResponse {
    public boolean success;
    public String message;
    public Data data;

    public static class Data {
        public String qrCodeId;
        public String paymentId;
        public String receiptCode;

        @SerializedName("qrCodeImage")
        public String qrCodeImage;  // Base64 PNG image

        @SerializedName("qrCodeSVG")
        public String qrCodeSVG;    // SVG string

        @SerializedName("qrCodeString")
        public String qrCodeString;  // UPI string

        @SerializedName("upiLink")
        public String upiLink;       // UPI link for QR generation

        public double amountINR;
        public double amountGC;
        public boolean fixedAmount;
        public String description;
        public String expiresAt;
        public String transactionId;
        public String status;
        public String message;
    }
}