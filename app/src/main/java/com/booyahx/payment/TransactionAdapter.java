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

        // ── Time display at bottom-right of each card ────────────────────────
        // ROOT CAUSE FIX: getDateTime() returns only the pre-formatted date string
        // e.g. "Feb 18" — it has no time component, so all old parsing branches
        // produced an empty string and tvTime never showed anything.
        //
        // The API response includes "createdAt":"2026-02-17T17:20:07.653Z" (full ISO).
        // We now use getCreatedAt() as the primary source for time extraction.
        // getDateTime() is still used only for the date label (tvDateTime) — unchanged.
        //
        // Converts ISO "2026-02-17T17:20:07.652Z" → local 12-hour "05:20 PM"
        String timeStr = "";
        try {
            String rawCreatedAt = transaction.getCreatedAt(); // full ISO-8601 from API
            if (rawCreatedAt != null && rawCreatedAt.contains("T")) {
                // "2026-02-17T17:20:07.652Z" → split on T → "17:20:07.652Z"
                String timePart = rawCreatedAt.split("T")[1];
                int hour   = Integer.parseInt(timePart.substring(0, 2));
                int minute = Integer.parseInt(timePart.substring(3, 5));
                String amPm = hour >= 12 ? "PM" : "AM";
                int hour12  = hour % 12;
                if (hour12 == 0) hour12 = 12;
                timeStr = String.format("%02d:%02d %s", hour12, minute, amPm);
            } else if (rawCreatedAt != null && rawCreatedAt.contains(" ")) {
                // Fallback: space-separated "Feb 17 5:30 PM"
                int firstSpace  = rawCreatedAt.indexOf(' ');
                int secondSpace = rawCreatedAt.indexOf(' ', firstSpace + 1);
                if (secondSpace != -1) {
                    timeStr = rawCreatedAt.substring(secondSpace + 1).trim();
                }
            }
        } catch (Exception e) {
            // Fallback to getDateTime() if getCreatedAt() is unavailable or throws
            String dateTime = transaction.getDateTime();
            if (dateTime != null && dateTime.contains(",")) {
                timeStr = dateTime.substring(dateTime.lastIndexOf(",") + 1).trim();
            } else if (dateTime != null && dateTime.contains("T")) {
                try {
                    String timePart = dateTime.split("T")[1];
                    timeStr = timePart.substring(0, 5); // "17:20"
                } catch (Exception ignored) {
                    timeStr = "";
                }
            } else if (dateTime != null && dateTime.contains(" ")) {
                int firstSpace  = dateTime.indexOf(' ');
                int secondSpace = dateTime.indexOf(' ', firstSpace + 1);
                if (secondSpace != -1) {
                    timeStr = dateTime.substring(secondSpace + 1).trim();
                }
            }
        }
        // Null-safe: tvTime is null if item_transaction.xml doesn't have R.id.tvTime yet
        if (holder.tvTime != null) {
            holder.tvTime.setText(timeStr);
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
        // ── Time view added at bottom-right of each card ─────────────────────
        TextView tvTime;
        // ─────────────────────────────────────────────────────────────────────

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime    = itemView.findViewById(R.id.tvDateTime);
            tvType        = itemView.findViewById(R.id.tvType);
            tvAmount      = itemView.findViewById(R.id.tvAmount);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            // ── Wire tvTime ──────────────────────────────────────────────────
            tvTime        = itemView.findViewById(R.id.tvTime);
            // ─────────────────────────────────────────────────────────────────
        }
    }
}