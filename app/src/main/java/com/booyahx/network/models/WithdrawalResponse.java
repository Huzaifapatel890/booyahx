package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class WithdrawalResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public static class Data {
        @SerializedName("id")
        private int id;

        @SerializedName("updatedAt")
        private String updatedAt;

        @SerializedName("transaction")
        private Transaction transaction;

        public static class Transaction {
            @SerializedName("id")
            private int id;

            @SerializedName("type")
            private String type;

            @SerializedName("amountGC")
            private int amountGC;

            @SerializedName("description")
            private String description;

            @SerializedName("createdAt")
            private String createdAt;

            // Getters
            public int getId() {
                return id;
            }

            public String getType() {
                return type;
            }

            public int getAmountGC() {
                return amountGC;
            }

            public String getDescription() {
                return description;
            }

            public String getCreatedAt() {
                return createdAt;
            }

            // Setters
            public void setId(int id) {
                this.id = id;
            }

            public void setType(String type) {
                this.type = type;
            }

            public void setAmountGC(int amountGC) {
                this.amountGC = amountGC;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public void setCreatedAt(String createdAt) {
                this.createdAt = createdAt;
            }
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        // Setters
        public void setId(int id) {
            this.id = id;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public void setTransaction(Transaction transaction) {
            this.transaction = transaction;
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