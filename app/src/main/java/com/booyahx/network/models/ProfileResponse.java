package com.booyahx.network.models;

public class ProfileResponse {

    public int status;
    public boolean success;
    public String message;
    public Data data;

    public static class Data {
        public String userId;
        public String email;
        public String name;
        public String ign;
        public String phoneNumber;
        public String gender;
        public int age;
        public String paymentUPI;
        public String paymentMethod;
        public boolean isPaymentVerified;
        public String createdAt;
        public String updatedAt;
    }
}