package com.booyahx.notifications;

import com.booyahx.notifications.NotificationActivity;
import com.booyahx.notifications.NotificationType;
import com.booyahx.notifications.NotificationAdapter;
import com.booyahx.notifications.NotificationItem;
import com.booyahx.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<NotificationItem> notifications;
    private final OnItemRemoveListener removeListener;

    public interface OnItemRemoveListener {
        void onRemove(int position);
    }

    public NotificationAdapter(
            List<NotificationItem> notifications,
            OnItemRemoveListener listener
    ) {
        this.notifications = notifications;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        NotificationItem item = notifications.get(position);

        holder.title.setText(item.getTitle());
        holder.message.setText(item.getMessage());
        holder.time.setText(item.getTime());
        holder.icon.setImageResource(item.getType().getIconResource());

        holder.closeButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                removeListener.onRemove(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, message, time;
        ImageView icon, closeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            time = itemView.findViewById(R.id.notificationTime);
            icon = itemView.findViewById(R.id.notificationIcon);
            closeButton = itemView.findViewById(R.id.closeButton);
        }
    }
}