package com.booyahx.network;

import android.content.Context;
import android.util.Log;

import com.booyahx.TokenManager;

import java.io.IOException;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.Response;

public final class CsrfResponseInterceptor implements Interceptor {

    private static final String TAG = "CsrfResponse";
    private final Context context;

    public CsrfResponseInterceptor(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        String csrfToken = null;

        // 🔥 PRIORITY 1: Extract from response header (lowercase)
        csrfToken = response.header("x-csrf-token");

        // 🔥 PRIORITY 2: Try uppercase variant
        if (csrfToken == null || csrfToken.isEmpty()) {
            csrfToken = response.header("X-CSRF-Token");
        }

        // 🔥 PRIORITY 3: Extract from XSRF-TOKEN cookie
        if (csrfToken == null || csrfToken.isEmpty()) {
            List<Cookie> cookies = CookieStore.getInstance()
                    .loadForRequest(response.request().url());

            for (Cookie cookie : cookies) {
                if ("XSRF-TOKEN".equals(cookie.name())) {
                    csrfToken = cookie.value();
                    Log.d(TAG, "CSRF token extracted from cookie: " + csrfToken);
                    break;
                }
            }
        }

        if (csrfToken != null && !csrfToken.isEmpty()) {
            String currentCsrf = TokenManager.getCsrf(context);

            // Only save if it's different (to avoid unnecessary writes)
            if (!csrfToken.equals(currentCsrf)) {
                Log.d(TAG, "New CSRF token received from response: " + csrfToken);
                TokenManager.saveCsrf(context, csrfToken);
            }
        }

        return response;
    }
}