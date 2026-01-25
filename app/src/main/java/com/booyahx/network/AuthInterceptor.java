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

        // 🔥 Skip auth endpoints EXCEPT logout (logout needs Bearer token)
        if (path.startsWith("/api/auth/") && !path.equals("/api/auth/logout")) {
            return chain.proceed(original);
        }

        String access = TokenManager.getAccessToken(context);
        String csrf = TokenManager.getCsrf(context);

        Request.Builder builder = original.newBuilder();

        if (access != null && !access.isEmpty()) {
            builder.header("Authorization", "Bearer " + access);
        }

        if (csrf != null) {
            builder.header("X-CSRF-Token", csrf);
        }

        return chain.proceed(builder.build());
    }
}