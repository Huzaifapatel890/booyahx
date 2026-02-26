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

    @PUT("/api/auth/change-password")
    Call<SimpleResponse> changePassword(@Body ChangePasswordRequest request);


    /* ================= PROFILE ================= */

    @GET("/api/profile")
    Call<ProfileResponse> getProfile();

    @PUT("/api/profile")
    Call<SimpleResponse> updateProfile(@Body UpdateProfileRequest request);

    @POST("/api/auth/logout")
    Call<LogoutResponse> logout(@Body LogoutRequest request);

    @POST("/api/profile/fcm-token")
    Call<SimpleResponse> saveFcmToken(@Body FcmTokenRequest request);


    /* ================= WALLET ================= */

    @GET("/api/wallet/history")
    Call<WalletHistoryResponse> getWalletHistory(
            @Query("limit") int limit,
            @Query("skip")  int skip
    );

    /**
     * Get top-up and withdrawal history.
     *
     * Supports:
     *   - type      : "topup" | "withdrawal" (omit for both)
     *   - page      : page number (default 1)
     *   - limit     : items per page (max 100, default 20)
     *   - fromDate  : start of date range YYYY-MM-DD  (nullable)
     *   - toDate    : end of date range YYYY-MM-DD    (nullable)
     */
    @GET("/api/wallet/topup-history")
    Call<TopUpHistoryResponse> getTopUpHistory(
            @Query("page")     int    page,
            @Query("limit")    int    limit,
            @Query("type")     String type,      // "topup" | "withdrawal" | null
            @Query("fromDate") String fromDate,  // "YYYY-MM-DD" | null
            @Query("toDate")   String toDate     // "YYYY-MM-DD" | null
    );

    /** @deprecated Use getWalletBalance() instead. */
    @Deprecated
    @GET("/api/wallet/withdraw-limit")
    Call<WithdrawalLimitResponse> getWithdrawLimit();

    @POST("/api/wallet/withdraw")
    Call<WithdrawalResponse> requestWithdrawal(@Body WithdrawalRequest request);

    /** Returns wallet balance AND withdrawal limit data. */
    @GET("/api/wallet/balance")
    Call<WalletBalanceResponse> getWalletBalance();

    /**
     * Cancel a pending withdrawal. Only the owner can cancel while status = "pending".
     * Amount is immediately refunded to wallet on success.
     */
    @POST("/api/wallet/withdraw/{transactionId}/cancel")
    Call<CancelWithdrawalResponse> cancelWithdrawal(
            @Path("transactionId") String transactionId
    );


    /* ================= TOURNAMENT ================= */

    @GET("/api/tournament/list")
    Call<TournamentResponse> getTournaments(
            @Query("status") String status,
            @Query("mode")   String mode
    );

    @GET("api/tournament/userHistory")
    Call<TournamentHistoryResponse> getUserTournamentHistory(
            @Query("limit")  int limit,
            @Query("offset") int offset
    );

    @GET("/api/tournament/userHistory")
    Call<TournamentHistoryResponse> getUserTournamentHistoryFiltered(
            @Query("limit")  int limit,
            @Query("offset") int offset,
            @Query("mode")   String mode,
            @Query("win")    String win
    );

    @GET("/api/tournament/{tournamentId}/live-results")
    Call<LiveResultResponse> getLiveResults(
            @Path("tournamentId") String tournamentId
    );

    @POST("/api/tournament/join")
    Call<JoinTournamentResponse> joinTournament(@Body JoinTournamentRequest request);

    @GET("/api/tournament/joined")
    Call<JoinedTournamentResponse> getJoinedTournaments();


    /* ================= SUPPORT ================= */

    @GET("/api/support/tickets")
    Call<TicketResponse> getTickets(
            @Query("page")   int page,
            @Query("limit")  int limit,
            @Query("status") String status
    );

    @POST("/api/support/tickets")
    Call<TicketResponse> createTicket(@Body CreateTicketRequest request);

    @GET("/api/tournament/{tournamentId}/chat")
    Call<ChatHistoryResponse> getChatHistory(
            @Path("tournamentId") String tournamentId,
            @Query("limit") int limit,
            @Query("skip")  int skip
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
            @Query("mode")   String mode
    );

    @POST("/api/host/tournaments/{tournamentId}/update-room")
    Call<UpdateRoomResponse> updateRoom(
            @Path("tournamentId") String tournamentId,
            @Body UpdateRoomRequest request
    );

    @POST("/api/tournament/submit-match-result")
    Call<MatchResultResponse> submitMatchResult(@Body MatchResultRequest request);

    @POST("/api/tournament/submit-final-result")
    Call<FinalResultResponse> submitFinalResult(@Body FinalResultRequest request);


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