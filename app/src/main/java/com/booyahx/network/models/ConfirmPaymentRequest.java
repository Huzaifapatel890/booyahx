package com.booyahx.network.models;

public class ConfirmPaymentRequest {
    public String qrCodeId;
    public String utr;
    public String paymentProof;

    public ConfirmPaymentRequest(String qrCodeId, String utr, String paymentProof) {
        this.qrCodeId = qrCodeId;
        this.utr = utr;
        this.paymentProof = paymentProof;
    }
}