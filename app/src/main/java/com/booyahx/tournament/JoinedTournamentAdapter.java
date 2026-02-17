package com.booyahx.tournament;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.Host.EnhancedFinalResultDialog;
import com.booyahx.Host.FinalRow;
import com.booyahx.R;
import com.booyahx.TokenManager;
import com.booyahx.network.models.JoinedTournament;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JoinedTournamentAdapter
        extends RecyclerView.Adapter<JoinedTournamentAdapter.TournamentViewHolder> {

    private static final String TAG = "AdapterDebug";
    private List<JoinedTournament> tournaments;

    public JoinedTournamentAdapter(List<JoinedTournament> tournaments) {
        this.tournaments = tournaments;
        Log.d(TAG, "Adapter created with " + (tournaments == null ? "null" : tournaments.size() + " tournaments"));
    }

    @NonNull
    @Override
    public TournamentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_joined_tournament_card, parent, false);
        return new TournamentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TournamentViewHolder holder,
            int position
    ) {
        Log.d(TAG, "onBindViewHolder called for position: " + position);
        holder.bind(tournaments.get(position));
    }

    @Override
    public int getItemCount() {
        return tournaments == null ? 0 : tournaments.size();
    }

    // REQUIRED FOR BACKEND UPDATE
    public void updateData(List<JoinedTournament> newTournaments) {
        this.tournaments = newTournaments;
        Log.d(TAG, "updateData called with " + (newTournaments == null ? "null" : newTournaments.size() + " tournaments"));
        notifyDataSetChanged();
    }

    static class TournamentViewHolder extends RecyclerView.ViewHolder {

        private static final String TAG = "ViewHolderDebug";

        TextView tvTitle, tvSubtitle, tvEntry, tvPrize,
                tvPlayers, tvTime, tvSlot,
                tvRoomId, tvPassword;

        TextView btnResults, btnRules, btnChat;

        CountDownTimer timer;

        // ✅ Store room details when received from API
        private String savedRoomId;
        private String savedPassword;
        private CountDownTimer revealTimer;

        public TournamentViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle    = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvEntry    = itemView.findViewById(R.id.tvEntry);
            tvPrize    = itemView.findViewById(R.id.tvPrize);
            tvPlayers  = itemView.findViewById(R.id.tvPlayers);
            tvTime     = itemView.findViewById(R.id.tvTime);
            tvSlot     = itemView.findViewById(R.id.tvSlot);
            tvRoomId   = itemView.findViewById(R.id.tvRoomId);
            tvPassword = itemView.findViewById(R.id.tvPassword);

            btnResults = itemView.findViewById(R.id.btnResults);
            btnRules   = itemView.findViewById(R.id.btnRules);
            btnChat    = itemView.findViewById(R.id.btnChat);

            Log.d(TAG, "ViewHolder created, tvSlot is " + (tvSlot == null ? "NULL" : "NOT NULL"));
        }

        public void bind(JoinedTournament t) {
            Log.d(TAG, "============ BIND START ============");
            Log.d(TAG, "Tournament: " + (t == null ? "NULL" : t.getLobbyName()));

            // ✅ CHANGED: Show lobby name without entry fee
            String lobbyName = t.getLobbyName();
            if (lobbyName != null) {
                // Remove entry fee number (e.g., "Lobby 1 75 3PM" -> "Lobby 1 3PM")
                // Split by spaces, filter out standalone numbers between lobby name and time
                String[] parts = lobbyName.split(" ");
                StringBuilder cleanName = new StringBuilder();

                for (int i = 0; i < parts.length; i++) {
                    // Skip if it's a pure number and not the first part after "Lobby"
                    if (i > 1 && parts[i].matches("\\d+") && i < parts.length - 1) {
                        continue; // Skip the entry fee number
                    }
                    if (cleanName.length() > 0) {
                        cleanName.append(" ");
                    }
                    cleanName.append(parts[i]);
                }
                lobbyName = cleanName.toString();
            } else {
                lobbyName = t.getGame();
            }
            tvTitle.setText(lobbyName);
            tvSubtitle.setText(t.getMode() + " - " + t.getSubMode());

            tvEntry.setText(t.getEntryFee() + " GC");
            tvPrize.setText(t.getPrizePool() + " GC");

            // ✅ FIXED: Use API fields directly for SLOTS display
            tvPlayers.setText(t.getJoinedTeams() + "/" + t.getMaxTeams());

            // ✅ NEW: Show user's slot number by matching UID WITH DEBUG
            Log.d(TAG, "========================================");
            Log.d(TAG, "🔍 SLOT NUMBER LOGIC START");
            Log.d(TAG, "========================================");

            Context context = itemView.getContext();
            Log.d(TAG, "Context: " + (context == null ? "NULL" : "OK"));

            String myUserId = TokenManager.getUserId(context);
            Log.d(TAG, "📌 My User ID from TokenManager: " + myUserId);

            if (myUserId == null || myUserId.isEmpty()) {
                Log.e(TAG, "❌❌❌ USER ID IS NULL OR EMPTY!");
                tvSlot.setText("#0");
            } else {
                Log.d(TAG, "✅ User ID is valid, calling getUserSlotNumberByUid()");

                int slotNumber = t.getUserSlotNumberByUid(myUserId);

                Log.d(TAG, "📊 Slot number returned: " + slotNumber);

                if (slotNumber > 0) {
                    Log.d(TAG, "✅✅✅ Setting slot text to: #" + slotNumber);
                    tvSlot.setText("#" + slotNumber);
                } else {
                    Log.e(TAG, "❌ Slot number is 0, setting to #0");
                    tvSlot.setText("#0");
                }
            }

            // Verify what's actually displayed
            Log.d(TAG, "📺 Slot TextView text after setting: " + tvSlot.getText().toString());
            Log.d(TAG, "========================================");
            Log.d(TAG, "🔍 SLOT NUMBER LOGIC END");
            Log.d(TAG, "========================================");

            handleCountdown(t);
            handleRoomAndPassword(t);

            // ========================================================================
            // ✅ UPDATED: RESULTS button with proper dialog integration
            // ========================================================================
            if (btnResults != null) {
                String status = t.getStatus();

                // Show button only for running/finished tournaments
                if (status != null &&
                        ("running".equalsIgnoreCase(status) || "finished".equalsIgnoreCase(status))) {

                    btnResults.setVisibility(View.VISIBLE);

                    btnResults.setOnClickListener(v -> {
                        Log.d(TAG, "🎯 btnResults clicked for tournament: " + t.getId());
                        Log.d(TAG, "   Lobby: " + t.getLobbyName());
                        Log.d(TAG, "   Status: " + status);

                        showResultsDialog(t);
                    });

                } else {
                    // Hide button for upcoming tournaments
                    btnResults.setVisibility(View.GONE);
                    Log.d(TAG, "btnResults hidden - status: " + status);
                }
            }
            // ========================================================================

            if (btnRules != null && t.getRules() != null) {
                btnRules.setVisibility(View.VISIBLE);
                btnRules.setOnClickListener(v -> {

                    if (!(v.getContext() instanceof androidx.fragment.app.FragmentActivity))
                        return;

                    JoinedRulesBottomSheet sheet =
                            JoinedRulesBottomSheet.newInstance(t);

                    sheet.show(
                            ((androidx.fragment.app.FragmentActivity) v.getContext())
                                    .getSupportFragmentManager(),
                            "JoinedRulesBottomSheet"
                    );
                });
            }

            // ========================================================================
            // ✅ CHAT button - Opens TournamentChatActivity
            // ========================================================================
            if (btnChat != null) {
                btnChat.setVisibility(View.VISIBLE);
                btnChat.setOnClickListener(v -> {
                    Log.d(TAG, "💬 btnChat clicked for tournament: " + t.getId());



                    // Get user info from SharedPreferences
                    android.content.SharedPreferences prefs =
                            context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                    String userId = prefs.getString("userId", "");

                    // Launch TournamentChatActivity
                    android.content.Intent intent =
                            new android.content.Intent(context, com.booyahx.TournamentChatActivity.class);
                    intent.putExtra("tournament_id", t.getId());
                    intent.putExtra("tournament_name", t.getLobbyName());
                    intent.putExtra("is_host", false); // User is not host
                    intent.putExtra("tournament_status", t.getStatus()); // ✅ FIX: Pass tournament status

                    context.startActivity(intent);
                });
            }
            // ========================================================================

            Log.d(TAG, "============ BIND END ============");
        }

        // ========================================================================
        // ✅ NEW METHOD: Show results dialog with proper tournament data
        // ========================================================================
        private void showResultsDialog(JoinedTournament tournament) {
            Context context = itemView.getContext();

            // Empty results list - dialog will fetch from API
            List<FinalRow> emptyResults = new ArrayList<>();

            // Get tournament data
            String tournamentId = tournament.getId();
            String tournamentStatus = tournament.getStatus();

            // Validate tournament ID
            if (tournamentId == null || tournamentId.isEmpty()) {
                Log.e(TAG, "❌ Cannot show results - tournament ID is null/empty");
                Toast.makeText(context, "Invalid tournament data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Default to "running" if status is null
            if (tournamentStatus == null || tournamentStatus.isEmpty()) {
                Log.w(TAG, "⚠️ Tournament status is null/empty, defaulting to 'running'");
                tournamentStatus = "running";
            }

            Log.d(TAG, "========================================");
            Log.d(TAG, "📊 CREATING RESULTS DIALOG");
            Log.d(TAG, "========================================");
            Log.d(TAG, "Tournament ID: " + tournamentId);
            Log.d(TAG, "Tournament Status: " + tournamentStatus);
            Log.d(TAG, "Lobby Name: " + tournament.getLobbyName());
            Log.d(TAG, "========================================");

            // Create and show dialog
            EnhancedFinalResultDialog dialog = new EnhancedFinalResultDialog(
                    context,
                    emptyResults,        // Empty - will be fetched from API
                    tournamentId,        // Tournament ID for API call
                    tournamentStatus     // "running" or "finished"
            );

            dialog.show();

            // Log expected behavior
            if ("running".equalsIgnoreCase(tournamentStatus)) {
                Log.d(TAG, "✅ Dialog will show: Live Results 🔥");
            } else if ("finished".equalsIgnoreCase(tournamentStatus)) {
                Log.d(TAG, "✅ Dialog will show: Overall Standings 🏆");
            }
        }
        // ========================================================================

        private void handleCountdown(JoinedTournament t) {

            // ================= STATUS OVERRIDE (ONLY THIS LOGIC ADDED) =================
            if (t.getStatus() != null) {
                String status = t.getStatus().toLowerCase(Locale.getDefault());

                if (status.contains("completed")) {
                    tvTime.setText("COMPLETED");
                    tvTime.setTextColor(Color.parseColor("#FFFFFF")); // White for status
                    return;
                }

                if (status.contains("pending")) {
                    tvTime.setText("PENDING");
                    tvTime.setTextColor(Color.parseColor("#FFFFFF")); // White for status
                    return;
                }

                if (status.contains("cancel")) {
                    tvTime.setText("CANCEL");
                    tvTime.setTextColor(Color.parseColor("#FFFFFF")); // White for status
                    return;
                }
            }
            // ===========================================================================

            try {
                String dateTime = t.getDate() + " " + t.getStartTime();
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

                Date startDate = sdf.parse(dateTime);
                long diff = startDate.getTime() - System.currentTimeMillis();

                if (timer != null) timer.cancel();

                if (diff > 0) {
                    // Set green color for countdown
                    tvTime.setTextColor(Color.parseColor("#00FF00"));

                    timer = new CountDownTimer(diff, 1000) {
                        @Override
                        public void onTick(long ms) {
                            long hours = ms / (1000 * 60 * 60);
                            long minutes = (ms % (1000 * 60 * 60)) / (1000 * 60);
                            long seconds = (ms % (1000 * 60)) / 1000;

                            // Format: HH:MM:SS (e.g., 25:35:05)
                            tvTime.setText(
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d:%02d",
                                            hours, minutes, seconds
                                    )
                            );
                        }

                        @Override
                        public void onFinish() {
                            tvTime.setText("LIVE");
                            tvTime.setTextColor(Color.parseColor("#FFFFFF")); // Reset to white
                        }
                    }.start();
                } else {
                    tvTime.setText("LIVE");
                    tvTime.setTextColor(Color.parseColor("#FFFFFF")); // Reset to white
                }

            } catch (Exception e) {
                tvTime.setText("--");
                tvTime.setTextColor(Color.parseColor("#FFFFFF")); // Reset to white
            }
        }

        private int calculateTeams(JoinedTournament t) {
            int players = t.getParticipantCount();
            String mode = t.getSubMode().toLowerCase(Locale.getDefault());

            if (mode.contains("squad") || mode.contains("4v4")) {
                return players / 4;
            } else if (mode.contains("duo")) {
                return players / 2;
            } else if (mode.contains("solo")) {
                return players;
            }
            return players;
        }

        private int calculateMaxTeams(JoinedTournament t) {
            String mode = t.getSubMode().toLowerCase(Locale.getDefault());

            if (mode.contains("squad") || mode.contains("4v4")) {
                return t.getMaxPlayers() / 4;
            } else if (mode.contains("duo")) {
                return t.getMaxPlayers() / 2;
            } else if (mode.contains("solo")) {
                return t.getMaxPlayers();
            }
            return t.getMaxPlayers();
        }

        /* ================= ROOM LOGIC (FIXED ORDER ONLY) ================= */

        private void handleRoomAndPassword(JoinedTournament t) {

            // ✅ FIX 1: Show VOIDED if match is cancelled
            if (t.getStatus() != null && t.getStatus().toLowerCase(Locale.getDefault()).contains("cancel")) {
                tvRoomId.setText("V O I D");
                tvPassword.setText("E D");

                disableCopy(tvRoomId, "Match has been cancelled");
                disableCopy(tvPassword, "Match has been cancelled");
                return;
            }

            // ✅ Save room details when received from API
            if (t.getRoom() != null && t.getRoom().getRoomId() != null) {
                savedRoomId = t.getRoom().getRoomId();
                savedPassword = t.getRoom().getPassword();

                // ✅ FIX 2: Check if within 10 minutes of match start
                if (isWithin10Minutes(t)) {
                    // Reveal immediately if within 10 minutes
                    tvRoomId.setText(savedRoomId);
                    tvPassword.setText(savedPassword);

                    enableCopy(tvRoomId, "Room ID copied");
                    enableCopy(tvPassword, "Password copied");
                } else {
                    // Hide but start timer to reveal at 10 minutes
                    tvRoomId.setText("U P C O");
                    tvPassword.setText("M I N G");

                    disableCopy(tvRoomId, "Room will be available 10 minutes before match time");
                    disableCopy(tvPassword, "Room will be available 10 minutes before match time");

                    // Start timer to auto-reveal at 10 minutes
                    startRevealTimer(t);
                }
                return;
            }

            Boolean started = isMatchStarted(t);

            if (!started) {
                tvRoomId.setText("U P C O");
                tvPassword.setText("M I N G");

                disableCopy(tvRoomId, "Room will be available 10 minutes before match time");
                disableCopy(tvPassword, "Room will be available 10 minutes before match time");
                return;
            }

            tvRoomId.setText(" U P D A");
            tvPassword.setText("T I N G");

            disableCopy(tvRoomId, "Host is updating room details");
            disableCopy(tvPassword, "Host is updating room details");
        }

        private Boolean isMatchStarted(JoinedTournament t) {
            try {
                String dateTime = t.getDate() + " " + t.getStartTime();
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                Date startDate = sdf.parse(dateTime);
                return System.currentTimeMillis() >= startDate.getTime();
            } catch (Exception e) {
                return false;
            }
        }

        // ✅ FIX 2: Helper method to check if within 10 minutes of match start
        private Boolean isWithin10Minutes(JoinedTournament t) {
            try {
                String dateTime = t.getDate() + " " + t.getStartTime();
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                Date startDate = sdf.parse(dateTime);
                long diff = startDate.getTime() - System.currentTimeMillis();

                // Return true if within 10 minutes (600000 ms) before or after match start
                return diff <= 600000;
            } catch (Exception e) {
                return false;
            }
        }

        // ✅ Timer to auto-reveal room details at 10 minutes before match
        private void startRevealTimer(JoinedTournament t) {
            try {
                String dateTime = t.getDate() + " " + t.getStartTime();
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                Date startDate = sdf.parse(dateTime);
                long diff = startDate.getTime() - System.currentTimeMillis();

                // Calculate time until 10 minutes before match (diff - 600000)
                long timeUntilReveal = diff - 600000; // 600000 ms = 10 minutes

                if (timeUntilReveal > 0 && savedRoomId != null && savedPassword != null) {
                    // Cancel existing reveal timer if any
                    if (revealTimer != null) {
                        revealTimer.cancel();
                    }

                    revealTimer = new CountDownTimer(timeUntilReveal, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            // Just waiting...
                        }

                        @Override
                        public void onFinish() {
                            // Reveal room details
                            if (savedRoomId != null && savedPassword != null) {
                                tvRoomId.setText(savedRoomId);
                                tvPassword.setText(savedPassword);

                                enableCopy(tvRoomId, "Room ID copied");
                                enableCopy(tvPassword, "Password copied");
                            }
                        }
                    }.start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting reveal timer: " + e.getMessage());
            }
        }

        private void enableCopy(TextView tv, String msg) {
            tv.setOnClickListener(v ->
                    copy(v.getContext(), tv.getText().toString(), msg)
            );
        }

        private void disableCopy(TextView tv, String msg) {
            tv.setOnClickListener(v ->
                    Toast.makeText(v.getContext(), msg, Toast.LENGTH_SHORT).show()
            );
        }

        private void copy(Context ctx, String text, String toast) {
            ClipboardManager cm =
                    (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("data", text));
            Toast.makeText(ctx, toast, Toast.LENGTH_SHORT).show();
        }
    }
}