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
        private double maxWithdrawableGC;

        @SerializedName("totalDepositedGC")
        private double totalDepositedGC;

        @SerializedName("withdrawnGC")
        private double withdrawnGC;

        @SerializedName("balanceGC")
        private double balanceGC;

        // Getters - return int by rounding
        public int getMaxWithdrawableGC() {
            return (int) Math.round(maxWithdrawableGC);
        }

        public int getTotalDepositedGC() {
            return (int) Math.round(totalDepositedGC);
        }

        public int getWithdrawnGC() {
            return (int) Math.round(withdrawnGC);
        }

        public int getBalanceGC() {
            return (int) Math.round(balanceGC);
        }

        // Setters
        public void setMaxWithdrawableGC(double maxWithdrawableGC) {
            this.maxWithdrawableGC = maxWithdrawableGC;
        }

        public void setTotalDepositedGC(double totalDepositedGC) {
            this.totalDepositedGC = totalDepositedGC;
        }

        public void setWithdrawnGC(double withdrawnGC) {
            this.withdrawnGC = withdrawnGC;
        }

        public void setBalanceGC(double balanceGC) {
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