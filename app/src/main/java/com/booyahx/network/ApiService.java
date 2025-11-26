package com.booyahx.network;

import com.booyahx.network.models.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface ApiService {


    // 🟢 REGISTER (sends OTP to email)
    @POST("/api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    // 🟢 VERIFY OTP & set password
    @POST("/api/auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body VerifyOtpRequest request);

}