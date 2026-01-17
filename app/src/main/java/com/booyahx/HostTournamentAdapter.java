package com.booyahx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.network.models.HostTournament;

import java.util.List;

public class HostTournamentAdapter
        extends RecyclerView.Adapter<HostTournamentAdapter.ViewHolder> {

    private Context context;
    private List<HostTournament> tournaments;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditRoom(HostTournament tournament);
        void onSubmitResult(HostTournament tournament);
        void onViewResult(HostTournament tournament);
        void onEndTournament(HostTournament tournament);
        void onViewRules(HostTournament tournament); // Added for Rules button
    }

    public HostTournamentAdapter(
            Context context,
            List<HostTournament> tournaments,
            OnItemClickListener listener
    ) {
        this.context = context;
        this.tournaments = tournaments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_host_tournament_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        HostTournament tournament = tournaments.get(position);

        holder.tournamentTitle.setText(tournament.getTitle());
        holder.tournamentMode.setText(tournament.getMode());
        holder.entryFee.setText(tournament.getEntryFee());
        holder.prizePool.setText(tournament.getPrizePool());
        holder.slots.setText(tournament.getSlots());
        holder.timeStatus.setText(tournament.getTimeStatus());
        holder.roomId.setText(tournament.getRoomId());
        holder.password.setText(tournament.getPassword());

        // Edit Room
        holder.editIcon.setOnClickListener(v ->
                listener.onEditRoom(tournament)
        );

        // Submit Result
        holder.submitResultBtn.setOnClickListener(v ->
                listener.onSubmitResult(tournament)
        );

        // View Result - THIS IS THE KEY FIX
        holder.resultBtn.setOnClickListener(v ->
                listener.onViewResult(tournament)
        );

        // View Rules
        holder.rulesBtn.setOnClickListener(v ->
                listener.onViewRules(tournament)
        );

        // End Tournament
        holder.endBtn.setOnClickListener(v ->
                listener.onEndTournament(tournament)
        );
    }

    @Override
    public int getItemCount() {
        return tournaments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tournamentTitle, tournamentMode, entryFee, prizePool,
                slots, timeStatus, roomId, password, slotBadge;

        TextView editIcon, submitResultBtn, resultBtn, rulesBtn, endBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tournamentTitle = itemView.findViewById(R.id.tournamentTitle);
            tournamentMode = itemView.findViewById(R.id.tournamentMode);
            entryFee = itemView.findViewById(R.id.entryFee);
            prizePool = itemView.findViewById(R.id.prizePool);
            slots = itemView.findViewById(R.id.slots);
            timeStatus = itemView.findViewById(R.id.timeStatus);
            roomId = itemView.findViewById(R.id.roomId);
            password = itemView.findViewById(R.id.password);

            editIcon = itemView.findViewById(R.id.editIcon);
            submitResultBtn = itemView.findViewById(R.id.submitResultBtn);
            resultBtn = itemView.findViewById(R.id.resultButton);
            rulesBtn = itemView.findViewById(R.id.rulesButton);
            endBtn = itemView.findViewById(R.id.endBtn);
        }
    }
}