package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("_id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("type")
    private String type;

    @SerializedName("amountGC")
    private double amountGC;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("addedBy")
    private String addedBy;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Constructor for dummy data (backwards compatibility)
    public Transaction(String dateTime, String type, String amount, String status, String description, boolean isPositive) {
        this.createdAt = dateTime;
        this.type = type;
        this.description = description;
        this.status = status;
        // Parse amount if needed
        this.amountGC = isPositive ? Math.abs(parseAmount(amount)) : -Math.abs(parseAmount(amount));
    }

    // Default constructor for Gson
    public Transaction() {
    }

    private double parseAmount(String amount) {
        try {
            return Double.parseDouble(amount.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public double getAmountGC() {
        return amountGC;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    // Helper methods for adapter
    public String getDateTime() {
        if (createdAt == null || createdAt.isEmpty()) {
            return "N/A";
        }

        try {
            // Parse ISO 8601 format: "2026-01-30T06:33:51.671Z"
            String[] parts = createdAt.split("T");
            if (parts.length > 0) {
                String datePart = parts[0]; // "2026-01-30"
                String[] dateSplit = datePart.split("-");
                if (dateSplit.length == 3) {
                    String month = getMonthName(Integer.parseInt(dateSplit[1]));
                    String day = dateSplit[2];
                    return month + " " + day;
                }
            }
            return createdAt.substring(0, Math.min(10, createdAt.length()));
        } catch (Exception e) {
            return createdAt;
        }
    }

    private String getMonthName(int month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return (month >= 1 && month <= 12) ? months[month - 1] : "";
    }

    public String getAmount() {
        String sign = amountGC >= 0 ? "+" : "";
        return sign + (int) Math.round(amountGC) + " GC";
    }

    public boolean isPositive() {
        return amountGC >= 0;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAmountGC(double amountGC) {
        this.amountGC = amountGC;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}