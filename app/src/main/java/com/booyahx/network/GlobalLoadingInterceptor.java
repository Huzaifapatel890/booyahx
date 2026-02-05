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

        // Check if this request should skip the loader
        boolean shouldSkipLoader = shouldSkipLoader(request);

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
     * Determine if the loader should be skipped for this request
     */
    private boolean shouldSkipLoader(Request request) {
        String url = request.url().toString();

        // Skip loader for withdraw-limit endpoint
        if (url.contains("/api/wallet/withdraw-limit")) {
            return true;
        }

        // Add more endpoints here if needed in the future
        if (url.contains("/api/wallet/balance ")) {
        //     return true;
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