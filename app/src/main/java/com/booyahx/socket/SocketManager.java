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

    // Stored chat listeners — re-applied after socket recreation
    private static io.socket.emitter.Emitter.Listener chatMessageListener = null;
    private static io.socket.emitter.Emitter.Listener chatErrorListener = null;
    private static io.socket.emitter.Emitter.Listener chatClosedListener = null;
    private static io.socket.emitter.Emitter.Listener userJoinedListener = null;
    private static io.socket.emitter.Emitter.Listener userLeftListener = null;

    // ✅ FIX: Named reference to the reconnect listener registered in subscribe().
    // Used to remove ONLY that specific listener on re-subscription, without
    // wiping other EVENT_CONNECT listeners (e.g. the lobby-chat one in joinTournamentRoom).
    private static io.socket.emitter.Emitter.Listener reconnectListener = null;

    // ── PERSISTENT LISTENER MAP ──────────────────────────────────────────────
    // Thread-safe map of all event → listener registrations made through the
    // public on*() helpers.  Populated BEFORE any null-socket early-exit so
    // listeners survive the window where the socket is null during recreation.
    // Key: Socket.IO event name.  Value: the Emitter.Listener to re-attach.
    private static final java.util.concurrent.ConcurrentHashMap<String, io.socket.emitter.Emitter.Listener>
            persistentListeners = new java.util.concurrent.ConcurrentHashMap<>();

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

        // Re-subscribe on reconnection.
        // ✅ FIX: Remove ONLY our previously registered reconnect listener (if any)
        // using socket.off(event, specificListener) instead of socket.off(event).
        // This preserves all other EVENT_CONNECT listeners — in particular the one
        // registered by joinTournamentRoom() that emits "subscribe:lobby-chat",
        // which is the root cause of the host not receiving lobby-chat messages.
        if (reconnectListener != null) {
            socket.off(Socket.EVENT_CONNECT, reconnectListener);
        }
        reconnectListener = args -> {
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
        };
        socket.on(Socket.EVENT_CONNECT, reconnectListener);
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

        if (socket.connected()) {
            socket.emit("subscribe:tournament", tournamentId);
            Log.d(TAG, "✅ Emitted immediately: subscribe:tournament → " + tournamentId);
        } else {
            Log.d(TAG, "⏳ Socket not connected, waiting to subscribe: " + tournamentId);

            socket.once(Socket.EVENT_CONNECT, args -> {
                socket.emit("subscribe:tournament", tournamentId);
                Log.d(TAG, "✅ Emitted after connect: subscribe:tournament → " + tournamentId);
            });
        }
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

    // ── PERSISTENT LISTENER REATTACHMENT ────────────────────────────────────
    /**
     * Reattach every listener stored in {@code persistentListeners} to the
     * current {@code socket} instance.  Called immediately after a new socket
     * is created so that no registered callback is ever silently dropped.
     *
     * Thread-safety: operates under the class monitor (static synchronized)
     * so it is safe to call from any thread.
     *
     * Duplicate prevention: {@code socket.off(event)} is called before each
     * {@code socket.on(event, listener)} so a listener is never registered
     * twice on the same socket instance.
     */
    private static synchronized void reattachPersistedListeners() {
        if (socket == null) {
            Log.w(TAG, "⚠️ reattachPersistedListeners: socket is null, skipping");
            return;
        }
        if (persistentListeners.isEmpty()) {
            Log.d(TAG, "ℹ️ reattachPersistedListeners: no persisted listeners to reattach");
            return;
        }
        Log.d(TAG, "============================================");
        Log.d(TAG, "🔁 REATTACHING PERSISTED LISTENERS");
        Log.d(TAG, "Total listeners to reattach: " + persistentListeners.size());
        for (java.util.Map.Entry<String, io.socket.emitter.Emitter.Listener> entry
                : persistentListeners.entrySet()) {
            String event    = entry.getKey();
            io.socket.emitter.Emitter.Listener listener = entry.getValue();
            // Remove first to guarantee no duplicates
            socket.off(event, listener);
            socket.on(event, listener);
            Log.d(TAG, "↩️ Reattached persisted listener for event: " + event);
        }
        Log.d(TAG, "✅ All persisted listeners reattached");
        Log.d(TAG, "============================================");
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
     * ✅ CORRECT FIX: Join lobby chat by recreating socket with fresh token.
     * Flow: 1) Refresh token  2) Disconnect old socket  3) Create new socket
     *       4) Connect         5) Emit subscribe:lobby-chat on connect
     * Chat listeners stored in fields are re-applied to the new socket automatically.
     * Wallet/tournament subscriptions are re-emitted via the EVENT_CONNECT listener
     * already registered in subscribe().
     * Matches backend: subscribe:lobby-chat
     *
     * @param tournamentId The tournament ID
     * @param userId       The user ID
     * @param username     The username
     */
    public void joinTournamentRoom(String tournamentId, String userId, String username) {
        Log.d(TAG, "============================================");
        Log.d(TAG, "🔵 JOINING LOBBY CHAT");
        Log.d(TAG, "Tournament ID: " + tournamentId);
        Log.d(TAG, "User ID:       " + userId);
        Log.d(TAG, "Username:      " + username);
        Log.d(TAG, "============================================");

        try {
            // ── STEP 1: Refresh token ──────────────────────────────────────
            String freshToken = getFreshToken();
            if (freshToken == null || freshToken.isEmpty()) {
                Log.e(TAG, "❌ Cannot join chat: no token available");
                return;
            }
            Log.d(TAG, "✅ Step 1 complete: token refreshed");

            // ── STEP 2: Disconnect old socket cleanly ──────────────────────
            if (socket != null) {
                socket.off(); // remove ALL listeners to prevent leaks
                socket.disconnect();
                socket = null;
                Log.d(TAG, "✅ Step 2 complete: old socket disconnected");
            }

            // ── STEP 3: Create new socket with fresh token ─────────────────
            IO.Options options = new IO.Options();
            options.auth = new java.util.HashMap<>();
            options.auth.put("token", freshToken);
            options.reconnection = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay = 1000;
            options.reconnectionDelayMax = 5000;
            options.timeout = 20000;

            socket = IO.socket(SOCKET_URL, options);
            Log.d(TAG, "✅ Step 3 complete: new socket created with fresh token");

            // Re-attach global listeners (connect/disconnect/wallet/tournament events)
            attachGlobalListeners();

            // Re-attach stored chat listeners so TournamentChatActivity keeps working
            if (chatMessageListener != null) {
                socket.on("lobby-chat:message", chatMessageListener);
                Log.d(TAG, "↩️ Re-attached: lobby-chat:message listener");
            }
            if (chatErrorListener != null) {
                socket.on("lobby-chat:error", chatErrorListener);
                Log.d(TAG, "↩️ Re-attached: lobby-chat:error listener");
            }
            if (chatClosedListener != null) {
                socket.on("lobby-chat:closed", chatClosedListener);
                Log.d(TAG, "↩️ Re-attached: lobby-chat:closed listener");
            }
            if (userJoinedListener != null) {
                socket.on("tournament:user-joined", userJoinedListener);
                Log.d(TAG, "↩️ Re-attached: tournament:user-joined listener");
            }
            if (userLeftListener != null) {
                socket.on("tournament:user-left", userLeftListener);
                Log.d(TAG, "↩️ Re-attached: tournament:user-left listener");
            }

            // ── PERSISTENT LISTENER MAP REATTACHMENT ──────────────────────
            // This catches every listener stored via persistentListeners, including
            // tournament:message-history (which has no dedicated static field) and
            // any listener registered while socket was null during this very
            // recreation cycle (the core race-condition fix).
            reattachPersistedListeners();

            // Re-register wallet/tournament subscriptions on connect
// 🔥 FORCE subscription to guarantee host works even if DashboardActivity
// did not call SocketManager.subscribe(userId)

            final String uid = userId;

// Set global tracking variables properly
            currentUserId = uid;
            isSubscribed = false;

            socket.once(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "🔄 Subscribing wallet/tournaments for user (forced): " + uid);

                socket.emit("subscribe:wallet", uid);
                socket.emit("subscribe:user-tournaments", uid);

                isSubscribed = true;

                Log.d(TAG, "✅ Forced subscription complete for user: " + uid);
            });
            // ── STEP 4: Connect ────────────────────────────────────────────
            // ── STEP 5: Emit subscribe:lobby-chat once connected ───────────
            // ✅ ROOT CAUSE FIX: Backend docs show  socket.emit('subscribe:lobby-chat', tournamentId)
            // — just the tournamentId STRING, NOT a JSON object {tournamentId, userId, username}.
            // We were sending the full object, which the server couldn't parse → "Failed to join
            // lobby chat" → host never in the room → host received zero participant messages.
            // Participants were unaffected because the server auto-delivers lobby-chat:message to
            // all registered participants via subscribe:user-tournaments. The host has no such
            // fallback channel and ONLY receives messages via the lobby-chat room subscription.
            socket.once(Socket.EVENT_CONNECT, args -> {
                socket.emit("subscribe:lobby-chat", tournamentId);
                Log.d(TAG, "✅ Step 5 complete: subscribe:lobby-chat emitted");
                Log.d(TAG, "   Payload (tournamentId): " + tournamentId);
                Log.d(TAG, "   Socket ID: " + socket.id());
                Log.d(TAG, "   Socket Connected: " + socket.connected());
                Log.d(TAG, "============================================");
            });

            socket.connect();
            Log.d(TAG, "✅ Step 4 complete: socket connecting...");

        } catch (Exception e) {
            Log.e(TAG, "============================================");
            Log.e(TAG, "❌ ERROR in joinTournamentRoom");
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
        // ── PERSIST FIRST (before any early-exit) ─────────────────────────
        // Storing the listener here ensures it survives the window where
        // socket is null during recreation and is picked up by
        // reattachPersistedListeners() when the new socket is ready.
        persistentListeners.put("lobby-chat:message", listener);

        if (socket == null) {
            Log.e(TAG, "Cannot listen for messages - socket is null");
            return;
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "🎧 SETTING UP MESSAGE LISTENER");
        Log.d(TAG, "Event: lobby-chat:message");
        Log.d(TAG, "============================================");

        // Store for re-registration after socket recreation
        chatMessageListener = listener;

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
        // ── PERSIST FIRST ──────────────────────────────────────────────────
        // onMessageHistory had NO dedicated static field; without this line
        // the history listener was permanently lost on every socket recreation.
        persistentListeners.put("tournament:message-history", listener);

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
        // ── PERSIST FIRST ──────────────────────────────────────────────────
        persistentListeners.put("tournament:user-joined", listener);

        if (socket == null) {
            Log.e(TAG, "Cannot listen for user joined - socket is null");
            return;
        }

        // Store for re-registration after socket recreation
        userJoinedListener = listener;

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
        // ── PERSIST FIRST ──────────────────────────────────────────────────
        persistentListeners.put("tournament:user-left", listener);

        if (socket == null) {
            Log.e(TAG, "Cannot listen for user left - socket is null");
            return;
        }

        // Store for re-registration after socket recreation
        userLeftListener = listener;

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
        // ── PERSIST FIRST ──────────────────────────────────────────────────
        persistentListeners.put("lobby-chat:error", listener);

        if (socket == null) {
            Log.e(TAG, "Cannot listen for chat errors - socket is null");
            return;
        }

        // Store for re-registration after socket recreation
        chatErrorListener = listener;

        socket.off("lobby-chat:error");
        socket.on("lobby-chat:error", listener);
        Log.d(TAG, "✅ Listening for: lobby-chat:error");
    }

    /**
     * 🔥 UPDATED: Listen for lobby chat closed event
     * @param listener The listener callback
     */
    public void onChatClosed(io.socket.emitter.Emitter.Listener listener) {
        // ── PERSIST FIRST ──────────────────────────────────────────────────
        persistentListeners.put("lobby-chat:closed", listener);

        if (socket == null) {
            Log.e(TAG, "Cannot listen for chat closed - socket is null");
            return;
        }

        // Store for re-registration after socket recreation
        chatClosedListener = listener;

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

        // Clear stored references
        chatMessageListener = null;
        chatErrorListener = null;
        chatClosedListener = null;
        userJoinedListener = null;
        userLeftListener = null;

        // ── CLEAR PERSISTENT MAP ───────────────────────────────────────────
        // Remove the exact keys this activity registered so they are not
        // spuriously re-attached if a different chat session opens later.
        // Using remove() per-key (not clear()) preserves any unrelated entries.
        persistentListeners.remove("lobby-chat:message");
        persistentListeners.remove("lobby-chat:error");
        persistentListeners.remove("lobby-chat:closed");
        persistentListeners.remove("tournament:message-history");
        persistentListeners.remove("tournament:user-joined");
        persistentListeners.remove("tournament:user-left");
        Log.d(TAG, "✅ Cleared persistent listener map entries for chat events");

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