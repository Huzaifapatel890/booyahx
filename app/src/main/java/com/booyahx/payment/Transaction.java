package com.booyahx.payment;

public class Transaction {
    private String dateTime;
    private String type;
    private String amount;
    private String status;
    private String description;
    private boolean isPositive;

    public Transaction(String dateTime, String type, String amount, String status, String description, boolean isPositive) {
        this.dateTime = dateTime;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.isPositive = isPositive;
    }

    // Getters
    public String getDateTime() {
        return dateTime;
    }

    public String getType() {
        return type;
    }

    public String getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPositive() {
        return isPositive;
    }

    // Setters
    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPositive(boolean positive) {
        isPositive = positive;
    }
}