package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class WalletBalanceResponse {

    @SerializedName("data")
    public Data data;

    public static class Data {

        // 🔥 FIX: backend uses balanceGC
        @SerializedName("balanceGC")
        public double balanceGC;

        // 🔥 NEW FIELDS - Added for merged API endpoint
        @SerializedName("maxWithdrawableGC")
        private Integer maxWithdrawableGC;

        @SerializedName("totalDepositsGC")  // 🔥 FIXED: API uses "totalDepositsGC" not "totalDepositedGC"
        private Integer totalDepositsGC;

        @SerializedName("totalWithdrawnGC")  // 🔥 FIXED: API uses "totalWithdrawnGC" not "withdrawnGC"
        private Integer totalWithdrawnGC;

        @SerializedName("dailyLimit")
        private DailyLimit dailyLimit;

        // 🔥 NEW GETTERS
        public Integer getMaxWithdrawableGC() {
            return maxWithdrawableGC;
        }

        public Integer getTotalDepositsGC() {  // 🔥 FIXED: getter name matches field
            return totalDepositsGC;
        }

        public Integer getTotalWithdrawnGC() {  // 🔥 FIXED: getter name matches field
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