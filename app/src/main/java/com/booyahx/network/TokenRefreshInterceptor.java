package com.booyahx.network;

import android.content.Context;

import com.booyahx.TokenManager;
import com.booyahx.network.models.RefreshRequest;
import com.booyahx.network.models.RefreshResponse;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

public final class TokenRefreshInterceptor implements Interceptor {

    private static boolean refreshing = false;
    private static final Object LOCK = new Object();

    private final Context ctx;

    public TokenRefreshInterceptor(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (response.code() != 401 ||
                request.url().encodedPath().startsWith("/api/auth/")) {
            return response;
        }

        response.close();

        synchronized (LOCK) {
            if (!refreshing) {
                refreshing = true;
                try {
                    String refresh = TokenManager.getRefreshToken(ctx);
                    if (refresh == null) return response;

                    ApiService api = ApiClient.getClient(ctx)
                            .create(ApiService.class);

                    Call<RefreshResponse> call =
                            api.refreshToken(new RefreshRequest(refresh));

                    retrofit2.Response<RefreshResponse> r = call.execute();

                    if (r.isSuccessful() && r.body() != null && r.body().success) {
                        TokenManager.saveTokens(
                                ctx,
                                r.body().data.accessToken,
                                r.body().data.refreshToken
                        );
                    }
                } finally {
                    refreshing = false;
                    LOCK.notifyAll();
                }
            } else {
                try {
                    LOCK.wait();
                } catch (InterruptedException ignored) {}
            }
        }

        String newToken = TokenManager.getAccessToken(ctx);
        if (newToken == null) return response;

        Request retry = request.newBuilder()
                .header("Authorization", "Bearer " + newToken)
                .build();

        return chain.proceed(retry);
    }
}