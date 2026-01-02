package com.booyahx.helpandsupport;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.booyahx.R;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private List<Ticket> tickets;

    public TicketAdapter(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.tvTicketId.setText("#" + ticket.getTicketId());
        holder.tvTicketSubject.setText(ticket.getSubject());
        holder.tvTicketDate.setText("Created: " + ticket.getDate());

        if (ticket.isOpen()) {
            holder.tvTicketStatus.setText("OPEN");
            holder.tvTicketStatus.setBackgroundResource(R.drawable.status_open_bg);
            holder.tvTicketStatus.setTextColor(Color.parseColor("#FFC107"));
        } else {
            holder.tvTicketStatus.setText("CLOSED");
            holder.tvTicketStatus.setBackgroundResource(R.drawable.status_closed_bg);
            holder.tvTicketStatus.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    public void updateTickets(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketId, tvTicketSubject, tvTicketDate, tvTicketStatus;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
            tvTicketSubject = itemView.findViewById(R.id.tvTicketSubject);
            tvTicketDate = itemView.findViewById(R.id.tvTicketDate);
            tvTicketStatus = itemView.findViewById(R.id.tvTicketStatus);
        }
    }
}
