package com.booyahx.helpandsupport;

public class Ticket {
    private String ticketId;
    private String subject;
    private String date;
    private boolean isOpen;

    public Ticket(String ticketId, String subject, String date, boolean isOpen) {
        this.ticketId = ticketId;
        this.subject = subject;
        this.date = date;
        this.isOpen = isOpen;
    }

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
        return isOpen;
    }
}

