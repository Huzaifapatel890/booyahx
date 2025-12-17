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

    private static final String BASE_URL =
            "https://api.gaminghuballday.buzz";

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
            refresh = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(new OkHttpClient.Builder()
                            .cookieJar(CookieStore.getInstance())
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return refresh;
    }
}