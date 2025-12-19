package com.booyahx.network;

import android.content.Context;

import com.booyahx.TokenManager;
import com.booyahx.utils.CSRFHelper;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request original = chain.request();
        Request.Builder builder = original.newBuilder();

        String path = original.url().encodedPath();

        // ❌ Do NOT attach headers to auth endpoints
        if (!path.contains("/auth/login")
                && !path.contains("/auth/register")
                && !path.contains("/auth/refresh")) {

            // ✅ Attach JWT
            String access = TokenManager.getAccessToken(context);
            if (access != null && !access.isEmpty()) {
                builder.header("Authorization", "Bearer " + access);
            }

            // ✅ Attach CSRF (cookie-based, auto-updated by backend)
            String csrf = CSRFHelper.getToken(context);
            if (csrf != null && !csrf.isEmpty()) {
                builder.header("X-CSRF-Token", csrf);
            }
        }

        return chain.proceed(builder.build());
    }
}