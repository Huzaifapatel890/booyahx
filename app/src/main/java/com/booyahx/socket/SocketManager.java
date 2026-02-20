package com.booyahx.socket;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.booyahx.MyApp;
import com.booyahx.TokenManager;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.RefreshRequest;
import com.booyahx.network.models.RefreshResponse;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Centralized Socket Manager for WebSocket connections
 * Handles all socket lifecycle, events, and broadcasting
 */
public class SocketManager {

    private static final String TAG = "SocketManager";
    private static final String SOCKET_URL = "wss://api.gaminghuballday.buzz";

    private static SocketManager instance;
    private static Socket socket;
    private static String currentUserId = null;
    private static boolean isSubscribed = false;

    /**
     * Private constructor for singleton pattern
     */
    private SocketManager() {
        // Private constructor to prevent direct instantiation
    }

    /**
     * Get singleton instance of SocketManager
     * @return SocketManager instance
     */
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * Initialize socket connection with token
     * This only creates the socket instance, call connect() to actually connect
     */
    public static Socket getSocket(String token) {
        if (socket == null) {
            try {
                Log.d(TAG, "============================================");
                Log.d(TAG, "Initializing socket...");
                Log.d(TAG, "Server URL: " + SOCKET_URL);

                IO.Options options = new IO.Options();
                options.auth = new java.util.HashMap<>();
                options.auth.put("token", token);

                // Socket.IO configuration
                options.reconnection = true;
                options.reconnectionAttempts = Integer.MAX_VALUE;
                options.reconnectionDelay = 1000;
                options.reconnectionDelayMax = 5000;
                options.timeout = 20000;

                socket = IO.socket(SOCKET_URL, options);

                // Attach global listeners only once
                attachGlobalListeners();

                Log.d(TAG, "✅ Socket initialized successfully");
                Log.d(TAG, "============================================");
            } catch (Exception e) {
                Log.e(TAG, "============================================");
                Log.e(TAG, "❌ Socket initialization FAILED");
                Log.e(TAG, "Error: " + e.getMessage());
                Log.e(TAG, "============================================");
                e.printStackTrace();
            }
        }
        return socket;
    }

    /**
     * Connect socket (called from MyApp.onStart or DashboardActivity.onCreate)
     */
    public static void connect() {
        if (socket == null) {
            Log.e(TAG, "❌ Cannot connect: Socket is null. Call getSocket() first.");
            return;
        }

        if (socket.connected()) {
            Log.d(TAG, "✅ Socket already connected");
            Log.d(TAG, "Socket ID: " + socket.id());
            return;
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "Connecting socket...");
        Log.d(TAG, "Target: " + SOCKET_URL);
        socket.connect();
        Log.d(TAG, "Connection initiated");
        Log.d(TAG, "============================================");
    }

    /**
     * Disconnect socket (called from MyApp.onStop)
     */
    public static void disconnect() {
        if (socket == null) {
            Log.d(TAG, "Socket is null, nothing to disconnect");
            return;
        }

        if (!socket.connected()) {
            Log.d(TAG, "Socket already disconnected");
            return;
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "Disconnecting socket...");

        // Unsubscribe before disconnecting
        unsubscribe();

        socket.disconnect();
        Log.d(TAG, "✅ Socket disconnected");
        Log.d(TAG, "============================================");
    }

