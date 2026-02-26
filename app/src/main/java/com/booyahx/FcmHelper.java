package com.booyahx;

import android.content.Context;
import android.util.Log;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.FcmTokenRequest;
import com.booyahx.network.models.SimpleResponse;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper called immediately after login success (both Google and email/password).
 * Gets the FCM device token and POSTs it to /api/profile/fcm-token so the backend
 * can send push notifications to this device.
 */
public class FcmHelper {

    private static final String TAG = "FcmHelper";

    /**
     * Call this right after tokens are saved (post-login) from any login activity.
     * Fire-and-forget — does not block or affect the login flow.
     *
     * @param context Any context (LoginActivity, LoginUsernameActivity, etc.)
     */
    public static void saveTokenToServer(Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(fcmToken -> {
                    if (fcmToken == null || fcmToken.isEmpty()) {
                        Log.w(TAG, "⚠️ FCM token is null/empty, skipping save");
                        return;
                    }

                    Log.d(TAG, "✅ FCM token retrieved: " + fcmToken.substring(0, 20) + "...");

                    ApiService api = ApiClient.getSilentClient(context).create(ApiService.class);

                    api.saveFcmToken(new FcmTokenRequest(fcmToken))
                            .enqueue(new Callback<SimpleResponse>() {
                                @Override
                                public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                                    if (response.isSuccessful()) {
                                        Log.d(TAG, "✅ FCM token saved to backend successfully");
                                    } else {
                                        Log.w(TAG, "⚠️ FCM token save failed: HTTP " + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(Call<SimpleResponse> call, Throwable t) {
                                    Log.e(TAG, "❌ FCM token save network error: " + t.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get FCM token: " + e.getMessage());
                });
    }
}