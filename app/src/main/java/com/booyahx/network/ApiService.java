package com.booyahx.network;

import com.booyahx.network.models.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Body;
public interface ApiService {


    // 🟢 REGISTER (sends OTP to email)
    @POST("/api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    // 🟢 VERIFY OTP & set password
    @POST("/api/auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body VerifyOtpRequest request);

    // 🟠 LOGIN
    @POST("/api/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);
    @Headers("Content-Type: application/json")
    @POST("/api/auth/forgot-password")
    Call<SimpleResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @Headers("Content-Type: application/json")
    @POST("/api/auth/reset-password")
    Call<SimpleResponse> resetPassword(@Body ResetPasswordRequest request);

    @POST("/api/auth/google-login")
    Call<GoogleLoginResponse> loginWithGoogle(@Body GoogleLoginRequest req);
}