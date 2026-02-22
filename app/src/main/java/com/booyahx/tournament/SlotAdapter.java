package com.booyahx.tournament;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;

import java.util.List;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.SlotViewHolder> {

    public static class SlotItem {
        public final int     slotNumber;
        public final String  teamName;
        public final boolean isFilled;
        public final boolean isYou;

        public SlotItem(int slotNumber, String teamName, boolean isFilled, boolean isYou) {
            this.slotNumber = slotNumber;
            this.teamName   = teamName;
            this.isFilled   = isFilled;
            this.isYou      = isYou;
        }
    }

    private final List<SlotItem> items;

    public SlotAdapter(List<SlotItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.slot_card, parent, false);
        return new SlotViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {

        TextView tvSlotNum;
        TextView tvTeamName;
        TextView tvYouBadge;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSlotNum  = itemView.findViewById(R.id.tvSlotNum);
            tvTeamName = itemView.findViewById(R.id.tvSlotTeamName);
            tvYouBadge = itemView.findViewById(R.id.tvSlotYouBadge);
        }

        void bind(SlotItem item) {
            tvSlotNum.setText("SLOT #" + item.slotNumber);

            if (item.isYou) {
                itemView.setBackgroundResource(R.drawable.bg_slot_item_you);
                tvTeamName.setText(
                        item.teamName != null && !item.teamName.isEmpty()
                                ? item.teamName.toUpperCase()
                                : "YOUR TEAM"
                );
                tvTeamName.setTextColor(0xFF00E5FF); // cyan
                tvYouBadge.setVisibility(View.VISIBLE);

            } else if (item.isFilled) {
                itemView.setBackgroundResource(R.drawable.bg_slot_item_normal);
                tvTeamName.setText(
                        item.teamName != null ? item.teamName.toUpperCase() : ""
                );
                tvTeamName.setTextColor(0xFFE2E8F0); // white-ish
                tvYouBadge.setVisibility(View.GONE);

            } else {
                itemView.setBackgroundResource(R.drawable.bg_slot_item_open);
                tvTeamName.setText("Open slot");
                tvTeamName.setTextColor(0xFF3A4A5A); // muted
                tvYouBadge.setVisibility(View.GONE);
            }
        }
    }
}