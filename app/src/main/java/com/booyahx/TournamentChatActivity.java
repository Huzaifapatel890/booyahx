package com.booyahx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.booyahx.adapters.ChatAdapter;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ChatMessage;
import com.booyahx.network.models.ChatHistoryResponse;
import com.booyahx.socket.SocketManager;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

/**
 * Tournament Chat Activity
 * Real-time chat using WebSocket for tournament participants
 * FIXED: API chat history + single-click send working
 * UPDATED: Corrected WebSocket event names to match backend
 */
public class TournamentChatActivity extends AppCompatActivity {

    private static final String TAG = "TournamentChat";

    // UI Components
    private ImageView ivBack;
    private TextView tvTournamentName, tvSubtitle;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView ivSend;

    // Data
    private ChatAdapter chatAdapter;
    private SocketManager socketManager;
    private String tournamentId;
    private String tournamentName;
    private String tournamentStatus; // 🔥 NEW: Tournament status
    private String userId;
    private String username;
    private boolean isHost;

    // 🔥 FIX: Prevent duplicate sends
    private boolean isSending = false;
    private long lastSendTime = 0;
    private static final long SEND_COOLDOWN_MS = 300; // 300ms between sends

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_chat);

        // Get data from intent
        tournamentId = getIntent().getStringExtra("tournament_id");
        tournamentName = getIntent().getStringExtra("tournament_name");
        tournamentStatus = getIntent().getStringExtra("tournament_status"); // 🔥 NEW: Get tournament status
        isHost = getIntent().getBooleanExtra("is_host", false);

        // Get user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        username = prefs.getString("username", "");

        // ✅ FIX: If empty, try TokenManager
        if (TextUtils.isEmpty(userId)) {
            userId = TokenManager.getUserId(this);
            Log.w(TAG, "UserId was empty in SharedPreferences, got from TokenManager: " + userId);
        }

        // 🔥 FIX: Improved username fallback chain
        // 1. Try "username" key in UserSession prefs (already done above)
        // 2. Try "name" key in UserSession prefs
        // 3. Try TokenManager's own "booyahx_user" prefs (stores "name" key from login)
        // 4. Fall back to "User"
        if (TextUtils.isEmpty(username)) {
            username = prefs.getString("name", "");
            Log.d(TAG, "Tried 'name' key in UserSession: '" + username + "'");
        }
        if (TextUtils.isEmpty(username)) {
            // TokenManager stores user prefs under "booyahx_user" SharedPreferences
            SharedPreferences tokenPrefs = getSharedPreferences("booyahx_user", MODE_PRIVATE);
            username = tokenPrefs.getString("name", "");
            Log.d(TAG, "Tried 'name' in booyahx_user prefs: '" + username + "'");
        }
        if (TextUtils.isEmpty(username)) {
            SharedPreferences tokenPrefs = getSharedPreferences("booyahx_user", MODE_PRIVATE);
            username = tokenPrefs.getString("username", "");
            Log.d(TAG, "Tried 'username' in booyahx_user prefs: '" + username + "'");
        }
        if (TextUtils.isEmpty(username)) {
            username = "User";
            Log.w(TAG, "⚠️ All username lookups failed, defaulting to 'User'");
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "🔐 User Authentication Check");
        Log.d(TAG, "User ID: " + (TextUtils.isEmpty(userId) ? "EMPTY!!!" : userId));
        Log.d(TAG, "Username: " + username);
        Log.d(TAG, "============================================");

        if (TextUtils.isEmpty(tournamentId)) {
            Toast.makeText(this, "Invalid tournament", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "User not authenticated. Please login again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "❌ CRITICAL: userId is empty! Cannot join chat.");
            finish();
            return;
        }

        initViews();
        setupRecyclerView();

        // 🔥 NEW: Load chat history from API FIRST
        loadChatHistoryFromAPI();

        setupWebSocket();
        setupClickListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTournamentName = findViewById(R.id.tvTournamentName);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        ivSend = findViewById(R.id.ivSend);

        // Set tournament name
        if (!TextUtils.isEmpty(tournamentName)) {
            tvTournamentName.setText(tournamentName);
        }
        tvSubtitle.setText("Lobby Chat");

        // ✅ FIX: Simpler input type that works better
        etMessage.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etMessage.setMaxLines(4);
        etMessage.setSingleLine(false);

        // ✅ FIX: Disable IME options that cause issues
        etMessage.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        // 🔥 FIX: Start with send button disabled and semi-transparent
        ivSend.setEnabled(false);
        ivSend.setAlpha(0.5f);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);
    }

    // 🔥 UPDATED METHOD: Load chat history using ChatHistoryResponse model
    private void loadChatHistoryFromAPI() {
        Log.d(TAG, "============================================");
        Log.d(TAG, "📡 CALLING API: Loading chat history from REST API");
        Log.d(TAG, "Endpoint: /tournament/" + tournamentId + "/chat");
        Log.d(TAG, "Tournament ID: " + tournamentId);
        Log.d(TAG, "============================================");

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<ChatHistoryResponse> call = apiService.getChatHistory(tournamentId, 50, 0);

        call.enqueue(new Callback<ChatHistoryResponse>() {
            @Override
            public void onResponse(Call<ChatHistoryResponse> call, Response<ChatHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        ChatHistoryResponse chatResponse = response.body();
                        Log.d(TAG, "✅ API RESPONSE SUCCESS!");

                        // Check if response is successful
                        if (!chatResponse.isSuccess()) {
                            Log.w(TAG, "⚠️ API returned success=false");
                            return;
                        }

                        // Get data object
                        ChatHistoryResponse.ChatData data = chatResponse.getData();
                        if (data == null) {
                            Log.w(TAG, "⚠️ Data object is null");
                            return;
                        }

                        Log.d(TAG, "📦 Got data object for tournament: " + data.getTournamentId());

                        // Get messages list
                        List<ChatHistoryResponse.MessageData> messages = data.getMessages();

                        if (messages != null) {
                            int messageCount = messages.size();
                            Log.d(TAG, "📜 API returned " + messageCount + " messages");

                            if (messageCount > 0) {
                                Log.d(TAG, "Processing " + messageCount + " messages from API");
                                handleMessageHistoryFromAPI(messages);
                            } else {
                                Log.d(TAG, "ℹ️ Messages list is empty - no chat history available yet");
                            }
                        } else {
                            Log.w(TAG, "⚠️ Messages list is null");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing API response", e);
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "❌ API call failed");
                    Log.e(TAG, "Response code: " + response.code());
                    Log.e(TAG, "Response message: " + response.message());
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ChatHistoryResponse> call, Throwable t) {
                Log.e(TAG, "❌ API call FAILED completely");
                Log.e(TAG, "Error message: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    // 🔥 NEW METHOD: Handle message history from API using ChatHistoryResponse model
    private void handleMessageHistoryFromAPI(List<ChatHistoryResponse.MessageData> messages) {
        try {
            Log.d(TAG, "📜 Processing " + messages.size() + " messages from API");

            for (ChatHistoryResponse.MessageData msgData : messages) {
                // Determine message type
                int messageType;
                if (msgData.getUserId().equals(userId)) {
                    messageType = ChatMessage.TYPE_SENT;
                } else if (msgData.isHost()) {
                    messageType = ChatMessage.TYPE_HOST;
                } else {
                    messageType = ChatMessage.TYPE_RECEIVED;
                }

                ChatMessage message = new ChatMessage(
                        msgData.getUserId(),
                        msgData.getUsername(),
                        msgData.getMessage(),
                        msgData.getTimestamp(),
                        msgData.isHost(),
                        messageType
                );

                chatAdapter.addMessage(message);
            }

            // Scroll to bottom after loading history
            if (chatAdapter.getItemCount() > 0) {
                rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
            }

            Log.d(TAG, "✅ Message history loaded successfully: " + messages.size() + " messages");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing API messages", e);
            e.printStackTrace();
        }
    }

    private void setupWebSocket() {
        Log.d(TAG, "============================================");
        Log.d(TAG, "Setting up WebSocket for tournament chat");
        Log.d(TAG, "Tournament ID: " + tournamentId);
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Username: " + username);
        Log.d(TAG, "Is Host: " + isHost);
        Log.d(TAG, "============================================");

        socketManager = SocketManager.getInstance();
        socketManager.connect();

        // 🔥 UPDATED: Check tournament status before joining
        // Backend only allows joining when status is 'running' or 'live'
        if (tournamentStatus != null && (tournamentStatus.equals("live") || tournamentStatus.equals("running"))) {
            Log.d(TAG, "✅ Tournament status is '" + tournamentStatus + "' - joining chat room");
            // 🔥 FIX: Run on background thread — joinTournamentRoom does a synchronous
            // token refresh network call which must NOT happen on the main thread
            new Thread(() -> {
                socketManager.joinTournamentRoom(tournamentId, userId, username);
                Log.d(TAG, "Join room request sent");
            }).start();
        } else {
            Log.w(TAG, "⚠️ Cannot join chat - tournament status is: " + tournamentStatus);
            Log.w(TAG, "Chat will be available when tournament goes live");
            runOnUiThread(() -> {
                Toast.makeText(this, "Chat will be available when tournament goes live", Toast.LENGTH_LONG).show();
                // Disable message input
                etMessage.setEnabled(false);
                ivSend.setEnabled(false);
                etMessage.setHint("Chat not available yet");
            });
        }

        // 🔥 UPDATED: Listen for new messages (lobby-chat:message)
        socketManager.onNewMessage(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "============================================");
                        Log.d(TAG, "📨 lobby-chat:message CALLBACK TRIGGERED");

                        if (args == null || args.length == 0) {
                            Log.w(TAG, "⚠️ No data received in callback");
                            Log.d(TAG, "============================================");
                            return;
                        }

                        Object firstArg = args[0];
                        Log.d(TAG, "Data type: " + firstArg.getClass().getName());
                        Log.d(TAG, "Data: " + firstArg.toString());

                        if (firstArg instanceof JSONObject) {
                            JSONObject data = (JSONObject) firstArg;
                            handleNewMessage(data);
                        } else {
                            Log.e(TAG, "❌ Expected JSONObject but got: " + firstArg.getClass().getName());
                        }

                        Log.d(TAG, "============================================");
                    } catch (Exception e) {
                        Log.e(TAG, "============================================");
                        Log.e(TAG, "❌ Error handling new message");
                        Log.e(TAG, "Exception: " + e.getMessage());
                        e.printStackTrace();
                        Log.e(TAG, "============================================");
                    }
                });
            }
        });

        // 🔥 NEW: Listen for chat errors
        socketManager.onChatError(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        Log.e(TAG, "============================================");
                        Log.e(TAG, "❌ lobby-chat:error RECEIVED");
                        if (args.length > 0) {
                            Log.e(TAG, "Error data: " + args[0].toString());
                            Toast.makeText(TournamentChatActivity.this,
                                    "Chat error: " + args[0].toString(),
                                    Toast.LENGTH_SHORT).show();
                        }
                        Log.e(TAG, "============================================");
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling chat error", e);
                    }
                });
            }
        });

        // 🔥 NEW: Listen for chat closed
        socketManager.onChatClosed(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "============================================");
                        Log.d(TAG, "📨 lobby-chat:closed RECEIVED");
                        Log.d(TAG, "Tournament chat has been closed");
                        Log.d(TAG, "============================================");

                        Toast.makeText(TournamentChatActivity.this,
                                "Tournament chat has been closed",
                                Toast.LENGTH_LONG).show();

                        // Optionally finish activity or disable chat input
                        etMessage.setEnabled(false);
                        ivSend.setEnabled(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling chat closed", e);
                    }
                });
            }
        });

        // Keep old listeners for backward compatibility
        socketManager.onUserJoined(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String joinedUsername = data.getString("username");
                        Log.d(TAG, "👤 " + joinedUsername + " joined the chat");
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling user joined", e);
                    }
                });
            }
        });

        socketManager.onUserLeft(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String leftUsername = data.getString("username");
                        Log.d(TAG, "👋 " + leftUsername + " left the chat");
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling user left", e);
                    }
                });
            }
        });

        Log.d(TAG, "✅ WebSocket listeners attached");
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        // 🔥 FIX: Prevent rapid multiple clicks with debouncing
        ivSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis();

                // Check if enough time has passed since last send
                if (currentTime - lastSendTime < SEND_COOLDOWN_MS) {
                    Log.d(TAG, "⚠️ Ignoring click - too soon after last send (" +
                            (currentTime - lastSendTime) + "ms)");
                    return;
                }

                // Check if we're already in the process of sending
                if (isSending) {
                    Log.d(TAG, "⚠️ Ignoring click - already sending message");
                    return;
                }

                Log.d(TAG, "🖱️ Send button clicked - proceeding to send");
                sendMessage();
            }
        });

        // ✅ FIX: Better IME action handling
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (!isSending) {
                    sendMessage();
                }
                return true;
            }
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (!isSending) {
                    sendMessage();
                }
                return true;
            }
            return false;
        });

        // ✅ FIX: Visual feedback - enable/disable send button
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                // Only enable if has text AND not currently sending
                ivSend.setEnabled(hasText && !isSending);
                ivSend.setAlpha((hasText && !isSending) ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        // ✅ FIX: Better validation
        if (TextUtils.isEmpty(messageText)) {
            Log.d(TAG, "Message is empty, not sending");
            return;
        }

        // ✅ FIX: Check if message contains only whitespace
        if (messageText.replaceAll("\\s+", "").isEmpty()) {
            Log.d(TAG, "Message contains only whitespace, not sending");
            etMessage.setText("");
            return;
        }

        // 🔥 FIX: Set sending flag IMMEDIATELY to prevent duplicate sends
        isSending = true;
        lastSendTime = System.currentTimeMillis();

        // Disable send button during sending
        ivSend.setEnabled(false);
        ivSend.setAlpha(0.5f);

        Log.d(TAG, "💬 Sending message: '" + messageText + "'");
        Log.d(TAG, "   Tournament: " + tournamentId);
        Log.d(TAG, "   User: " + username + " (" + userId + ")");
        Log.d(TAG, "   Is Host: " + isHost);

        // Send via WebSocket
        try {
            socketManager.sendMessage(tournamentId, userId, username, messageText, isHost);
            Log.d(TAG, "✅ Message sent to socket manager");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending message", e);
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();

            // 🔥 FIX: Reset flags on error
            isSending = false;
            updateSendButtonState();
            return;
        }

        // Clear input immediately
        etMessage.setText("");

        // Add to local UI immediately (will be confirmed by server)
        ChatMessage sentMessage = new ChatMessage(
                userId,
                username,
                messageText,
                System.currentTimeMillis(),
                isHost,
                ChatMessage.TYPE_SENT
        );
        chatAdapter.addMessage(sentMessage);

        // Scroll to bottom
        if (chatAdapter.getItemCount() > 0) {
            rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }

        // 🔥 FIX: Reset sending flag after a short delay
        rvMessages.postDelayed(() -> {
            isSending = false;
            updateSendButtonState();
            Log.d(TAG, "✅ Send cooldown complete, button re-enabled");
        }, SEND_COOLDOWN_MS);
    }

    // 🔥 NEW METHOD: Update send button state based on text and sending status
    private void updateSendButtonState() {
        boolean hasText = etMessage.getText().toString().trim().length() > 0;
        ivSend.setEnabled(hasText && !isSending);
        ivSend.setAlpha((hasText && !isSending) ? 1.0f : 0.5f);
    }

    private void handleNewMessage(JSONObject data) {
        try {
            Log.d(TAG, "📝 Parsing message data...");

            String messageUserId = data.getString("userId");

            // 🔥 FIX: Server sends "senderName" not "username"
            String messageUsername = data.optString("senderName",
                    data.optString("username", "Unknown"));

            String messageText = data.getString("message");

            // 🔥 FIX: Server sends "createdAt" timestamp, parse it or use current time
            long timestamp;
            if (data.has("createdAt")) {
                try {
                    // Try to parse ISO timestamp if provided
                    String createdAt = data.getString("createdAt");
                    timestamp = System.currentTimeMillis(); // Use current time for now
                } catch (Exception e) {
                    timestamp = System.currentTimeMillis();
                }
            } else if (data.has("timestamp")) {
                timestamp = data.getLong("timestamp");
            } else {
                timestamp = System.currentTimeMillis();
            }

            // 🔥 FIX: Server sends "role": "host" or "participant", not "isHost"
            boolean messageIsHost = false;
            if (data.has("role")) {
                String role = data.getString("role");
                messageIsHost = "host".equalsIgnoreCase(role);
            } else if (data.has("isHost")) {
                messageIsHost = data.getBoolean("isHost");
            }

            Log.d(TAG, "Handling new message from: " + messageUsername);
            Log.d(TAG, "   User ID: " + messageUserId);
            Log.d(TAG, "   Is Host: " + messageIsHost);
            Log.d(TAG, "   Message: " + messageText);

            // Don't add if it's our own message (already added)
            if (messageUserId.equals(userId)) {
                Log.d(TAG, "Skipping own message (already displayed locally)");
                return;
            }

            // Determine message type
            int messageType;
            if (messageIsHost) {
                messageType = ChatMessage.TYPE_HOST;
            } else {
                messageType = ChatMessage.TYPE_RECEIVED;
            }

            ChatMessage message = new ChatMessage(
                    messageUserId,
                    messageUsername,
                    messageText,
                    timestamp,
                    messageIsHost,
                    messageType
            );

            chatAdapter.addMessage(message);
            rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            Log.d(TAG, "✅ Message added to UI");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing message", e);
            Log.e(TAG, "Message data was: " + data.toString());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "============================================");
        Log.d(TAG, "🔴 Activity being destroyed - cleaning up");
        Log.d(TAG, "============================================");

        // Leave tournament room and cleanup
        if (socketManager != null) {
            socketManager.leaveTournamentRoom(tournamentId, userId);
            socketManager.removeAllListeners();
        }
    }
}