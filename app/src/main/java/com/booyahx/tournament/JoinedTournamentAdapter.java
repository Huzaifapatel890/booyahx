package com.booyahx.tournament;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;
import com.booyahx.network.models.JoinedTournament;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JoinedTournamentAdapter
        extends RecyclerView.Adapter<JoinedTournamentAdapter.TournamentViewHolder> {

    private List<JoinedTournament> tournaments;

    public JoinedTournamentAdapter(List<JoinedTournament> tournaments) {
        this.tournaments = tournaments;
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
        holder.bind(tournaments.get(position));
    }

    @Override
    public int getItemCount() {
        return tournaments == null ? 0 : tournaments.size();
    }

    // REQUIRED FOR BACKEND UPDATE
    public void updateData(List<JoinedTournament> newTournaments) {
        this.tournaments = newTournaments;
        notifyDataSetChanged();
    }

    static class TournamentViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvSubtitle, tvEntry, tvPrize,
                tvPlayers, tvTime, tvSlot,
                tvRoomId, tvPassword;

        TextView btnRules;

        CountDownTimer timer;

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

            btnRules   = itemView.findViewById(R.id.btnRules);
        }

        public void bind(JoinedTournament t) {

            tvTitle.setText(t.getGame());
            tvSubtitle.setText(t.getMode() + " - " + t.getSubMode());

            tvEntry.setText(t.getEntryFee() + " GC");
            tvPrize.setText(t.getPrizePool() + " GC");

            int teams = calculateTeams(t);
            tvPlayers.setText(teams + "/" + calculateMaxTeams(t));
            tvSlot.setText("#" + t.getParticipantCount());

            handleCountdown(t);
            handleRoomAndPassword(t);

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
        }

        private void handleCountdown(JoinedTournament t) {

            // ================= STATUS OVERRIDE (ONLY THIS LOGIC ADDED) =================
            if (t.getStatus() != null) {
                String status = t.getStatus().toLowerCase(Locale.getDefault());

                if (status.contains("completed")) {
                    tvTime.setText("COMPLETED");
                    return;
                }

                if (status.contains("pending")) {
                    tvTime.setText("PENDING");
                    return;
                }

                if (status.contains("cancel")) {
                    tvTime.setText("CANCEL");
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
                    timer = new CountDownTimer(diff, 1000) {
                        @Override
                        public void onTick(long ms) {
                            long totalSeconds = ms / 1000;
                            long hours = totalSeconds / 3600;
                            long minutes = (totalSeconds % 3600) / 60;
                            long seconds = totalSeconds % 60;

                            if (hours > 0) {
                                tvTime.setText(
                                        String.format(
                                                Locale.getDefault(),
                                                "Starts in %dh %dm",
                                                hours, minutes
                                        )
                                );
                            } else {
                                tvTime.setText(
                                        String.format(
                                                Locale.getDefault(),
                                                "Starts in %02d:%02d",
                                                minutes, seconds
                                        )
                                );
                            }
                        }

                        @Override
                        public void onFinish() {
                            tvTime.setText("LIVE");
                        }
                    }.start();
                } else {
                    tvTime.setText("LIVE");
                }

            } catch (Exception e) {
                tvTime.setText("--");
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

            if (t.getRoom() != null && t.getRoom().getRoomId() != null) {
                tvRoomId.setText(t.getRoom().getRoomId());
                tvPassword.setText(t.getRoom().getPassword());

                enableCopy(tvRoomId, "Room ID copied");
                enableCopy(tvPassword, "Password copied");
                return;
            }

            Boolean started = isMatchStarted(t);

            if (!started) {
                tvRoomId.setText("U P C O");
                tvPassword.setText("M I N G");

                disableCopy(tvRoomId, "Room will be available at match time");
                disableCopy(tvPassword, "Room will be available at match time");
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