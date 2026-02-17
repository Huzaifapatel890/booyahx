package com.booyahx.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.booyahx.R;
import com.booyahx.network.models.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying chat messages in RecyclerView
 * Supports three message types: SENT, RECEIVED, and HOST
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messages;
    private SimpleDateFormat timeFormat;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getMessageType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case ChatMessage.TYPE_SENT:
                View sentView = inflater.inflate(R.layout.item_chat_message_sent, parent, false);
                return new SentMessageViewHolder(sentView);

            case ChatMessage.TYPE_HOST:
                View hostView = inflater.inflate(R.layout.item_chat_message_host, parent, false);
                return new HostMessageViewHolder(hostView);

            case ChatMessage.TYPE_RECEIVED:
            default:
                View receivedView = inflater.inflate(R.layout.item_chat_message_recieved, parent, false);
                return new ReceivedMessageViewHolder(receivedView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        String formattedTime = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message, formattedTime);
        } else if (holder instanceof HostMessageViewHolder) {
            ((HostMessageViewHolder) holder).bind(message, formattedTime);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message, formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessages(List<ChatMessage> newMessages) {
        int startPosition = messages.size();
        messages.addAll(newMessages);
        notifyItemRangeInserted(startPosition, newMessages.size());
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    // ViewHolder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatMessage message, String time) {
            tvMessage.setText(message.getMessage());
            tvTime.setText(time);
        }
    }

    // ViewHolder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvMessage, tvTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatMessage message, String time) {
            tvSenderName.setText(message.getUsername());
            tvMessage.setText(message.getMessage());
            tvTime.setText(time);
        }
    }

    // ViewHolder for host messages
    static class HostMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvHostBadge, tvMessage, tvTime;

        HostMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvHostBadge = itemView.findViewById(R.id.tvHostBadge);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatMessage message, String time) {
            tvSenderName.setText(message.getUsername());
            tvMessage.setText(message.getMessage());
            tvTime.setText(time);
        }
    }
}