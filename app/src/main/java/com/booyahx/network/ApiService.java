package com.booyahx.network;

import com.booyahx.network.models.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import java.util.List;
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
    Call<ProfileResponse> getProfile();
    @PUT("/api/profile")
    Call<SimpleResponse> updateProfile(
            @Header("Authorization") String token,
            @Header("X-CSRF-Token") String csrfToken,
            @Body UpdateProfileRequest request
    );
    @GET("/api/wallet/history")
    Call<WalletHistoryResponse> getWalletHistory(
            @Query("limit") int limit,
            @Query("skip") int skip
    );

    @GET("/api/tournament/list")
    Call<TournamentResponse> getTournaments(
            @Query("status") String status,
            @Query("mode") String mode
    );
    @GET("/api/wallet/balance")
    Call<WalletBalanceResponse> getWalletBalance();

    @POST("/api/tournament/join")
    Call<JoinTournamentResponse> joinTournament(
            @Header("Authorization") String auth,
            @Header("X-CSRF-Token") String csrf,
            @Body JoinTournamentRequest request
    );

    @GET("/api/tournament/joined")
    Call<JoinedTournamentResponse> getJoinedTournaments();

    @GET("/api/support/tickets")
    Call<TicketResponse> getTickets(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status
    );

    @POST("/api/support/tickets")
    Call<TicketResponse> createTicket(
            @Header("Authorization") String token,
            @Header("X-CSRF-Token") String csrf,
            @Body CreateTicketRequest request
    );
    }