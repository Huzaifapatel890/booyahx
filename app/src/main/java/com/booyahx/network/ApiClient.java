package com.booyahx.network;

import android.content.Context;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String TAG = "ApiClient_DEBUG";

    private static Retrofit api;
    private static Retrofit refresh;
    private static GlobalLoadingInterceptor loadingInterceptor;
    private static FragmentActivity currentActivity;

    private static final String BASE_URL = "https://api.gaminghuballday.buzz";

    /**
     * Initialize the API client with global loading
     * Call this from your MainActivity or Application class
     */
    public static void initialize(FragmentActivity activity) {
        Log.d(TAG, "🟢 initialize() called with activity: " + (activity != null ? activity.getClass().getSimpleName() : "NULL"));
        currentActivity = activity;
        loadingInterceptor = new GlobalLoadingInterceptor(activity);
        Log.d(TAG, "✅ loadingInterceptor created: " + (loadingInterceptor != null));
        Log.d(TAG, "✅ currentActivity stored: " + (currentActivity != null));
    }

    /**
     * Update the activity reference (call when switching activities)
     */
    public static void updateActivity(FragmentActivity newActivity) {
        Log.d(TAG, "🟢 updateActivity() called with: " + (newActivity != null ? newActivity.getClass().getSimpleName() : "NULL"));
        Log.d(TAG, "   Current state - loadingInterceptor: " + (loadingInterceptor != null) + ", currentActivity: " + (currentActivity != null));

        currentActivity = newActivity;

        if (loadingInterceptor != null) {
            Log.d(TAG, "   ✅ Updating existing interceptor");
            loadingInterceptor.updateActivity(newActivity);
        } else if (currentActivity != null) {
            Log.d(TAG, "   🔄 Recreating interceptor (was null)");
            loadingInterceptor = new GlobalLoadingInterceptor(currentActivity);
        } else {
            Log.e(TAG, "   ❌ Cannot create interceptor - activity is null!");
        }

        Log.d(TAG, "   Final state - loadingInterceptor: " + (loadingInterceptor != null));
    }

    public static Retrofit getClient(Context ctx) {
        Log.d(TAG, "🟢 getClient() called");
        Log.d(TAG, "   api instance: " + (api != null ? "EXISTS" : "NULL"));
        Log.d(TAG, "   loadingInterceptor: " + (loadingInterceptor != null ? "EXISTS" : "NULL"));
        Log.d(TAG, "   currentActivity: " + (currentActivity != null ? currentActivity.getClass().getSimpleName() : "NULL"));

        if (api == null) {
            Log.d(TAG, "🔨 Building new Retrofit instance...");

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .cookieJar(CookieStore.getInstance())
                    .addInterceptor(new CsrfResponseInterceptor(ctx))
                    .addInterceptor(new AuthInterceptor(ctx))
                    .addInterceptor(new TokenRefreshInterceptor(ctx))
                    .addInterceptor(log)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            // 🔥 RECREATE LOADING INTERCEPTOR IF NEEDED
            if (loadingInterceptor == null && currentActivity != null) {
                Log.d(TAG, "   🔄 loadingInterceptor was null, recreating...");
                loadingInterceptor = new GlobalLoadingInterceptor(currentActivity);
                Log.d(TAG, "   ✅ loadingInterceptor recreated: " + (loadingInterceptor != null));
            }

            // ✅ ADD GLOBAL LOADING INTERCEPTOR
            if (loadingInterceptor != null) {
                Log.d(TAG, "   ✅ ADDING loadingInterceptor to OkHttpClient");
                clientBuilder.addInterceptor(loadingInterceptor);
            } else {
                Log.e(TAG, "   ❌❌❌ CRITICAL: loadingInterceptor is NULL - LOADER WILL NOT WORK!");
                Log.e(TAG, "   ❌ currentActivity: " + (currentActivity != null ? "exists" : "NULL"));
                Log.e(TAG, "   ❌ Did you call ApiClient.initialize() or updateActivity()?");
            }

            OkHttpClient client = clientBuilder.build();
            Log.d(TAG, "   ✅ OkHttpClient built with " + client.interceptors().size() + " interceptors");

            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "   ✅ Retrofit instance created");
        } else {
            Log.d(TAG, "   ℹ️ Returning existing Retrofit instance");
        }

        return api;
    }

    public static Retrofit getRefreshClient() {
        Log.d(TAG, "🟢 getRefreshClient() called (NO LOADER on this client)");

        if (refresh == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient refreshClient = new OkHttpClient.Builder()
                    .cookieJar(CookieStore.getInstance())
                    .addInterceptor(log)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            refresh = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(refreshClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return refresh;
    }

    public static void reset() {
        Log.d(TAG, "🔴 reset() called - CLEARING ALL INSTANCES");
        Log.d(TAG, "   Before reset - api: " + (api != null) + ", refresh: " + (refresh != null) + ", loadingInterceptor: " + (loadingInterceptor != null));

        api = null;
        refresh = null;
        loadingInterceptor = null;

        Log.d(TAG, "   After reset - currentActivity preserved: " + (currentActivity != null ? currentActivity.getClass().getSimpleName() : "NULL"));
        Log.d(TAG, "   ⚠️ Next API call will need to recreate loadingInterceptor");
    }
}