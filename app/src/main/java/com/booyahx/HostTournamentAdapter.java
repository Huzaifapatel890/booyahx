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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HostTournamentAdapter
        extends RecyclerView.Adapter<HostTournamentAdapter.ViewHolder> {

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
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_host_tournament_card, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        HostTournament t = list.get(pos);

        h.title.setText(t.getHeaderTitle());
        h.mode.setText(t.getModeDisplay());
        h.slots.setText(t.getSlotsDisplay());
        h.roomId.setText(t.getRoomIdDisplay());
        h.password.setText(t.getPasswordDisplay());

        if (activeTimers.containsKey(pos)) {
            activeTimers.get(pos).cancel();
            activeTimers.remove(pos);
        }

        String status = t.getStatus();
        Log.d("HostAdapter", "Tournament: " + t.getHeaderTitle() + ", Status: " + status);

        if ("upcoming".equals(status)) {
            long startMillis = t.getStartTimeMillis();
            long diff = startMillis - System.currentTimeMillis();

            if (diff <= 0) {
                h.time.setText("Starting Soon");
            } else {
                CountDownTimer timer = new CountDownTimer(diff, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long hours = millisUntilFinished / (1000 * 60 * 60);
                        long mins = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60);
                        long secs = (millisUntilFinished % (1000 * 60)) / 1000;

                        h.time.setText(
                                String.format(Locale.getDefault(),
                                        "%02d:%02d:%02d", hours, mins, secs)
                        );
                    }

                    @Override
                    public void onFinish() {
                        h.time.setText("Starting Soon");
                    }
                };
                timer.start();
                activeTimers.put(pos, timer);
            }
        } else {
            h.time.setText(t.getTimeDisplay());
        }

        // ✅ FIX: Show all buttons ONLY for "live" status, hide for others
        // Rules button is always visible for all categories
        if ("live".equals(status)) {
            h.edit.setVisibility(View.VISIBLE);
            h.submit.setVisibility(View.VISIBLE);
            h.result.setVisibility(View.VISIBLE);
            h.end.setVisibility(View.VISIBLE);
        } else {
            h.edit.setVisibility(View.GONE);
            h.submit.setVisibility(View.GONE);
            h.result.setVisibility(View.GONE);
            h.end.setVisibility(View.GONE);
        }

        // Rules button always visible
        h.rules.setVisibility(View.VISIBLE);

        h.edit.setOnClickListener(v -> listener.onEditRoom(t));
        h.submit.setOnClickListener(v -> listener.onSubmitResult(t));
        h.result.setOnClickListener(v -> listener.onViewResult(t));
        h.rules.setOnClickListener(v -> listener.onViewRules(t));
        h.end.setOnClickListener(v -> listener.onEndTournament(t));
    }

    @Override
    public int getItemCount() {
        return list.size();
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
        TextView edit, submit, result, rules, end;

        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.tournamentTitle);
            mode = v.findViewById(R.id.tournamentMode);
            slots = v.findViewById(R.id.slots);
            time = v.findViewById(R.id.timeStatus);
            roomId = v.findViewById(R.id.roomId);
            password = v.findViewById(R.id.password);

            edit = v.findViewById(R.id.editIcon);
            submit = v.findViewById(R.id.submitResultBtn);
            result = v.findViewById(R.id.resultButton);
            rules = v.findViewById(R.id.rulesButton);
            end = v.findViewById(R.id.endBtn);
        }
    }
}