    /**
     * Subscribe to user-specific events (ONLY called from DashboardActivity)
     */
    public static void subscribe(String userId) {
        if (socket == null) {
            Log.e(TAG, "❌ Cannot subscribe: socket is null");
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ Cannot subscribe: userId is null or empty");
            return;
        }

        // Prevent duplicate subscriptions
        if (isSubscribed && userId.equals(currentUserId)) {
            Log.d(TAG, "Already subscribed for user: " + userId);
            return;
        }

        // Unsubscribe from previous user if different
        if (isSubscribed && !userId.equals(currentUserId)) {
            Log.d(TAG, "Different user detected, unsubscribing previous user");
            unsubscribe();
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "Subscribing to events...");
        Log.d(TAG, "User ID: " + userId);

        currentUserId = userId;
        isSubscribed = true;

        // Subscribe to wallet updates
        socket.emit("subscribe:wallet", userId);
        Log.d(TAG, "✅ Emitted: subscribe:wallet");

        // Subscribe to user-specific tournament updates
        socket.emit("subscribe:user-tournaments", userId);
        Log.d(TAG, "✅ Emitted: subscribe:user-tournaments");

        Log.d(TAG, "✅ Subscription complete");
        Log.d(TAG, "============================================");

        // Re-subscribe on reconnection
        socket.off(Socket.EVENT_CONNECT);
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "Socket reconnected, re-subscribing...");
            socket.emit("subscribe:wallet", currentUserId);
            socket.emit("subscribe:user-tournaments", currentUserId);
            Log.d(TAG, "✅ Re-subscription complete");
            // ✅ GAP 4 FIX: Broadcast reconnect so ParticipatedFragment re-fetches
            // joined tournaments via REST to recover any events missed while offline.
            sendBroadcast("SOCKET_RECONNECTED");
            Log.d(TAG, "✅ SOCKET_RECONNECTED broadcast sent — fragments will re-fetch missed data");
            Log.d(TAG, "============================================");
        });
    }

    /**
     * ✅ GAP 1 FIX: Subscribe to a specific tournament room on the server.
     * Call this from ParticipatedFragment after the joined tournament list loads,
     * for every tournament in the list. Backend routes room-credential push events
     * to participants via 'subscribe:tournament', so without this emit the app
     * never receives room updates for that specific tournament room channel.
     *
     * @param tournamentId The tournament ID to subscribe to
     */
    public static void subscribeToTournament(String tournamentId) {
        if (socket == null) {
            Log.w(TAG, "⚠️ subscribeToTournament: socket is null, skipping: " + tournamentId);
            return;
        }
        if (tournamentId == null || tournamentId.isEmpty()) {
            Log.w(TAG, "⚠️ subscribeToTournament: tournamentId is null/empty, skipping");
            return;
        }
        socket.emit("subscribe:tournament", tournamentId);
        Log.d(TAG, "✅ Emitted: subscribe:tournament → " + tournamentId);
    }

    /**
     * Unsubscribe from user-specific events
     */
    public static void unsubscribe() {
        if (socket == null) {
            Log.d(TAG, "Socket is null, nothing to unsubscribe");
            return;
        }

        if (!isSubscribed) {
            Log.d(TAG, "Not subscribed, nothing to unsubscribe");
            return;
        }

        if (currentUserId == null) {
            Log.w(TAG, "currentUserId is null, cannot unsubscribe properly");
            isSubscribed = false;
            return;
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "Unsubscribing from events...");
        Log.d(TAG, "User ID: " + currentUserId);

        socket.emit("unsubscribe:wallet", currentUserId);
        Log.d(TAG, "✅ Emitted: unsubscribe:wallet");

        socket.emit("unsubscribe:tournament", currentUserId);
        Log.d(TAG, "✅ Emitted: unsubscribe:tournament");

        isSubscribed = false;
        currentUserId = null;

        Log.d(TAG, "✅ Unsubscription complete");
        Log.d(TAG, "============================================");
    }

    /**
     * Attach global server event listeners (called once during initialization)
     */
    private static void attachGlobalListeners() {
        Log.d(TAG, "Attaching global event listeners...");

        // ========== CONNECTION EVENTS ==========

        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "✅✅✅ Socket CONNECTED successfully ✅✅✅");
            Log.d(TAG, "Socket ID: " + socket.id());
            Log.d(TAG, "Connected to: " + SOCKET_URL);
            Log.d(TAG, "============================================");
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "❌ Socket DISCONNECTED");
            if (args.length > 0) {
                Log.d(TAG, "Reason: " + args[0]);
            }
            Log.d(TAG, "============================================");
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(TAG, "============================================");
            Log.e(TAG, "❌❌❌ Socket CONNECTION ERROR ❌❌❌");
            if (args.length > 0) {
                Log.e(TAG, "Error: " + args[0]);
                if (args[0] instanceof Exception) {
                    ((Exception) args[0]).printStackTrace();
                }
            }
            Log.e(TAG, "============================================");
        });

        // ========== WALLET EVENTS ==========

        socket.on("wallet:balance-updated", args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "📨 Received: wallet:balance-updated");
            if (args.length > 0) {
                Log.d(TAG, "Data: " + args[0].toString());
            }
            Log.d(TAG, "Broadcasting to app...");
            sendBroadcast("WALLET_UPDATED", args);
            Log.d(TAG, "============================================");
        });

        socket.on("wallet:transaction-updated", args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "📨 Received: wallet:transaction-updated");
            if (args.length > 0) {
                Log.d(TAG, "Data: " + args[0].toString());
            }
            Log.d(TAG, "Broadcasting to app...");
            sendBroadcast("WALLET_UPDATED", args);
            Log.d(TAG, "============================================");
        });

        socket.on("wallet:history-updated", args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "📨 Received: wallet:history-updated");
            if (args.length > 0) {
                Log.d(TAG, "Data: " + args[0].toString());
            }
            Log.d(TAG, "Broadcasting to app...");
            sendBroadcast("WALLET_UPDATED", args);
            Log.d(TAG, "============================================");
        });

        socket.on("payment:qr-status-updated", args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "📨 Received: payment:qr-status-updated");
            if (args.length > 0) {
                Log.d(TAG, "Data: " + args[0].toString());
            }
            Log.d(TAG, "Broadcasting to app...");
            sendBroadcast("PAYMENT_QR_UPDATED", args);
            Log.d(TAG, "============================================");
        });

        // ========== TOURNAMENT EVENTS ==========

        socket.on("tournament:room-updated", args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "📨 Received: tournament:room-updated");
            if (args.length > 0) {
                Log.d(TAG, "Data: " + args[0].toString());
            }
            Log.d(TAG, "Broadcasting to app...");
            sendBroadcast("TOURNAMENT_ROOM_UPDATED", args);
            Log.d(TAG, "============================================");
        });

        socket.on("tournament:status-updated", args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "📨 Received: tournament:status-updated");
            if (args.length > 0) {
                Log.d(TAG, "Data: " + args[0].toString());
            }
            Log.d(TAG, "Broadcasting to app...");
            sendBroadcast("TOURNAMENT_STATUS_UPDATED", args);
            Log.d(TAG, "============================================");
        });

        // ========== NOTIFICATION PUSH ==========
        // ✅ GAP 3 FIX: notification:push was listed in backend docs but never listened to here.
        // Admin / backend can push arbitrary notifications to subscribed users via this event.
        // We broadcast it as NOTIFICATION_PUSH so DashboardActivity can show the banner
        // and persist it to NotificationManager for NotificationActivity.
        socket.on("notification:push", args -> {
            Log.d(TAG, "============================================");
            Log.d(TAG, "📨 Received: notification:push");
            if (args.length > 0) {
                Log.d(TAG, "Data: " + args[0].toString());
            }
            Log.d(TAG, "Broadcasting to app...");
            sendBroadcast("NOTIFICATION_PUSH", args);
            Log.d(TAG, "============================================");
        });

        // ========== LOBBY CHAT EVENTS ==========
        // NOTE: lobby-chat:message, lobby-chat:error, lobby-chat:closed are NOT registered here.
        // They are registered per-activity by TournamentChatActivity via onNewMessage(),
        // onChatError(), and onChatClosed() to avoid listener conflicts.

        Log.d(TAG, "✅ Global event listeners attached");
    }

    /**
     * Send local broadcast with optional data
     */
    private static void sendBroadcast(String action, Object... args) {
        Intent intent = new Intent(action);

        // ✅ USER ISOLATION FIX (Option B): Tag every broadcast with the currently
        // subscribed userId so DashboardActivity can drop events meant for other users.
        // currentUserId is set in subscribe() and cleared in unsubscribe(), so it always
        // reflects the user whose socket room emitted this event.
        if (currentUserId != null) {
            intent.putExtra("targetUserId", currentUserId);
            Log.d(TAG, "Broadcast tagged for userId: " + currentUserId);
        }

        // Attach data if available
        if (args != null && args.length > 0 && args[0] instanceof JSONObject) {
            try {
                JSONObject data = (JSONObject) args[0];
                intent.putExtra("data", data.toString());
                Log.d(TAG, "Broadcast sent: " + action + " with data");
            } catch (Exception e) {
                Log.e(TAG, "Error parsing broadcast data", e);
            }
        } else {
            Log.d(TAG, "Broadcast sent: " + action + " (no data)");
        }

        LocalBroadcastManager
                .getInstance(MyApp.getInstance())
                .sendBroadcast(intent);
    }

    /**
     * Get current socket instance (overloaded method)
     */
    public static Socket getSocket() {
        return socket;
    }

    // ========== TOURNAMENT CHAT METHODS (UPDATED) ==========

    /**
     * 🔥 FIX: Synchronously refresh the access token before joining chat.
     * The TokenRefreshInterceptor only works for HTTP calls, not WebSocket emits.
     * So we manually refresh here to ensure a fresh token is in the payload.
     */
    private static String getFreshToken() {
        try {
            String refreshToken = TokenManager.getRefreshToken(MyApp.getInstance());
            if (refreshToken == null) {
                Log.e(TAG, "⚠️ No refresh token available, using existing access token");
                return TokenManager.getAccessToken(MyApp.getInstance());
            }

            Log.d(TAG, "🔄 Refreshing token before joining lobby chat...");

            ApiService refreshApi = ApiClient.getRefreshClient().create(ApiService.class);
            retrofit2.Response<RefreshResponse> r = refreshApi
                    .refreshToken(new RefreshRequest(refreshToken))
                    .execute();

            if (r.isSuccessful() && r.body() != null
                    && r.body().success && r.body().data != null) {

                String newAccessToken = r.body().data.accessToken;
                String newRefreshToken = r.body().data.refreshToken;

                TokenManager.saveTokens(MyApp.getInstance(), newAccessToken, newRefreshToken);

                // Save new CSRF if present
                String newCsrf = r.headers().get("x-csrf-token");
                if (newCsrf == null || newCsrf.isEmpty()) {
                    newCsrf = r.headers().get("X-CSRF-Token");
                }
                if (newCsrf != null && !newCsrf.isEmpty()) {
                    TokenManager.saveCsrf(MyApp.getInstance(), newCsrf);
                }

                Log.d(TAG, "✅ Token refreshed successfully before joining chat");
                return newAccessToken;

            } else {
                Log.e(TAG, "⚠️ Token refresh failed, using existing token");
                return TokenManager.getAccessToken(MyApp.getInstance());
            }

        } catch (Exception e) {
            Log.e(TAG, "⚠️ Exception during token refresh: " + e.getMessage());
            return TokenManager.getAccessToken(MyApp.getInstance());
        }
    }

    /**
     * 🔥 FIX: Recreate the socket with a fresh token so the handshake auth is up-to-date.
     * The existing socket's auth cannot be mutated after the handshake, so we must
     * tear it down and rebuild it. Global listeners are re-attached automatically.
     * Subscriptions (wallet, tournaments) are NOT re-emitted here — that is handled
     * by the EVENT_CONNECT listener already registered inside subscribe().
     */
    private static void recreateSocketWithFreshToken(String freshToken) {
        Log.d(TAG, "============================================");
        Log.d(TAG, "🔄 Recreating socket with fresh token...");

        // Tear down the existing socket cleanly
        if (socket != null) {
            socket.off(); // remove all listeners to avoid leaks
            socket.disconnect();
            socket = null;
            Log.d(TAG, "Old socket torn down");
        }

        try {
            IO.Options options = new IO.Options();
            options.auth = new java.util.HashMap<>();
            options.auth.put("token", freshToken);

            options.reconnection = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay = 1000;
            options.reconnectionDelayMax = 5000;
            options.timeout = 20000;

            socket = IO.socket(SOCKET_URL, options);
            attachGlobalListeners();

            // Re-register subscription reconnect listener if we had an active user
            if (currentUserId != null) {
                isSubscribed = false; // force re-subscription on connect
                final String uid = currentUserId;
                socket.once(Socket.EVENT_CONNECT, args -> {
                    Log.d(TAG, "🔄 Re-subscribing after socket recreate for user: " + uid);
                    socket.emit("subscribe:wallet", uid);
                    socket.emit("subscribe:user-tournaments", uid);
                    isSubscribed = true;
                });
            }

            socket.connect();
            Log.d(TAG, "✅ New socket created and connecting with fresh token");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to recreate socket: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "============================================");
    }

    /**
     * 🔥 UPDATED: Subscribe to lobby chat (join tournament chat room)
     * Recreates the socket with a fresh token BEFORE joining so the backend's
     * handshake auth check always sees a valid token.
     * Matches backend: subscribe:lobby-chat
     * @param tournamentId The tournament ID
     * @param userId The user ID (optional, for participants)
     * @param username The username
     */
    public void joinTournamentRoom(String tournamentId, String userId, String username) {
        try {
            // 🔥 THE REAL FIX: Refresh the token first, then recreate the socket so the
            // handshake auth carries the fresh token. The old socket's auth is stale after
            // a token refresh — the backend will reject subscribe:lobby-chat with that auth.
            String freshToken = getFreshToken();
            recreateSocketWithFreshToken(freshToken);

            // Wait for the new socket to connect before emitting
            socket.once(Socket.EVENT_CONNECT, args -> {
                try {
                    JSONObject data = new JSONObject();
                    data.put("tournamentId", tournamentId);
                    data.put("userId", userId);
                    data.put("username", username);
                    if (freshToken != null) {
                        data.put("token", freshToken);
                        Log.d(TAG, "✅ Fresh auth token added to payload");
                    }
                    Log.d(TAG, "============================================");
                    Log.d(TAG, "🔵 JOINING LOBBY CHAT");
                    Log.d(TAG, "Tournament ID: " + tournamentId);
                    Log.d(TAG, "User ID: " + userId);
                    Log.d(TAG, "Username: " + username);
                    Log.d(TAG, "============================================");

                    socket.emit("subscribe:lobby-chat", data);

                    Log.d(TAG, "✅ Emitted: subscribe:lobby-chat");
                    Log.d(TAG, "   Payload: " + data.toString());
                    Log.d(TAG, "   Socket ID: " + socket.id());
                    Log.d(TAG, "   Socket Connected: " + socket.connected());
                    Log.d(TAG, "============================================");
                } catch (Exception e) {
                    Log.e(TAG, "============================================");
                    Log.e(TAG, "❌ ERROR joining tournament room");
                    Log.e(TAG, "Exception: " + e.getMessage());
                    e.printStackTrace();
                    Log.e(TAG, "============================================");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "============================================");
            Log.e(TAG, "❌ ERROR setting up joinTournamentRoom");
            Log.e(TAG, "Exception: " + e.getMessage());
            e.printStackTrace();
            Log.e(TAG, "============================================");
        }
    }

    /**
     * 🔥 UPDATED: Unsubscribe from lobby chat (leave tournament chat room)
     * Matches backend: unsubscribe:lobby-chat
     * @param tournamentId The tournament ID
     * @param userId The user ID
     */
    public void leaveTournamentRoom(String tournamentId, String userId) {
        if (socket == null) {
            Log.e(TAG, "Cannot leave tournament room - socket is null");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("tournamentId", tournamentId);
            data.put("userId", userId);

            Log.d(TAG, "============================================");
            Log.d(TAG, "🔵 LEAVING LOBBY CHAT");
            Log.d(TAG, "Tournament ID: " + tournamentId);
            Log.d(TAG, "User ID: " + userId);
            Log.d(TAG, "============================================");

            // 🔥 FIX: Send data object with user info
            socket.emit("unsubscribe:lobby-chat", data);

            Log.d(TAG, "✅ Emitted: unsubscribe:lobby-chat");
            Log.d(TAG, "   Payload: " + data.toString());
            Log.d(TAG, "============================================");
        } catch (Exception e) {
            Log.e(TAG, "Error leaving tournament room", e);
        }
    }

    /**
     * 🔥 UPDATED: Send a message in tournament lobby chat
     * Matches backend: lobby-chat:send-message
     * @param tournamentId The tournament ID
     * @param message The message text
     */
    public void sendMessage(String tournamentId, String userId, String username, String message, boolean isHost) {
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "❌ Cannot send message - socket not connected");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("tournamentId", tournamentId);
            data.put("message", message);

            Log.d(TAG, "============================================");
            Log.d(TAG, "💬 SENDING MESSAGE");
            Log.d(TAG, "Tournament ID: " + tournamentId);
            Log.d(TAG, "Message: " + message);
            Log.d(TAG, "Socket ID: " + socket.id());
            Log.d(TAG, "Socket Connected: " + socket.connected());
            Log.d(TAG, "============================================");

            // Emit lobby-chat:send-message with { tournamentId, message }
            socket.emit("lobby-chat:send-message", data);

            Log.d(TAG, "✅ Emitted: lobby-chat:send-message");
            Log.d(TAG, "   Payload: " + data.toString());
            Log.d(TAG, "============================================");
        } catch (Exception e) {
            Log.e(TAG, "============================================");
            Log.e(TAG, "❌ ERROR sending message");
            Log.e(TAG, "Exception: " + e.getMessage());
            e.printStackTrace();
            Log.e(TAG, "============================================");
        }
    }

    /**
     * 🔥 UPDATED: Listen for new messages
     * Matches backend: lobby-chat:message
     * @param listener The listener callback
     */
    public void onNewMessage(io.socket.emitter.Emitter.Listener listener) {
        if (socket == null) {
            Log.e(TAG, "Cannot listen for messages - socket is null");
            return;
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "🎧 SETTING UP MESSAGE LISTENER");
        Log.d(TAG, "Event: lobby-chat:message");
        Log.d(TAG, "============================================");

        // Remove any existing listener first to prevent duplicates
        socket.off("lobby-chat:message");
        socket.on("lobby-chat:message", listener);

        Log.d(TAG, "✅ Listening for: lobby-chat:message");
    }

    /**
     * Listen for message history in tournament chat
     * @param listener The listener callback
     */
    public void onMessageHistory(io.socket.emitter.Emitter.Listener listener) {
        if (socket == null) {
            Log.e(TAG, "Cannot listen for message history - socket is null");
            return;
        }

        // Remove any existing listener first to prevent duplicates
        socket.off("tournament:message-history");
        socket.on("tournament:message-history", listener);
        Log.d(TAG, "✅ Listening for message history");
    }

    /**
     * Listen for user joined event
     * @param listener The listener callback
     */
    public void onUserJoined(io.socket.emitter.Emitter.Listener listener) {
        if (socket == null) {
            Log.e(TAG, "Cannot listen for user joined - socket is null");
            return;
        }

        // Remove any existing listener first to prevent duplicates
        socket.off("tournament:user-joined");
        socket.on("tournament:user-joined", listener);
        Log.d(TAG, "✅ Listening for user joined events");
    }

    /**
     * Listen for user left event
     * @param listener The listener callback
     */
    public void onUserLeft(io.socket.emitter.Emitter.Listener listener) {
        if (socket == null) {
            Log.e(TAG, "Cannot listen for user left - socket is null");
            return;
        }

        // Remove any existing listener first to prevent duplicates
        socket.off("tournament:user-left");
        socket.on("tournament:user-left", listener);
        Log.d(TAG, "✅ Listening for user left events");
    }

    /**
     * 🔥 UPDATED: Listen for lobby chat errors
     * @param listener The listener callback
     */
    public void onChatError(io.socket.emitter.Emitter.Listener listener) {
        if (socket == null) {
            Log.e(TAG, "Cannot listen for chat errors - socket is null");
            return;
        }

        socket.off("lobby-chat:error");
        socket.on("lobby-chat:error", listener);
        Log.d(TAG, "✅ Listening for: lobby-chat:error");
    }

    /**
     * 🔥 UPDATED: Listen for lobby chat closed event
     * @param listener The listener callback
     */
    public void onChatClosed(io.socket.emitter.Emitter.Listener listener) {
        if (socket == null) {
            Log.e(TAG, "Cannot listen for chat closed - socket is null");
            return;
        }

        socket.off("lobby-chat:closed");
        socket.on("lobby-chat:closed", listener);
        Log.d(TAG, "✅ Listening for: lobby-chat:closed");
    }

    /**
     * Remove all tournament chat listeners
     */
    public void removeAllChatListeners() {
        if (socket == null) {
            Log.e(TAG, "Cannot remove chat listeners - socket is null");
            return;
        }

        socket.off("lobby-chat:message");
        socket.off("lobby-chat:error");
        socket.off("lobby-chat:closed");
        socket.off("tournament:message-history");
        socket.off("tournament:user-joined");
        socket.off("tournament:user-left");
        Log.d(TAG, "✅ Removed all tournament chat listeners");
    }

    /**
     * Remove all listeners (alias for compatibility)
     * This removes only chat-related listeners
     */
    public void removeAllListeners() {
        removeAllChatListeners();
    }
}