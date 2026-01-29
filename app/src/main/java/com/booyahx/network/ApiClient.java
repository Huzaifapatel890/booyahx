package com.booyahx.network;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit api;
    private static Retrofit refresh;
    private static GlobalLoadingInterceptor loadingInterceptor;

    private static final String BASE_URL = "https://api.gaminghuballday.buzz";

    /**
     * Initialize the API client with global loading
     * Call this from your MainActivity or Application class
     */
    public static void initialize(FragmentActivity activity) {
        loadingInterceptor = new GlobalLoadingInterceptor(activity);
    }

    /**
     * Update the activity reference (call when switching activities)
     */
    public static void updateActivity(FragmentActivity newActivity) {
        if (loadingInterceptor != null) {
            loadingInterceptor.updateActivity(newActivity);
        }
    }

    public static Retrofit getClient(Context ctx) {

        if (api == null) {

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .cookieJar(CookieStore.getInstance())
                    // 🔥 CRITICAL: Add CSRF Response Interceptor FIRST
                    .addInterceptor(new CsrfResponseInterceptor(ctx))
                    // 🔥 Then request interceptors
                    .addInterceptor(new AuthInterceptor(ctx))
                    .addInterceptor(new TokenRefreshInterceptor(ctx))
                    .addInterceptor(log)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            // ✅ ADD GLOBAL LOADING INTERCEPTOR (if initialized)
            if (loadingInterceptor != null) {
                clientBuilder.addInterceptor(loadingInterceptor);
            }

            OkHttpClient client = clientBuilder.build();

            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return api;
    }

    public static Retrofit getRefreshClient() {

        if (refresh == null) {

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            // ❌ NO LOADING INTERCEPTOR for refresh client
            // (to avoid showing loader during token refresh)
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

    // Optional: Method to reset clients (useful for logout)
    public static void reset() {
        api = null;
        refresh = null;
        loadingInterceptor = null;
    }
}