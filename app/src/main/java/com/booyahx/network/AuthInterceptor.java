package com.booyahx.network;

import android.content.Context;
import android.util.Log;

import com.booyahx.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor_DEBUG";
    private final Context context;

    public AuthInterceptor(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request original = chain.request();
        String path = original.url().encodedPath();

        // 🔥 FIXED: Skip auth endpoints EXCEPT logout and change-password
        // Both logout and change-password need Bearer token
        boolean needsAuth = path.equals("/api/auth/logout")
                || path.equals("/api/auth/change-password");

        if (path.startsWith("/api/auth/") && !needsAuth) {
            Log.d(TAG, "Skipping auth header for public endpoint: " + path);
            return chain.proceed(original);
        }

        String access = TokenManager.getAccessToken(context);
        String csrf = TokenManager.getCsrf(context);

        Log.d(TAG, "Adding auth headers for: " + path);
        Log.d(TAG, "Access Token: " + (access != null ? "EXISTS" : "NULL"));
        Log.d(TAG, "CSRF Token: " + (csrf != null ? "EXISTS" : "NULL"));

        Request.Builder builder = original.newBuilder();

        if (access != null && !access.isEmpty()) {
            builder.header("Authorization", "Bearer " + access);
            Log.d(TAG, "✅ Authorization header added");
        } else {
            Log.e(TAG, "❌ No access token available!");
        }

        if (csrf != null) {
            builder.header("X-CSRF-Token", csrf);
            Log.d(TAG, "✅ CSRF header added");
        }

        return chain.proceed(builder.build());
    }
}