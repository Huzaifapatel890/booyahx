package com.booyahx.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.booyahx.utils.GlobalLoadingDialog;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import android.content.Context;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GlobalLoadingInterceptor implements Interceptor {

    private static final String TAG = "LoadingInterceptor_DEBU";

    private FragmentActivity activity;
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GlobalLoadingInterceptor(FragmentActivity activity) {
        Log.d(TAG, "🟢 Constructor called with : " + (activity != null ? activity.getClass().getSimpleName() : "NULL"));
        this.activity = activity;
    }

    public void updateActivity(FragmentActivity newActivity) {
        Log.d(TAG, "🟢 updateActivity() from " +
                (activity != null ? activity.getClass().getSimpleName() : "NULL") +
                " to " +
                (newActivity != null ? newActivity.getClass().getSimpleName() : "NULL"));
        this.activity = newActivity;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();

        Log.d(TAG, "🌐 ========================================");
        Log.d(TAG, "🌐 API REQUEST: " + request.method() + " " + url);
        Log.d(TAG, "   activity: " + (activity != null ? activity.getClass().getSimpleName() : "NULL"));

        // ✅ Check if this is a login/auth request - skip loader for these
        boolean isLoginRequest = url.contains("/login") ||
                url.contains("/auth/google") ||
                url.contains("/auth/csrf") ||
                url.contains("/register") ||
                url.contains("/forgot-password");

        if (isLoginRequest) {
            Log.d(TAG, "   🔓 LOGIN/AUTH REQUEST - SKIPPING LOADER");
        }

        // Only manage loader for non-login requests
        if (!isLoginRequest) {
            // Increment active requests and show loader
            int count = activeRequests.incrementAndGet();
            Log.d(TAG, "   📈 activeRequests incremented: " + count);

            if (count == 1) {
                Log.d(TAG, "   🔄 First request - SHOWING LOADER");
                showLoader();
            } else {
                Log.d(TAG, "   ℹ️ Already have " + count + " active requests");
            }
        }

        Response response = null;
        try {
            response = chain.proceed(request);
            Log.d(TAG, "   ✅ Response received: " + response.code());
            return response;
        } catch (IOException e) {
            Log.e(TAG, "   ❌ Request failed: " + e.getMessage());
            throw e;
        } finally {
            // Only manage loader for non-login requests
            if (!isLoginRequest) {
                // Decrement active requests and hide loader if no more requests
                int remainingCount = activeRequests.decrementAndGet();
                Log.d(TAG, "   📉 activeRequests decremented: " + remainingCount);

                if (remainingCount == 0) {
                    Log.d(TAG, "   ✅ No more requests - HIDING LOADER");
                    hideLoader();
                } else {
                    Log.d(TAG, "   ℹ️ Still have " + remainingCount + " active requests");
                }
            }
            Log.d(TAG, "🌐 ========================================");
        }
    }

    private void showLoader() {
        Log.d(TAG, "   🔵 showLoader() called");
        Log.d(TAG, "      activity: " + (activity != null ? activity.getClass().getSimpleName() : "NULL"));

        if (activity == null) {
            Log.e(TAG, "      ❌ CANNOT SHOW LOADER - activity is NULL!");
            return;
        }

        Log.d(TAG, "      isFinishing: " + activity.isFinishing());
        Log.d(TAG, "      isDestroyed: " + activity.isDestroyed());

        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "      ❌ CANNOT SHOW LOADER - activity is finishing/destroyed!");
            return;
        }

        mainHandler.post(() -> {
            Log.d(TAG, "      🎯 mainHandler.post() executing on UI thread");
            Log.d(TAG, "         activity: " + (activity != null ? activity.getClass().getSimpleName() : "NULL"));

            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                try {
                    Log.d(TAG, "         📞 Calling GlobalLoadingDialog.show()");
                    GlobalLoadingDialog.show(activity.getSupportFragmentManager());
                    Log.d(TAG, "         ✅ GlobalLoadingDialog.show() completed");
                } catch (Exception e) {
                    Log.e(TAG, "         ❌ FAILED to show loader: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "         ❌ Activity became invalid before showing loader");
            }
        });
    }

    private void hideLoader() {
        Log.d(TAG, "   🔴 hideLoader() called");
        Log.d(TAG, "      activity: " + (activity != null ? activity.getClass().getSimpleName() : "NULL"));

        if (activity == null) {
            Log.e(TAG, "      ⚠️ activity is NULL, cannot hide loader");
            return;
        }

        Log.d(TAG, "      isFinishing: " + activity.isFinishing());
        Log.d(TAG, "      isDestroyed: " + activity.isDestroyed());

        // Don't try to hide if activity is finishing or destroyed
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "      ⚠️ Activity is finishing/destroyed, skipping hide");
            return;
        }

        mainHandler.post(() -> {
            Log.d(TAG, "      🎯 mainHandler.post() executing on UI thread");
            Log.d(TAG, "         activity: " + (activity != null ? activity.getClass().getSimpleName() : "NULL"));

            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                try {
                    Log.d(TAG, "         📞 Calling GlobalLoadingDialog.hide()");
                    GlobalLoadingDialog.hide(activity.getSupportFragmentManager());
                    Log.d(TAG, "         ✅ GlobalLoadingDialog.hide() completed");
                } catch (Exception e) {
                    Log.e(TAG, "         ❌ FAILED to hide loader: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "         ⚠️ Activity became invalid before hiding loader");
            }
        });
    }
}