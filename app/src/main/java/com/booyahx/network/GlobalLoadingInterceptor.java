package com.booyahx.network;

import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;

import com.booyahx.utils.GlobalLoadingDialog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Global Loading Interceptor
 * Automatically shows/hides loading dialog for all API requests
 */
public class GlobalLoadingInterceptor implements Interceptor {

    private FragmentActivity activity;
    private Handler mainHandler;

    public GlobalLoadingInterceptor(FragmentActivity activity) {
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // Show loader on main thread BEFORE request
        mainHandler.post(() -> {
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                try {
                    GlobalLoadingDialog.getInstance().show(activity.getSupportFragmentManager());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Response response = null;
        try {
            // Execute the request
            response = chain.proceed(request);
        } finally {
            // Hide loader on main thread AFTER request completes
            mainHandler.post(() -> {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    try {
                        GlobalLoadingDialog.getInstance().hide();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        return response;
    }

    /**
     * Update the activity reference (useful when activity changes)
     */
    public void updateActivity(FragmentActivity newActivity) {
        this.activity = newActivity;
    }
}