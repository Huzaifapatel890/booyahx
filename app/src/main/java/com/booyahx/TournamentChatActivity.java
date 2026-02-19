package com.booyahx;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.booyahx.adapters.ChatAdapter;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ChatMessage;
import com.booyahx.network.models.ChatHistoryResponse;
import com.booyahx.socket.SocketManager;
import io.socket.emitter.Emitter;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

/**
 * Tournament Chat Activity
 * Real-time chat using WebSocket for tournament participants
 * FIX LOG (current pass):
 *  - Wired new layout IDs: cvSend, layoutActiveBanner, viewLiveDot
 *  - Live dot pulse animation via ObjectAnimator (alpha 1.0 ↔ 0.3, 1.6s loop)
 *  - Banner visibility driven by tournament status ("live" | "running" → VISIBLE, else GONE)
 *  - Subtitle online count updated from onUserJoined / onUserLeft events
 *  - ChatHistoryResponse.MessageData field fix consumed here:
 *      getSenderNale string
 *  *  - handleNewMessage: reads "sendeme() replaces broken getUsername()
 *      getTimestamp() now parses createdAt ISO string
 *      isHost() now derived from rorName" field (API) with fallback to "username"
 *  - tournamentName shown in header, fallback to "Lobby"
 *  - Input disabled with correct hint when chat not live
 */
public class TournamentChatActivity extends AppCompatActivity {

    private static final String TAG = "TournamentChat";

    // ── UI Components ────────────────────────────────────────────
    private ImageView ivBack;
    private TextView tvTournamentName, tvSubtitle;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView ivSend;

    // FIX: New layout IDs from updated activity_lobby_chat.xml
    private CardView cvSend;
    private LinearLayout layoutActiveBanner;
    private View viewLiveDot;

    // ── Data ─────────────────────────────────────────────────────
    private ChatAdapter chatAdapter;
    private SocketManager socketManager;
    private String tournamentId;
    private String tournamentName;
    private String tournamentStatus;
    private String userId;
    private String username;
    private boolean isHost;

    // FIX: Online count tracking for subtitle
    private int onlineCount = 0;

    // FIX: Prevent duplicate sends
    private boolean isSending = false;
    private long lastSendTime = 0;
    private static final long SEND_COOLDOWN_MS = 300;

