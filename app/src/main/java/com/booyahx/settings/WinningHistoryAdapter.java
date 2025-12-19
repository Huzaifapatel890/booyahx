package com.booyahx.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;

import java.util.List;

public class WinningHistoryAdapter extends RecyclerView.Adapter<WinningHistoryAdapter.ViewHolder> {

    private List<WinningHistoryItem> list;

    public WinningHistoryAdapter(List<WinningHistoryItem> list) {
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_winning_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int pos) {
        WinningHistoryItem item = list.get(pos);

        h.txtAmount.setText("+" + item.amountGC + " GC");
        h.txtDescription.setText(item.description);
        h.txtDate.setText(item.date);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAmount, txtDescription, txtDate;

        ViewHolder(View v) {
            super(v);
            txtAmount = v.findViewById(R.id.txtAmount);
            txtDescription = v.findViewById(R.id.txtDescription);
            txtDate = v.findViewById(R.id.txtDate);
        }
    }
}