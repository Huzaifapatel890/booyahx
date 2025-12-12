package com.booyahx.network;

import com.booyahx.network.models.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface ApiService {

    // REGISTER
    @POST("/api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    // VERIFY OTP
    @POST("/api/auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body VerifyOtpRequest request);

    // LOGIN
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

    @POST("/api/auth/refresh-token")
    Call<RefreshResponse> refreshToken(@Body RefreshRequest request);

    @GET("/api/auth/csrf-token")
    Call<CsrfResponse> getCsrfToken();

    // CHANGE PASSWORD
    @PUT("/api/auth/change-password")
    Call<SimpleResponse> changePassword(
            @Header("Authorization") String token,
            @Header("X-CSRF-Token") String csrfToken,
            @Body ChangePasswordRequest request
    );

    // GET PROFILE
    @GET("/api/profile")
    Call<ProfileResponse> getProfile(
            @Header("Authorization") String token

    );
    @PUT("/api/profile")
    Call<SimpleResponse> updateProfile(
            @Header("Authorization") String token,
            @Header("X-CSRF-Token") String csrfToken,
            @Body UpdateProfileRequest request
    );
}