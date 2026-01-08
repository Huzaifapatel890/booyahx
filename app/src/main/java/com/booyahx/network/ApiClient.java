package com.booyahx.network;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit api;
    private static Retrofit refresh;

    private static final String BASE_URL = "https://api.gaminghuballday.buzz";

    public static Retrofit getClient(Context ctx) {

        if (api == null) {

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(CookieStore.getInstance())
                    .addInterceptor(new AuthInterceptor(ctx))
                    .addInterceptor(new TokenRefreshInterceptor(ctx))
                    .addInterceptor(log)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

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

            // Add logging to debug refresh requests
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient refreshClient = new OkHttpClient.Builder()
                    .cookieJar(CookieStore.getInstance()) // ✅ CRITICAL: Must have cookies
                    .addInterceptor(log) // ✅ Add logging to see what's happening
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
    }
}