    // Live dot animator reference (so we can cancel in onDestroy)
    private ObjectAnimator liveDotAnimator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_chat);   // updated layout name

        // Get data from intent
        tournamentId     = getIntent().getStringExtra("tournament_id");
        tournamentName   = getIntent().getStringExtra("tournament_name");
        tournamentStatus = getIntent().getStringExtra("tournament_status");
        isHost           = getIntent().getBooleanExtra("is_host", false);

        // Get user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId   = prefs.getString("userId", "");
        username = prefs.getString("username", "");

        // FIX: If empty, try TokenManager
        if (TextUtils.isEmpty(userId)) {
            userId = TokenManager.getUserId(this);
            Log.w(TAG, "UserId was empty in SharedPreferences, got from TokenManager: " + userId);
        }

        // FIX: Username fallback chain
        if (TextUtils.isEmpty(username)) {
            username = prefs.getString("name", "");
        }
        if (TextUtils.isEmpty(username)) {
            SharedPreferences tokenPrefs = getSharedPreferences("booyahx_user", MODE_PRIVATE);
            username = tokenPrefs.getString("name", "");
        }
        if (TextUtils.isEmpty(username)) {
            SharedPreferences tokenPrefs = getSharedPreferences("booyahx_user", MODE_PRIVATE);
            username = tokenPrefs.getString("username", "");
        }
        if (TextUtils.isEmpty(username)) {
            username = "User";
            Log.w(TAG, "⚠️ All username lookups failed, defaulting to 'User'");
        }

        Log.d(TAG, "============================================");
        Log.d(TAG, "🔐 User Authentication Check");
        Log.d(TAG, "User ID: "   + (TextUtils.isEmpty(userId) ? "EMPTY!!!" : userId));
        Log.d(TAG, "Username: "  + username);
        Log.d(TAG, "Status: "    + tournamentStatus);
        Log.d(TAG, "============================================");

        if (TextUtils.isEmpty(tournamentId)) {
            Toast.makeText(this, "Invalid tournament", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "User not authenticated. Please login again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "❌ CRITICAL: userId is empty!");
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadChatHistoryFromAPI();
        setupWebSocket();
        setupClickListeners();
    }

    // ═══════════════════════════════════════════════════════════════
    //  INIT VIEWS
    // ═══════════════════════════════════════════════════════════════
    private void initViews() {
        ivBack            = findViewById(R.id.ivBack);
        tvTournamentName  = findViewById(R.id.tvTournamentName);
        tvSubtitle        = findViewById(R.id.tvSubtitle);
        rvMessages        = findViewById(R.id.rvMessages);
        etMessage         = findViewById(R.id.etMessage);
        ivSend            = findViewById(R.id.ivSend);

        // FIX: New IDs from updated layout
        cvSend            = findViewById(R.id.cvSend);
        layoutActiveBanner = findViewById(R.id.layoutActiveBanner);
        viewLiveDot       = findViewById(R.id.viewLiveDot);

        // ── Header ──
        // Show tournament name in header; fallback to "Lobby"
        if (!TextUtils.isEmpty(tournamentName)) {
            tvTournamentName.setText(tournamentName);
        } else {
            tvTournamentName.setText("Lobby");
        }
        updateSubtitleOnlineCount();

        // ── Active banner visibility ──
        // FIX: Banner only shown when match is live/running
        boolean isLive = "live".equalsIgnoreCase(tournamentStatus)
                || "running".equalsIgnoreCase(tournamentStatus);

        if (isLive) {
            layoutActiveBanner.setVisibility(View.VISIBLE);
            startLiveDotAnimation();
        } else {
            layoutActiveBanner.setVisibility(View.GONE);
        }

        // ── Input setup ──
        etMessage.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etMessage.setMaxLines(4);
        etMessage.setSingleLine(false);
        etMessage.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        // FIX: Disable input when not live
        if (!isLive) {
            etMessage.setEnabled(false);
            etMessage.setHint("Chat not available yet");
            if (cvSend != null) cvSend.setAlpha(0.3f);
            ivSend.setEnabled(false);
            ivSend.setAlpha(0.3f);
        } else {
            ivSend.setEnabled(false);   // enabled by TextWatcher once user types
            ivSend.setAlpha(0.5f);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  LIVE DOT PULSE ANIMATION
    //  Matches HTML: @keyframes livePulse — alpha 1.0 ↔ 0.3, 1.6s infinite
    // ═══════════════════════════════════════════════════════════════
    private void startLiveDotAnimation() {
        if (viewLiveDot == null) return;

        liveDotAnimator = ObjectAnimator.ofFloat(viewLiveDot, "alpha", 1.0f, 0.3f);
        liveDotAnimator.setDuration(800);                          // half of 1.6s cycle
        liveDotAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        liveDotAnimator.setRepeatMode(ObjectAnimator.REVERSE);     // ping-pong = full pulse
        liveDotAnimator.setInterpolator(new LinearInterpolator());
        liveDotAnimator.start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  SUBTITLE HELPER — "Lobby Chat · N online"
    // ═══════════════════════════════════════════════════════════════
    private void updateSubtitleOnlineCount() {
        if (tvSubtitle == null) return;
        if (onlineCount > 0) {
            tvSubtitle.setText("Lobby Chat · " + onlineCount + " online");
        } else {
            tvSubtitle.setText("Lobby Chat");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RECYCLER VIEW
    // ═══════════════════════════════════════════════════════════════
    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);
    }

    // ═══════════════════════════════════════════════════════════════
    //  API: LOAD CHAT HISTORY
    // ═══════════════════════════════════════════════════════════════
    private void loadChatHistoryFromAPI() {
        Log.d(TAG, "📡 Loading chat history — /tournament/" + tournamentId + "/chat");

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<ChatHistoryResponse> call = apiService.getChatHistory(tournamentId, 50, 0);

        call.enqueue(new Callback<ChatHistoryResponse>() {
            @Override
            public void onResponse(Call<ChatHistoryResponse> call,
                                   Response<ChatHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        ChatHistoryResponse chatResponse = response.body();
                        Log.d(TAG, "✅ API response success");

                        if (!chatResponse.isSuccess()) {
                            Log.w(TAG, "⚠️ API returned success=false");
                            return;
                        }

                        ChatHistoryResponse.ChatData data = chatResponse.getData();
                        if (data == null) {
                            Log.w(TAG, "⚠️ Data object is null");
                            return;
                        }

                        Log.d(TAG, "📦 Tournament: " + data.getTournamentId());

                        List<ChatHistoryResponse.MessageData> messages = data.getMessages();
                        if (messages != null && !messages.isEmpty()) {
                            Log.d(TAG, "📜 " + messages.size() + " messages from API");
                            handleMessageHistoryFromAPI(messages);
                        } else {
                            Log.d(TAG, "ℹ️ No chat history yet");
                            // ANIMATION: no history, mark boundary at 0 so all live messages animate
                            chatAdapter.markHistoryLoaded();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing API response", e);
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "❌ API failed — code: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFailure(Call<ChatHistoryResponse> call, Throwable t) {
                Log.e(TAG, "❌ API call FAILED: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  PROCESS HISTORY — FIX: uses getSenderName() + getTimestamp()
    //  which now correctly read senderName/createdAt from API
    // ═══════════════════════════════════════════════════════════════
    private void handleMessageHistoryFromAPI(List<ChatHistoryResponse.MessageData> messages) {
        try {
            Log.d(TAG, "📜 Processing " + messages.size() + " history messages");

            for (ChatHistoryResponse.MessageData msgData : messages) {

                int messageType;
                if (msgData.getUserId().equals(userId)) {
                    messageType = ChatMessage.TYPE_SENT;
                } else if (msgData.isHost()) {               // FIX: now reads role field
                    messageType = ChatMessage.TYPE_HOST;
                } else {
                    messageType = ChatMessage.TYPE_RECEIVED;
                }

                ChatMessage message = new ChatMessage(
                        msgData.getUserId(),
                        msgData.getSenderName(),             // FIX: was getUsername() — broken
                        msgData.getMessage(),
                        msgData.getTimestamp(),              // FIX: now parses createdAt ISO string
                        msgData.isHost(),
                        messageType
                );

                chatAdapter.addMessage(message);
            }

            if (chatAdapter.getItemCount() > 0) {
                rvMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
            }

            // ANIMATION: mark boundary so all subsequent live messages animate in
            chatAdapter.markHistoryLoaded();

            Log.d(TAG, "✅ History loaded: " + messages.size() + " messages");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing history messages", e);
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  WEBSOCKET SETUP
    // ═══════════════════════════════════════════════════════════════
    private void setupWebSocket() {
        Log.d(TAG, "============================================");
        Log.d(TAG, "Setting up WebSocket");
        Log.d(TAG, "Tournament: " + tournamentId + " | User: " + userId + " | Host: " + isHost);
        Log.d(TAG, "============================================");

        socketManager = SocketManager.getInstance();
        socketManager.connect();

        boolean isLive = "live".equalsIgnoreCase(tournamentStatus)
                || "running".equalsIgnoreCase(tournamentStatus);

        if (isLive) {
            Log.d(TAG, "✅ Tournament is live — joining chat room");
            new Thread(() -> {
                socketManager.joinTournamentRoom(tournamentId, userId, username);
                Log.d(TAG, "Join room request sent");
            }).start();
        } else {
            Log.w(TAG, "⚠️ Tournament not live — status: " + tournamentStatus);
            // Input is already disabled in initViews()
        }

        // ── lobby-chat:message ──
        // Payload: { _id, tournamentId, userId, senderName, role, message, createdAt }
        socketManager.onNewMessage(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        if (args == null || args.length == 0) return;
                        Object firstArg = args[0];
                        Log.d(TAG, "📨 lobby-chat:message — " + firstArg.toString());
                        if (firstArg instanceof JSONObject) {
                            handleNewMessage((JSONObject) firstArg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error handling new message", e);
                        e.printStackTrace();
                    }
                });
            }
        });

        // ── lobby-chat:error ──
        socketManager.onChatError(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        Log.e(TAG, "❌ lobby-chat:error — " + (args.length > 0 ? args[0] : "no data"));
                        if (args.length > 0) {
                            Toast.makeText(TournamentChatActivity.this,
                                    "Chat error: " + args[0], Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling chat error", e);
                    }
                });
            }
        });

        // ── lobby-chat:closed ──
        // Payload: { tournamentId, status }
        socketManager.onChatClosed(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        // Parse payload: { tournamentId, status }
                        String closedTournamentId = null;
                        String closedStatus = null;
                        if (args != null && args.length > 0 && args[0] instanceof JSONObject) {
                            JSONObject payload = (JSONObject) args[0];
                            closedTournamentId = payload.optString("tournamentId", null);
                            closedStatus       = payload.optString("status", null);
                        }

                        Log.d(TAG, "============================================");
                        Log.d(TAG, "📨 lobby-chat:closed received");
                        Log.d(TAG, "   tournamentId: " + closedTournamentId);
                        Log.d(TAG, "   status: "       + closedStatus);
                        Log.d(TAG, "============================================");

                        Toast.makeText(TournamentChatActivity.this,
                                "Tournament chat has been closed", Toast.LENGTH_LONG).show();

                        // Clear local chat data
                        chatAdapter = new ChatAdapter();
                        rvMessages.setAdapter(chatAdapter);
                        Log.d(TAG, "🗑️ Local chat data cleared");

                        // Disable input
                        etMessage.setEnabled(false);
                        etMessage.setHint("Chat closed");
                        ivSend.setEnabled(false);
                        if (cvSend != null) cvSend.setAlpha(0.3f);

                        // Hide live banner & stop animation
                        layoutActiveBanner.setVisibility(View.GONE);
                        if (liveDotAnimator != null) liveDotAnimator.cancel();

                    } catch (Exception e) {
                        Log.e(TAG, "Error handling chat closed", e);
                    }
                });
            }
        });

        // ── user joined — update online count ──
        socketManager.onUserJoined(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String joinedUsername = data.optString("username", "Someone");
                        Log.d(TAG, "👤 " + joinedUsername + " joined");
                        onlineCount++;
                        updateSubtitleOnlineCount();
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling user joined", e);
                    }
                });
            }
        });

        // ── user left — update online count ──
        socketManager.onUserLeft(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String leftUsername = data.optString("username", "Someone");
                        Log.d(TAG, "👋 " + leftUsername + " left");
                        if (onlineCount > 0) onlineCount--;
                        updateSubtitleOnlineCount();
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling user left", e);
                    }
                });
            }
        });

        Log.d(TAG, "✅ WebSocket listeners attached");
    }

    // ═══════════════════════════════════════════════════════════════
    //  CLICK LISTENERS
    // ═══════════════════════════════════════════════════════════════
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        // FIX: Click listener on cvSend (CardView container) — works even if tap misses ivSend
        View sendTarget = cvSend != null ? cvSend : ivSend;
        sendTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSendTime < SEND_COOLDOWN_MS) {
                    Log.d(TAG, "⚠️ Cooldown active, ignoring click");
                    return;
                }
                if (isSending) {
                    Log.d(TAG, "⚠️ Already sending, ignoring click");
                    return;
                }
                Log.d(TAG, "🖱️ Send clicked");
                sendMessage();
            }
        });

        // IME send key
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (!isSending) sendMessage();
                return true;
            }
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (!isSending) sendMessage();
                return true;
            }
            return false;
        });

        // Enable/disable send button based on text
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                boolean enabled = hasText && !isSending;
                ivSend.setEnabled(enabled);
                ivSend.setAlpha(enabled ? 1.0f : 0.5f);
                if (cvSend != null) cvSend.setAlpha(enabled ? 1.0f : 0.5f);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEND MESSAGE
    // ═══════════════════════════════════════════════════════════════
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) return;
        if (messageText.replaceAll("\\s+", "").isEmpty()) {
            etMessage.setText("");
            return;
        }

        isSending = true;
        lastSendTime = System.currentTimeMillis();

        // Disable send during send
        ivSend.setEnabled(false);
        ivSend.setAlpha(0.5f);
        if (cvSend != null) cvSend.setAlpha(0.5f);

        Log.d(TAG, "💬 Sending: '" + messageText + "' | user: " + username + " | host: " + isHost);

        try {
            socketManager.sendMessage(tournamentId, userId, username, messageText, isHost);
            Log.d(TAG, "✅ Message sent to socket manager");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending message", e);
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            isSending = false;
            updateSendButtonState();
            return;
        }

        // Clear input immediately
        etMessage.setText("");

        // Add to local UI immediately
        ChatMessage sentMessage = new ChatMessage(
                userId,
                username,
                messageText,
                System.currentTimeMillis(),
                isHost,
                ChatMessage.TYPE_SENT
        );
        chatAdapter.addMessage(sentMessage);

        if (chatAdapter.getItemCount() > 0) {
            rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }

        // Reset sending flag after cooldown
        rvMessages.postDelayed(() -> {
            isSending = false;
            updateSendButtonState();
            Log.d(TAG, "✅ Send cooldown complete");
        }, SEND_COOLDOWN_MS);
    }

    private void updateSendButtonState() {
        boolean hasText = etMessage.getText().toString().trim().length() > 0;
        boolean enabled = hasText && !isSending;
        ivSend.setEnabled(enabled);
        ivSend.setAlpha(enabled ? 1.0f : 0.5f);
        if (cvSend != null) cvSend.setAlpha(enabled ? 1.0f : 0.5f);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HANDLE INCOMING WEBSOCKET MESSAGE
    //  Payload: { _id, tournamentId, userId, senderName, role, message, createdAt }
    //  FIX: reads "senderName" (API field) with fallback to "username"
    //  FIX: reads "role" ("host"|"participant") instead of boolean isHost
    //  FIX: parses "createdAt" ISO string for timestamp
    // ═══════════════════════════════════════════════════════════════
    private void handleNewMessage(JSONObject data) {
        try {
            Log.d(TAG, "📝 Parsing incoming message...");

            // Payload field: userId
            String messageUserId = data.getString("userId");

            // Payload field: senderName (FIX: API sends "senderName" not "username")
            String messageUsername = data.optString("senderName",
                    data.optString("username", "Unknown"));

            // Payload field: message
            String messageText = data.getString("message");

            // Payload field: createdAt — parse ISO string; fallback to timestamp millis
            long timestamp;
            if (data.has("createdAt")) {
                try {
                    String createdAt = data.getString("createdAt");
                    java.text.SimpleDateFormat sdf =
                            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    java.util.Locale.US);
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date date = sdf.parse(createdAt);
                    timestamp = date != null ? date.getTime() : System.currentTimeMillis();
                } catch (Exception e) {
                    timestamp = System.currentTimeMillis();
                }
            } else if (data.has("timestamp")) {
                timestamp = data.getLong("timestamp");
            } else {
                timestamp = System.currentTimeMillis();
            }

            // Payload field: role ("host" | "participant") — FIX: was boolean isHost
            boolean messageIsHost = false;
            if (data.has("role")) {
                messageIsHost = "host".equalsIgnoreCase(data.getString("role"));
            } else if (data.has("isHost")) {
                messageIsHost = data.getBoolean("isHost");
            }

            Log.d(TAG, "  _id: "   + data.optString("_id", "n/a"));
            Log.d(TAG, "  from: "  + messageUsername + " | host: " + messageIsHost);
            Log.d(TAG, "  msg: "   + messageText);

            // Skip own message (already shown locally)
            if (messageUserId.equals(userId)) {
                Log.d(TAG, "Skipping own message (already displayed)");
                return;
            }

            int messageType = messageIsHost ? ChatMessage.TYPE_HOST : ChatMessage.TYPE_RECEIVED;

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
            Log.e(TAG, "❌ Error parsing message: " + data.toString(), e);
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════════
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "🔴 Activity destroyed — cleaning up");

        // Cancel live dot animation
        if (liveDotAnimator != null) {
            liveDotAnimator.cancel();
            liveDotAnimator = null;
        }

        // Leave room and remove listeners
        if (socketManager != null) {
            socketManager.leaveTournamentRoom(tournamentId, userId);
            socketManager.removeAllListeners();
        }
    }
}