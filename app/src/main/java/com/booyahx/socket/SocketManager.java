package com.booyahx.socket;

import android.util.Log;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {

    private static Socket socket;
    private static final String TAG = "SocketManager";

    public static Socket getSocket(String token) {

        if (socket != null) {
            return socket;
        }

        try {
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket", "polling"};
            options.reconnection = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay = 2000;

            options.extraHeaders = new HashMap<>();
            options.extraHeaders.put(
                    "Authorization",
                    Collections.singletonList("Bearer " + token)
            );

            Log.d(TAG, "Creating socket instance");
            socket = IO.socket("wss://api.gaminghuballday.buzz", options);

            socket.on(Socket.EVENT_CONNECT, args ->
                    Log.d(TAG, "✅ SOCKET CONNECTED")
            );

            socket.on(Socket.EVENT_DISCONNECT, args ->
                    Log.d(TAG, "⚠ SOCKET DISCONNECTED")
            );

            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    Log.e(TAG, "❌ SOCKET CONNECT ERROR: " + args[0])
            );

        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket URL error", e);
        }

        return socket;
    }

    public static void connect() {
        if (socket != null && !socket.connected()) {
            Log.d(TAG, "Connecting socket...");
            socket.connect();
        }
    }

    public static void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
    }
}