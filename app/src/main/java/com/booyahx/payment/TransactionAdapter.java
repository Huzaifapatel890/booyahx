package com.booyahx.payment;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactionList = new ArrayList<>();

    public TransactionAdapter(Context context, List<Transaction> list) {
        this.context = context;
        if (list != null) this.transactionList = list;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        if (transaction == null) return;

        holder.tvDateTime.setText(transaction.getDateTime());
        holder.tvType.setText(transaction.getType());
        holder.tvAmount.setText(transaction.getAmount());
        holder.tvDescription.setText(transaction.getDescription());
        holder.tvStatus.setText(transaction.getStatus());

        holder.tvAmount.setTextColor(
                transaction.isPositive() ? Color.parseColor("#00FF88") : Color.parseColor("#FF4444")
        );

        switch (transaction.getStatus().toLowerCase()) {
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.status_completed_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#00FF88"));
                break;
            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#FFB800"));
                break;
            case "failed":
                holder.tvStatus.setBackgroundResource(R.drawable.status_failed_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#FF4444"));
                break;
            default:
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg);
                holder.tvStatus.setTextColor(Color.WHITE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void setTransactions(List<Transaction> list) {
        transactionList = list;
        notifyDataSetChanged();
    }

    public void addTransaction(Transaction transaction) {
        transactionList.add(0, transaction);
        notifyItemInserted(0);
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvType, tvAmount, tvStatus, tvDescription;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvType = itemView.findViewById(R.id.tvType);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}