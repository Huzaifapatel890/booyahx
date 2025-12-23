package com.booyahx.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.booyahx.R;
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactionList;

    public TransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
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

        holder.tvDateTime.setText(transaction.getDateTime());
        holder.tvType.setText(transaction.getType());
        holder.tvAmount.setText(transaction.getAmount());
        holder.tvDescription.setText(transaction.getDescription());
        holder.tvStatus.setText(transaction.getStatus());

        // Set amount color based on positive/negative
        if (transaction.isPositive()) {
            holder.tvAmount.setTextColor(Color.parseColor("#00FF88"));
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#FF4444"));
        }

        // Set status background and color
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
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
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