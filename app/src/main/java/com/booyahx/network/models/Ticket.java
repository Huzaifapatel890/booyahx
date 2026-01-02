package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;

public class Ticket {

    @SerializedName("id")
    private String ticketId;

    @SerializedName("subject")
    private String subject;

    @SerializedName("createdAt")
    private String date;

    @SerializedName("status")
    private String status; // open / closed

    public String getTicketId() {
        return ticketId;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public boolean isOpen() {
        return "open".equalsIgnoreCase(status);
    }
}