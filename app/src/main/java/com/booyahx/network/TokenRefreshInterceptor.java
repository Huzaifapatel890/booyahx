package com.booyahx.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.booyahx.LoginActivity;
import com.booyahx.TokenManager;
import com.booyahx.network.models.RefreshRequest;
import com.booyahx.network.models.RefreshResponse;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

public final class TokenRefreshInterceptor implements Interceptor {

    private static final String TAG = "TokenRefresh";
    private static final int MAX_RETRIES = 1;

    private final Context ctx;
    private boolean refreshing = false;
    private final Object LOCK = new Object();

    public TokenRefreshInterceptor(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        String retryHeader = request.header("X-Retry-Count");
        int retryCount = retryHeader != null ? Integer.parseInt(retryHeader) : 0;

        Response response = chain.proceed(request);

        if (response.code() != 401
                || request.header("Authorization") == null
                || request.url().encodedPath().startsWith("/api/auth/")
                || retryCount >= MAX_RETRIES) {
            return response;
        }

        Log.d(TAG, "Got 401, attempting token refresh...");

        synchronized (LOCK) {
            String currentToken = TokenManager.getAccessToken(ctx);
            String requestToken = request.header("Authorization");

            if (requestToken != null && currentToken != null
                    && !requestToken.equals("Bearer " + currentToken)) {
                Log.d(TAG, "Token already refreshed by another thread");
                response.close();

                Request retry = request.newBuilder()
                        .header("Authorization", "Bearer " + currentToken)
                        .header("X-Retry-Count", String.valueOf(retryCount + 1))
                        .build();

                return chain.proceed(retry);
            }

            if (!refreshing) {
                refreshing = true;
                try {
                    String refreshToken = TokenManager.getRefreshToken(ctx);
                    if (refreshToken == null) {
                        Log.e(TAG, "No refresh token available");
                        redirectToLogin();
                        return response;
                    }

                    Log.d(TAG, "Calling refresh endpoint with refresh token...");

                    ApiService api = ApiClient
                            .getRefreshClient()
                            .create(ApiService.class);

                    RefreshRequest refreshRequest = new RefreshRequest(refreshToken);
                    Call<RefreshResponse> call = api.refreshToken(refreshRequest);

                    retrofit2.Response<RefreshResponse> r = call.execute();

                    Log.d(TAG, "Refresh response code: " + r.code());

                    if (r.isSuccessful() && r.body() != null && r.body().success) {
                        Log.d(TAG, "Token refresh successful");

                        TokenManager.saveTokens(
                                ctx,
                                r.body().data.accessToken,
                                r.body().data.refreshToken
                        );

                        // 🔥 EXTRACT AND SAVE NEW CSRF TOKEN FROM REFRESH RESPONSE
                        String newCsrf = r.headers().get("x-csrf-token");
                        if (newCsrf == null || newCsrf.isEmpty()) {
                            newCsrf = r.headers().get("X-CSRF-Token");
                        }

                        if (newCsrf != null && !newCsrf.isEmpty()) {
                            Log.d(TAG, "New CSRF token received from refresh: " + newCsrf);
                            TokenManager.saveCsrf(ctx, newCsrf);
                        }

                    } else {
                        String errorMsg = r.body() != null ? r.body().message : "null body";
                        Log.e(TAG, "Token refresh failed: " + errorMsg);

                        redirectToLogin();
                        return response;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Token refresh exception", e);
                    redirectToLogin();
                    return response;
                } finally {
                    refreshing = false;
                    LOCK.notifyAll();
                }
            } else {
                try {
                    Log.d(TAG, "Waiting for token refresh to complete...");
                    LOCK.wait(5000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Wait interrupted", e);
                    return response;
                }
            }
        }

        String newToken = TokenManager.getAccessToken(ctx);
        if (newToken == null) {
            Log.e(TAG, "No token after refresh attempt");
            redirectToLogin();
            return response;
        }

        response.close();

        Log.d(TAG, "Retrying request with new token");

        // 🔥 GET UPDATED CSRF TOKEN FOR RETRY
        String newCsrf = TokenManager.getCsrf(ctx);

        Request.Builder retryBuilder = request.newBuilder()
                .header("Authorization", "Bearer " + newToken)
                .header("X-Retry-Count", String.valueOf(retryCount + 1));

        if (newCsrf != null) {
            retryBuilder.header("X-CSRF-Token", newCsrf);
        }

        return chain.proceed(retryBuilder.build());
    }

    private void redirectToLogin() {
        TokenManager.clearTokens(ctx);

        Intent intent = new Intent(ctx, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(intent);

        Log.d(TAG, "Redirected to LoginActivity");
    }
}