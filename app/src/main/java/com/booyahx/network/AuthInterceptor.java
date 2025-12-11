package com.booyahx.network;

import android.content.Context;

import com.booyahx.TokenManager;
import com.booyahx.network.models.RefreshRequest;
import com.booyahx.network.models.RefreshResponse;
import com.booyahx.utils.CSRFHelper;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

public class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String access = TokenManager.getAccessToken(context);

        Request original = chain.request();
        Request.Builder builder = original.newBuilder();

        // -------------------------------
        // ADD JWT
        // -------------------------------
        if (access != null) {
            builder.header("Authorization", "Bearer " + access);
        }

        // -------------------------------
        // ADD CSRF TOKEN  (🔥 YOUR MISSING PART)
        // -------------------------------
        String csrf = CSRFHelper.getToken(context);
        if (csrf != null && !csrf.isEmpty()) {
            builder.header("X-CSRF-Token", csrf);
        }

        // Proceed with request
        Response response = chain.proceed(builder.build());

        // -------------------------------
        // HANDLE 401 -> REFRESH TOKEN
        // -------------------------------
        if (response.code() == 401) {
            response.close();

            String refresh = TokenManager.getRefreshToken(context);
            if (refresh == null) return response;

            ApiService refreshApi = ApiClient.getRefreshInstance().create(ApiService.class);
            Call<RefreshResponse> call = refreshApi.refreshToken(new RefreshRequest(refresh));

            retrofit2.Response<RefreshResponse> refreshRes;

            try {
                refreshRes = call.execute();
            } catch (Exception e) {
                return response;
            }

            if (refreshRes.isSuccessful()
                    && refreshRes.body() != null
                    && refreshRes.body().success) {

                String newAccess = refreshRes.body().data.accessToken;
                String newRefresh = refreshRes.body().data.refreshToken;

                TokenManager.saveTokens(context, newAccess, newRefresh);

                Request newRequest = original.newBuilder()
                        .header("Authorization", "Bearer " + newAccess)
                        .header("X-CSRF-Token", csrf) // ADD AGAIN
                        .build();

                return chain.proceed(newRequest);
            }
        }

        return response;
    }
}