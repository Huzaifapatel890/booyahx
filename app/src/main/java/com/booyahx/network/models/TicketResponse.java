package com.booyahx.network.models;

import java.util.List;

public class TicketResponse {

    private boolean success;
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        private List<Ticket> tickets;

        public List<Ticket> getTickets() {
            return tickets;
        }
    }
}