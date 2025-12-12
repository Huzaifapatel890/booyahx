package com.booyahx.network.models;

public class UpdateProfileRequest {

    public String name;
    public String ign;
    public String gender;
    public int age;
    public String phoneNumber;
    public String paymentUPI;
    public String paymentMethod;

    public UpdateProfileRequest(String name,
                                String ign,
                                String gender,
                                int age,
                                String phoneNumber,
                                String paymentUPI,
                                String paymentMethod) {

        this.name = name;
        this.ign = ign;
        this.gender = gender;
        this.age = age;
        this.phoneNumber = phoneNumber;
        this.paymentUPI = paymentUPI;
        this.paymentMethod = paymentMethod;
    }
}