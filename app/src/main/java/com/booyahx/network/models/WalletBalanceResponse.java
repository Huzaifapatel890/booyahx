package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class WalletBalanceResponse {

    @SerializedName("data")
    public Data data;

    public static class Data {

        // 🔥 FIX: backend uses balanceGC
        @SerializedName("balanceGC")
        public double balanceGC;

        // 🔥 FIXED: Changed from Integer to Double because API returns decimal values like 245.99
        @SerializedName("maxWithdrawableGC")
        private Double maxWithdrawableGC;

        @SerializedName("totalDepositsGC")  // 🔥 FIXED: API uses "totalDepositsGC" not "totalDepositedGC"
        private Double totalDepositsGC;

        @SerializedName("totalWithdrawnGC")  // 🔥 FIXED: API uses "totalWithdrawnGC" not "withdrawnGC"
        private Double totalWithdrawnGC;

        @SerializedName("dailyLimit")
        private DailyLimit dailyLimit;

        // 🔥 FIXED: Changed return type to Double
        public Double getMaxWithdrawableGC() {
            return maxWithdrawableGC;
        }

        public Double getTotalDepositsGC() {  // 🔥 FIXED: getter name matches field
            return totalDepositsGC;
        }

        public Double getTotalWithdrawnGC() {  // 🔥 FIXED: getter name matches field
            return totalWithdrawnGC;
        }

        public DailyLimit getDailyLimit() {
            return dailyLimit;
        }

        /**
         * Daily withdrawal limit information
         */
        public static class DailyLimit {
            @SerializedName("count")
            private int count;

            @SerializedName("totalGC")
            private int totalGC;

            @SerializedName("maxGC")
            private int maxGC;

            @SerializedName("maxCount")
            private int maxCount;

            public int getCount() {
                return count;
            }

            public int getTotalGC() {
                return totalGC;
            }

            public int getMaxGC() {
                return maxGC;
            }

            public int getMaxCount() {
                return maxCount;
            }
        }
    }
}