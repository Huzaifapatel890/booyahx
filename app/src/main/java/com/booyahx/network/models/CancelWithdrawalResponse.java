package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for:
 *   POST /api/wallet/withdraw/{transactionId}/cancel
 *
 * Success (200):
 * {
 *   "status": 200,
 *   "success": true,
 *   "message": "Withdrawal request cancelled successfully. Amount has been refunded to your wallet.",
 *   "data": {
 *     "balanceGC": 350,
 *     "updatedAt": "2026-02-19T10:30:00.000Z",
 *     "transaction": {
 *       "_id": "...",
 *       "type": "withdrawal",
 *       "amountGC": 100,
 *       "description": "Withdrawal request",
 *       "status": "cancelled",
 *       "verifiedAt": "2026-02-19T10:30:00.000Z"
 *     }
 *   }
 * }
 */
public class CancelWithdrawalResponse {

    @SerializedName("status")
    public int status;

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("balanceGC")
        public double balanceGC;

        @SerializedName("updatedAt")
        public String updatedAt;

        @SerializedName("transaction")
        public TransactionData transaction;
    }

    public static class TransactionData {
        @SerializedName("_id")
        public String id;

        @SerializedName("type")
        public String type;

        @SerializedName("amountGC")
        public double amountGC;

        @SerializedName("description")
        public String description;

        @SerializedName("status")
        public String status;

        @SerializedName("verifiedAt")
        public String verifiedAt;
    }
}