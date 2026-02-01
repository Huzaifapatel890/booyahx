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

        // Increment active requests and show loader
        int count = activeRequests.incrementAndGet();
        if (count == 1) {
            showLoader();
        }

        Response response = null;
        try {
            response = chain.proceed(request);
            return response;
        } finally {
            // Decrement active requests and hide loader if no more requests
            int remainingCount = activeRequests.decrementAndGet();
            if (remainingCount == 0) {
                hideLoader();
            }
        }
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