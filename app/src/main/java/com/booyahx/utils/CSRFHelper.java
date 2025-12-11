package com.booyahx.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.CsrfResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CSRFHelper {

    private static final String PREF_NAME = "auth";
    private static final String CSRF_KEY = "csrf";

    // ------------------------------------------------------------
    // SAVE TOKEN
    // ------------------------------------------------------------
    public static void saveToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(CSRF_KEY, token).apply();
    }

    // ------------------------------------------------------------
    // GET TOKEN
    // ------------------------------------------------------------
    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(CSRF_KEY, "");
    }

    // ------------------------------------------------------------
    // FETCH CSRF TOKEN FROM API
    // ------------------------------------------------------------
    public static void fetchToken(Context context, CSRFCallback callback) {

        // ✅ FIXED - USE AUTHINTERCEPTOR CLIENT
        ApiService api = ApiClient.getClient(context).create(ApiService.class);

        Call<CsrfResponse> call = api.getCsrfToken();

        call.enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call, Response<CsrfResponse> response) {

                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {

                    String token = response.body().getData().getCsrfToken();

                    saveToken(context, token);
                    callback.onSuccess(token);

                } else {
                    callback.onFailure("Failed to fetch CSRF token");
                }
            }

            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    // ------------------------------------------------------------
    // CALLBACK INTERFACE
    // ------------------------------------------------------------
    public interface CSRFCallback {
        void onSuccess(String token);
        void onFailure(String error);
    }
}