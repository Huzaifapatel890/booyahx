package com.booyahx.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * ChatHistoryResponse model
 *
 * FIX LOG — matched to actual API schema from Swagger doc:
 *   - MessageData.senderName  (was: username)       → @SerializedName("senderName")
 *   - MessageData.role        (was: isHost boolean)  → @SerializedName("role") String
 *   - MessageData.createdAt   (was: timestamp long)  → @SerializedName("createdAt") String ISO-8601
 *   - isHost() now derived from role.equals("host") instead of broken boolean field
 *   - getTimestamp() parses ISO createdAt string → millis, falls back to System.currentTimeMillis()
 *   - getSenderName() added (kept getUsername() as alias for backwards compat)
 */
public class ChatHistoryResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ChatData data;

    // ─────────────────────────────────────────────────
    public static class ChatData {

        @SerializedName("tournamentId")
        private String tournamentId;

        @SerializedName("messages")
        private List<MessageData> messages;

        @SerializedName("limit")
        private int limit;

        @SerializedName("skip")
        private int skip;

        public List<MessageData> getMessages()   { return messages; }
        public String getTournamentId()           { return tournamentId; }
        public int getLimit()                     { return limit; }
        public int getSkip()                      { return skip; }
    }

    // ─────────────────────────────────────────────────
    public static class MessageData {

        @SerializedName("_id")
        private String id;

        @SerializedName("userId")
        private String userId;

        /**
         * FIX: API sends "senderName", not "username"
         */
        @SerializedName("senderName")
        private String senderName;

        /**
         * FIX: API sends "role" ("host" | "participant"), not a boolean "isHost"
         */
        @SerializedName("role")
        private String role;

        @SerializedName("message")
        private String message;

        /**
         * FIX: API sends "createdAt" as ISO-8601 string, not a long "timestamp"
         * e.g. "2026-02-17T19:32:24.346Z"
         */
        @SerializedName("createdAt")
        private String createdAt;

        // ── Getters ──────────────────────────────────

        public String getId()         { return id; }
        public String getUserId()     { return userId; }
        public String getMessage()    { return message; }
        public String getRole()       { return role; }
        public String getCreatedAt()  { return createdAt; }

        /**
         * Returns sender display name.
         * FIX: was getUsername() → now reads senderName field.
         */
        public String getSenderName() { return senderName != null ? senderName : "Unknown"; }

        /**
         * Backwards-compat alias — code that called getUsername() still works.
         */
        public String getUsername()   { return getSenderName(); }

        /**
         * FIX: was a broken boolean field. Now derived from role string.
         * Returns true when role == "host" (case-insensitive).
         */
        public boolean isHost() {
            return "host".equalsIgnoreCase(role);
        }

        /**
         * FIX: Parses ISO-8601 "createdAt" string into epoch millis.
         * Falls back to System.currentTimeMillis() if parsing fails.
         */
        public long getTimestamp() {
            if (createdAt == null || createdAt.isEmpty()) {
                return System.currentTimeMillis();
            }
            try {
                // android.icu or java.time not always available on older APIs;
                // use SimpleDateFormat which is safe on all API levels.
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = sdf.parse(createdAt);
                return date != null ? date.getTime() : System.currentTimeMillis();
            } catch (Exception e) {
                // Try without milliseconds
                try {
                    java.text.SimpleDateFormat sdf2 =
                            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                                    java.util.Locale.US);
                    sdf2.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date date = sdf2.parse(createdAt);
                    return date != null ? date.getTime() : System.currentTimeMillis();
                } catch (Exception e2) {
                    return System.currentTimeMillis();
                }
            }
        }
    }

    // ─────────────────────────────────────────────────
    public boolean isSuccess()  { return success; }
    public int getStatus()      { return status; }
    public String getMessage()  { return message; }
    public ChatData getData()   { return data; }
}