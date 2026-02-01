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
import com.booyahx.network.models.Transaction;

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

        // Capitalize first letter of type
        String type = transaction.getType();
        if (type != null && !type.isEmpty()) {
            type = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
        }
        holder.tvType.setText(type);

        holder.tvAmount.setText(transaction.getAmount());
        holder.tvDescription.setText(transaction.getDescription());

        // Format status - convert "fail" to "Failed"
        String status = transaction.getStatus();
        if (status != null && !status.isEmpty()) {
            if (status.equalsIgnoreCase("fail")) {
                status = "Failed";
            } else {
                status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
            }
        }
        holder.tvStatus.setText(status);

        // Set status background and color + amount color based on status
        String statusLower = transaction.getStatus() != null ? transaction.getStatus().toLowerCase() : "";
        switch (statusLower) {
            case "success":
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.status_completed_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#00FF88"));
                // 🔥 Amount color matches success color
                holder.tvAmount.setTextColor(Color.parseColor("#00FF88"));
                break;

            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#FFB800"));
                // 🔥 Amount color matches pending color
                holder.tvAmount.setTextColor(Color.parseColor("#FFB800"));
                break;

            case "fail":
            case "failed":
            case "cancelled":
                holder.tvStatus.setBackgroundResource(R.drawable.status_failed_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#FF4444"));
                // 🔥 Amount color matches failed color (red)
                holder.tvAmount.setTextColor(Color.parseColor("#FF4444"));
                break;

            default:
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg);
                holder.tvStatus.setTextColor(Color.WHITE);
                holder.tvAmount.setTextColor(Color.WHITE);
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