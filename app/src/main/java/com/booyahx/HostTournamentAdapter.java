package com.booyahx;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.network.models.HostTournament;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HostTournamentAdapter
        extends RecyclerView.Adapter<HostTournamentAdapter.ViewHolder> {

    private static final String TAG = "HostAdapter";

    private Context context;
    private List<HostTournament> list;
    private OnItemClickListener listener;
    private Map<Integer, CountDownTimer> activeTimers = new HashMap<>();

    public interface OnItemClickListener {
        void onEditRoom(HostTournament t);
        void onSubmitResult(HostTournament t);
        void onViewResult(HostTournament t);
        void onEndTournament(HostTournament t);
        void onViewRules(HostTournament t);
    }

    public HostTournamentAdapter(Context c,
                                 List<HostTournament> l,
                                 OnItemClickListener li) {
        context = c;
        list = l;
        listener = li;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_host_tournament_card, p, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        if (list == null || list.isEmpty()) {
            return;
        }

        if (pos >= list.size()) {
            return;
        }

        HostTournament t = list.get(pos);

        if (t == null) {
            return;
        }

        // ✅ Show ALL buttons for ALL categories
        h.edit.setVisibility(View.VISIBLE);
        h.submit.setVisibility(View.VISIBLE);
        h.result.setVisibility(View.VISIBLE);
        h.end.setVisibility(View.VISIBLE);
        h.rules.setVisibility(View.VISIBLE);

        // Set text values
        h.title.setText(t.getHeaderTitle());
        h.mode.setText(t.getModeDisplay());
        h.slots.setText(t.getSlotsDisplay());
        h.roomId.setText(t.getRoomIdDisplay());
        h.password.setText(t.getPasswordDisplay());

        // ✅ FIXED: Add entry fee and prize pool display
        h.entryFee.setText(t.getEntryFeeDisplay());
        h.prizePool.setText(t.getPrizePoolDisplay());

        // Cancel existing timer for this position
        if (activeTimers.containsKey(pos)) {
            activeTimers.get(pos).cancel();
            activeTimers.remove(pos);
        }

        // Handle countdown logic
        handleCountdown(t, h, pos);

        // Set click listeners
        h.edit.setOnClickListener(v -> listener.onEditRoom(t));
        h.submit.setOnClickListener(v -> listener.onSubmitResult(t));
        h.result.setOnClickListener(v -> listener.onViewResult(t));
        h.rules.setOnClickListener(v -> listener.onViewRules(t));
        h.end.setOnClickListener(v -> listener.onEndTournament(t));
    }

    private void handleCountdown(HostTournament t, ViewHolder h, int pos) {
        // Check status first
        if (t.getStatus() != null) {
            String status = t.getStatus().toLowerCase(Locale.getDefault());

            if (status.contains("completed")) {
                h.time.setText("COMPLETED");
                return;
            }

            if (status.contains("pending") || status.contains("resultpending")) {
                h.time.setText("PENDING");
                return;
            }

            if (status.contains("cancel")) {
                h.time.setText("CANCELLED");
                return;
            }

            if (status.contains("live")) {
                h.time.setText("LIVE");
                return;
            }
        }

        // If status is "upcoming", calculate countdown
        try {
            String dateStr = t.getDate();
            String timeStr = t.getStartTime();

            if (dateStr == null || timeStr == null) {
                h.time.setText("--");
                return;
            }

            if (dateStr.contains("T")) {
                dateStr = dateStr.substring(0, dateStr.indexOf("T"));
            }

            String dateTime = dateStr + " " + timeStr;
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

            Date startDate = sdf.parse(dateTime);
            if (startDate == null) {
                h.time.setText("--");
                return;
            }

            long diff = startDate.getTime() - System.currentTimeMillis();

            if (diff > 0) {
                CountDownTimer timer = new CountDownTimer(diff, 1000) {
                    @Override
                    public void onTick(long ms) {
                        long totalSeconds = ms / 1000;
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;

                        if (hours > 0) {
                            h.time.setText(
                                    String.format(
                                            Locale.getDefault(),
                                            "%d:%d:%d",
                                            hours, minutes, seconds
                                    )
                            );

                        }
                    }

                    @Override
                    public void onFinish() {
                        h.time.setText("LIVE");
                    }
                };
                timer.start();
                activeTimers.put(pos, timer);
            } else {
                h.time.setText("LIVE");
            }

        } catch (Exception e) {
            h.time.setText("--");
            Log.e(TAG, "Error parsing date/time: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        int position = holder.getAdapterPosition();

        if (position != RecyclerView.NO_POSITION && activeTimers.containsKey(position)) {
            activeTimers.get(position).cancel();
            activeTimers.remove(position);
        }
    }

    public void cancelAllTimers() {
        for (CountDownTimer timer : activeTimers.values()) {
            timer.cancel();
        }
        activeTimers.clear();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, mode, slots, time, roomId, password;
        TextView entryFee, prizePool;  // ✅ ADDED: Entry fee and prize pool TextViews
        TextView edit, submit, result, rules, end;

        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.tournamentTitle);
            mode = v.findViewById(R.id.tournamentMode);
            slots = v.findViewById(R.id.slots);
            time = v.findViewById(R.id.timeStatus);
            roomId = v.findViewById(R.id.roomId);
            password = v.findViewById(R.id.password);

            // ✅ ADDED: Initialize entry fee and prize pool TextViews
            entryFee = v.findViewById(R.id.entryFee);
            prizePool = v.findViewById(R.id.prizePool);

            edit = v.findViewById(R.id.editIcon);
            submit = v.findViewById(R.id.submitResultBtn);
            result = v.findViewById(R.id.resultButton);
            rules = v.findViewById(R.id.rulesButton);
            end = v.findViewById(R.id.endBtn);
        }
    }
}