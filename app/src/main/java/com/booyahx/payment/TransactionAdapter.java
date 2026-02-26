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
import com.booyahx.WalletFragment;
import com.booyahx.network.models.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactionList = new ArrayList<>();

    // Reference to the fragment so we can call cancelWithdrawal()
    private WalletFragment walletFragment;

    public TransactionAdapter(Context context, List<Transaction> list, WalletFragment fragment) {
        this.context = context;
        this.walletFragment = fragment;
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

        // Capitalize type label
        String type = transaction.getType();
        if (type != null && !type.isEmpty()) {
            // Normalise: "withdrawal" → "Withdraw"
            if (type.equalsIgnoreCase("withdrawal")) type = "Withdraw";
            else type = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
        }
        holder.tvType.setText(type);

        holder.tvAmount.setText(transaction.getAmount());
        holder.tvDescription.setText(transaction.getDescription());

        // Format status
        String status = transaction.getStatus();
        if (status != null && !status.isEmpty()) {
            if (status.equalsIgnoreCase("fail")) {
                status = "Failed";
            } else {
                status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
            }
        }
        holder.tvStatus.setText(status);

        // Status colour + amount colour
        String statusLower = transaction.getStatus() != null
                ? transaction.getStatus().toLowerCase() : "";
        switch (statusLower) {
            case "success":
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.status_completed_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#00FF88"));
                holder.tvAmount.setTextColor(Color.parseColor("#00FF88"));
                break;

            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#FFB800"));
                holder.tvAmount.setTextColor(Color.parseColor("#FFB800"));
                break;

            case "fail":
            case "failed":
            case "cancelled":
                holder.tvStatus.setBackgroundResource(R.drawable.status_failed_bg);
                holder.tvStatus.setTextColor(Color.parseColor("#FF4444"));
                holder.tvAmount.setTextColor(Color.parseColor("#FF4444"));
                break;

            default:
                holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg);
                holder.tvStatus.setTextColor(Color.WHITE);
                holder.tvAmount.setTextColor(Color.WHITE);
                break;
        }

        // ── Time display ──────────────────────────────────────────────────────
        String timeStr = "";
        try {
            String rawCreatedAt = transaction.getCreatedAt();
            if (rawCreatedAt != null && rawCreatedAt.contains("T")) {
                String timePart = rawCreatedAt.split("T")[1];
                int hour   = Integer.parseInt(timePart.substring(0, 2));
                int minute = Integer.parseInt(timePart.substring(3, 5));
                String amPm = hour >= 12 ? "PM" : "AM";
                int hour12  = hour % 12;
                if (hour12 == 0) hour12 = 12;
                timeStr = String.format("%02d:%02d %s", hour12, minute, amPm);
            } else if (rawCreatedAt != null && rawCreatedAt.contains(" ")) {
                int f = rawCreatedAt.indexOf(' ');
                int s = rawCreatedAt.indexOf(' ', f + 1);
                if (s != -1) timeStr = rawCreatedAt.substring(s + 1).trim();
            }
        } catch (Exception e) {
            String dateTime = transaction.getDateTime();
            if (dateTime != null && dateTime.contains(",")) {
                timeStr = dateTime.substring(dateTime.lastIndexOf(",") + 1).trim();
            } else if (dateTime != null && dateTime.contains("T")) {
                try {
                    timeStr = dateTime.split("T")[1].substring(0, 5);
                } catch (Exception ignored) { }
            }
        }
        if (holder.tvTime != null) holder.tvTime.setText(timeStr);
        // ─────────────────────────────────────────────────────────────────────

        // ── Cancel button: visible only for PENDING withdrawals ───────────────
        String rawType = transaction.getType() != null ? transaction.getType().toLowerCase() : "";
        boolean isWithdrawal = rawType.equals("withdraw") || rawType.equals("withdrawal");
        boolean isPending    = statusLower.equals("pending");

        if (holder.btnCancelWithdrawal != null) {
            if (isWithdrawal && isPending && transaction.getId() != null) {
                holder.btnCancelWithdrawal.setVisibility(View.VISIBLE);
                holder.btnCancelWithdrawal.setOnClickListener(v -> {
                    if (walletFragment != null) {
                        // Disable button immediately to prevent double-tap
                        holder.btnCancelWithdrawal.setEnabled(false);
                        holder.btnCancelWithdrawal.setAlpha(0.5f);
                        walletFragment.cancelWithdrawal(
                                transaction.getId(),
                                holder.getAdapterPosition()
                        );
                    }
                });
            } else {
                holder.btnCancelWithdrawal.setVisibility(View.GONE);
                holder.btnCancelWithdrawal.setOnClickListener(null);
            }
        }
        // ─────────────────────────────────────────────────────────────────────
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
        TextView tvTime;
        TextView btnCancelWithdrawal;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime           = itemView.findViewById(R.id.tvDateTime);
            tvType               = itemView.findViewById(R.id.tvType);
            tvAmount             = itemView.findViewById(R.id.tvAmount);
            tvStatus             = itemView.findViewById(R.id.tvStatus);
            tvDescription        = itemView.findViewById(R.id.tvDescription);
            tvTime               = itemView.findViewById(R.id.tvTime);
            btnCancelWithdrawal  = itemView.findViewById(R.id.btnCancelWithdrawal);
        }
    }
}