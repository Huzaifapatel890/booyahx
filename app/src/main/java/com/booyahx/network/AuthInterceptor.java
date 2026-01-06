package com.booyahx.network;

import android.content.Context;

import com.booyahx.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();

        // Skip ONLY auth endpoints
        if (path.startsWith("/api/auth/")) {
            return chain.proceed(original);
        }

        String access = TokenManager.getAccessToken(context);
        if (access == null || access.isEmpty()) {
            return chain.proceed(original);
        }

        Request authReq = original.newBuilder()
                .header("Authorization", "Bearer " + access)
                .build();

        return chain.proceed(authReq);
    }
}