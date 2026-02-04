package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class WithdrawalLimitResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public static class Data {
        @SerializedName("maxWithdrawableGC")
        private int maxWithdrawableGC;

        @SerializedName("totalDepositedGC")
        private int totalDepositedGC;

        @SerializedName("withdrawnGC")
        private int withdrawnGC;

        @SerializedName("balanceGC")
        private int balanceGC;

        // Getters
        public int getMaxWithdrawableGC() {
            return maxWithdrawableGC;
        }

        public int getTotalDepositedGC() {
            return totalDepositedGC;
        }

        public int getWithdrawnGC() {
            return withdrawnGC;
        }

        public int getBalanceGC() {
            return balanceGC;
        }

        // Setters
        public void setMaxWithdrawableGC(int maxWithdrawableGC) {
            this.maxWithdrawableGC = maxWithdrawableGC;
        }

        public void setTotalDepositedGC(int totalDepositedGC) {
            this.totalDepositedGC = totalDepositedGC;
        }

        public void setWithdrawnGC(int withdrawnGC) {
            this.withdrawnGC = withdrawnGC;
        }

        public void setBalanceGC(int balanceGC) {
            this.balanceGC = balanceGC;
        }
    }

    // Getters
    public int getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    // Setters
    public void setStatus(int status) {
        this.status = status;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(Data data) {
        this.data = data;
    }
}