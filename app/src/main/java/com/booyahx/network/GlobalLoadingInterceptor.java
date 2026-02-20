package com.booyahx.network;

import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;

import com.booyahx.utils.GlobalLoadingDialog;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class GlobalLoadingInterceptor implements Interceptor {

    private FragmentActivity activity;
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GlobalLoadingInterceptor(FragmentActivity activity) {
        this.activity = activity;
    }

    public void updateActivity(FragmentActivity newActivity) {
        this.activity = newActivity;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // ✅ SILENT MODE: If the request carries the X-Silent-Request header (set by
        // ApiClient.getSilentClient()), skip the loader entirely and strip the header
        // so it never reaches the server. This covers ALL broadcast/websocket-driven
        // background API calls without needing per-URL configuration.
        boolean isSilent = "true".equals(request.header("X-Silent-Request"));
        if (isSilent) {
            request = request.newBuilder()
                    .removeHeader("X-Silent-Request")
                    .build();
        }

        // Check if this request should skip the loader (silent flag OR URL-based rules)
        boolean shouldSkipLoader = isSilent || shouldSkipLoader(request);

        // Increment active requests and show loader (only if not skipping)
        int count = 0;
        if (!shouldSkipLoader) {
            count = activeRequests.incrementAndGet();
            if (count == 1) {
                showLoader();
            }
        }

        Response response = null;
        try {
            response = chain.proceed(request);
            return response;
        } finally {
            // Decrement active requests and hide loader if no more requests (only if not skipping)
            if (!shouldSkipLoader) {
                int remainingCount = activeRequests.decrementAndGet();
                if (remainingCount == 0) {
                    hideLoader();
                }
            }
        }
    }

    /**
     * URL-based loader skip rules.
     * For broadcast/websocket-driven calls use ApiClient.getSilentClient() instead —
     * that is the global mechanism. Only add URLs here for endpoints that should
     * ALWAYS be silent regardless of who calls them.
     */
    private boolean shouldSkipLoader(Request request) {
        String url = request.url().toString();

        // wallet balance — also covers withdrawal limit data (same unified endpoint)
        if (url.contains("/api/wallet/balance")) {
            return true;
        }

        // legacy withdraw-limit endpoint (deprecated but kept for safety)
        if (url.contains("/api/wallet/withdraw-limit")) {
            return true;
        }

        return false;
    }

    private void showLoader() {
        if (activity != null) {
            mainHandler.post(() -> {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    try {
                        GlobalLoadingDialog.show(activity.getSupportFragmentManager());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void hideLoader() {
        if (activity != null) {
            mainHandler.post(() -> {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    try {
                        GlobalLoadingDialog.hide(activity.getSupportFragmentManager());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}