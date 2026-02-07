package com.booyahx.socket;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.booyahx.MyApp;

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

    private static Socket socket;
    private static String currentUserId = null;
    private static boolean isSubscribed = false;

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
            Log.d(TAG, "============================================");
        });
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

        Log.d(TAG, "✅ Global event listeners attached");
    }

    /**
     * Send local broadcast with optional data
     */
    private static void sendBroadcast(String action, Object... args) {
        Intent intent = new Intent(action);

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
}