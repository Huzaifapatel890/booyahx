package com.booyahx.network;

import com.booyahx.network.models.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface ApiService {

    /* ================= AUTH ================= */

    @POST("/api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("/api/auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body VerifyOtpRequest request);

    @POST("/api/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);

    @POST("/api/auth/forgot-password")
    Call<SimpleResponse> forgotPassword(@Body ForgotPasswordRequest request);

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
            @Body ChangePasswordRequest request
    );



    /* ================= PROFILE ================= */

    @GET("/api/profile")
    Call<ProfileResponse> getProfile();

    @PUT("/api/profile")
    Call<SimpleResponse> updateProfile(@Body UpdateProfileRequest request);

    @POST("/api/auth/logout")
    Call<LogoutResponse> logout(@Body LogoutRequest request);


    /* ================= WALLET ================= */

    @GET("/api/wallet/history")
    Call<WalletHistoryResponse> getWalletHistory(
            @Query("limit") int limit,
            @Query("skip") int skip
    );
    @GET("/api/wallet/topup-history")
    Call<TopUpHistoryResponse> getTopUpHistory(
            @Query("limit") int limit,
            @Query("skip") int skip
    );

    @GET("/api/wallet/balance")
    Call<WalletBalanceResponse> getWalletBalance();


    /* ================= TOURNAMENT ================= */

    @GET("/api/tournament/list")
    Call<TournamentResponse> getTournaments(
            @Query("status") String status,
            @Query("mode") String mode
    );

    @POST("/api/tournament/join")
    Call<JoinTournamentResponse> joinTournament(
            @Body JoinTournamentRequest request
    );

    @GET("/api/tournament/joined")
    Call<JoinedTournamentResponse> getJoinedTournaments();

    @POST("api/host/tournaments/{tournamentId}/end")
    Call<EndTournamentResponse> endTournament(
            @Path("tournamentId") String tournamentId,
            @Body EndTournamentRequest request
    );

    /* ================= SUPPORT ================= */

    @GET("/api/support/tickets")
    Call<TicketResponse> getTickets(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status
    );

    @POST("/api/support/tickets")
    Call<TicketResponse> createTicket(
            @Body CreateTicketRequest request
    );


    /* ================= HOST ================= */

    @POST("/api/host/tournaments/{id}/apply")
    Call<HostApplyResponse> applyForHostTournament(
            @Path("id") String tournamentId,
            @Body HostApplyRequest request
    );

    @GET("/api/host/tournaments/available")
    Call<HostTournamentsListResponse> getHostTournaments(
            @Query("status") String status,
            @Query("mode") String mode
    );

    @POST("/api/host/tournaments/{tournamentId}/update-room")
    Call<UpdateRoomResponse> updateRoom(
            @Path("tournamentId") String tournamentId,
            @Body UpdateRoomRequest request
    );
    /* ================= PAYMENT ================= */
    @POST("/api/payment/create-qr")
    Call<CreateQRResponse> createQR(@Body CreateQRRequest request);

    @POST("/api/payment/confirm")
    Call<SuccessResponse> confirmPayment(@Body ConfirmPaymentRequest request);

    @GET("/api/payment/qr-status/{qrCodeId}")
    Call<QRStatusResponse> getQRStatus(@Path("qrCodeId") String qrCodeId);

    @POST("/api/payment/close-qr/{qrCodeId}")
    Call<SuccessResponse> closeQR(@Path("qrCodeId") String qrCodeId);

    @GET("/api/host/my-lobbies")
    Call<HostTournamentResponse> getHostMyLobbies();

}