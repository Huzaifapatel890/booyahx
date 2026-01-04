package com.booyahx.network.models;

public class HostApplyResponse {

    public int status;
    public boolean success;
    public String message;
    public Data data;

    public static class Data {
        public Application application;
    }

    public static class Application {
        public String id;
        public String tournamentId;
        public String hostId;
        public String status; // pending / approved / rejected
        public String createdAt;
        public String updatedAt;
    }
}