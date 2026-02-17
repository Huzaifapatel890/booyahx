package com.booyahx.adapters;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.booyahx.R;
import com.booyahx.network.models.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter for displaying chat messages in RecyclerView
 * Supports three message types: SENT, RECEIVED, and HOST
 *
 * ANIMATION ADDED (all original code preserved):
 *   - TYPE_SENT     → slide in from RIGHT + fade  (DecelerateInterpolator)
 *   - TYPE_RECEIVED → slide in from LEFT  + fade  (DecelerateInterpolator)
 *   - TYPE_HOST     → slide in from LEFT  + fade  + scale pop (OvershootInterpolator)
 *
 *   History messages on open      → NO animation (instant, clean load)
 *   New live messages thereafter  → ANIMATED slide in
 *   Scroll/recycle re-binds       → NO re-animation (HashSet tracks seen positions)
 *
 *   Requires one call in TournamentChatActivity — see markHistoryLoaded() below.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── YOUR ORIGINAL FIELDS (untouched) ─────────────────────────────
    private List<ChatMessage> messages;
    private SimpleDateFormat timeFormat;

    // ── ANIMATION FIELDS (added) ─────────────────────────────────────
    /** Positions already animated — prevents re-animation on RecyclerView recycle */
    private final Set<Integer> animatedPositions = new HashSet<>();
    /** Messages below this index are history → no animation */
    private int historyBoundary = 0;
    private boolean historyLoaded = false;

    private static final int   ANIM_DURATION_MS = 280;
    private static final float ANIM_SLIDE_OFFSET = 350f;
    private static final float ANIM_SCALE_FROM   = 0.88f;

    // ── YOUR ORIGINAL CONSTRUCTOR (untouched) ────────────────────────
    public ChatAdapter() {
        this.messages = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    // ── NEW: ANIMATION ACTIVATION METHOD ─────────────────────────────
    /**
     * Call this AFTER your history loop finishes in handleMessageHistoryFromAPI().
     * All messages added after this point will animate in as live messages.
     *
     * Also call it in the empty-history branch so live messages animate even
     * when there's no prior chat history to load.
     */
    public void markHistoryLoaded() {
        historyBoundary = messages.size();
        historyLoaded   = true;
    }

    // ── YOUR ORIGINAL getItemViewType (untouched) ────────────────────
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getMessageType();
    }

    // ── YOUR ORIGINAL onCreateViewHolder (untouched) ─────────────────
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

    // ── YOUR ORIGINAL onBindViewHolder + ANIMATION INJECTED AT END ───
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        String formattedTime = timeFormat.format(new Date(message.getTimestamp()));

        // YOUR ORIGINAL BIND LOGIC — completely untouched
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message, formattedTime);
        } else if (holder instanceof HostMessageViewHolder) {
            ((HostMessageViewHolder) holder).bind(message, formattedTime);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message, formattedTime);
        }

        // ANIMATION — injected after bind, never interferes with your logic
        boolean shouldAnimate = historyLoaded
                && position >= historyBoundary
                && !animatedPositions.contains(position);

        if (shouldAnimate) {
            animatedPositions.add(position);
            animateIn(holder.itemView, message.getMessageType());
        } else {
            // Clear any leftover transform from a previously recycled holder
            resetView(holder.itemView, message.getMessageType());
        }
    }

    // ── YOUR ORIGINAL getItemCount (untouched) ───────────────────────
    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ── YOUR ORIGINAL addMessage (untouched) ─────────────────────────
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    // ── YOUR ORIGINAL addMessages (untouched) ────────────────────────
    public void addMessages(List<ChatMessage> newMessages) {
        int startPosition = messages.size();
        messages.addAll(newMessages);
        notifyItemRangeInserted(startPosition, newMessages.size());
    }

    // ── YOUR ORIGINAL clearMessages (untouched) ──────────────────────
    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    // ════════════════════════════════════════════════════════════════
    //  ANIMATION HELPERS (new private methods — no name conflicts)
    // ════════════════════════════════════════════════════════════════

    private void animateIn(View view, int type) {
        view.setAlpha(0f);
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator fade = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        if (type == ChatMessage.TYPE_SENT) {
            // Slide from right — snappy, confident
            view.setTranslationX(ANIM_SLIDE_OFFSET);
            ObjectAnimator slide = ObjectAnimator.ofFloat(view, "translationX", ANIM_SLIDE_OFFSET, 0f);
            slide.setInterpolator(new DecelerateInterpolator(2f));
            fade.setInterpolator(new DecelerateInterpolator(1.5f));
            set.playTogether(fade, slide);
            set.setDuration(ANIM_DURATION_MS);

        } else if (type == ChatMessage.TYPE_HOST) {
            // Slide from left + scale pop — authoritative feel
            view.setTranslationX(-ANIM_SLIDE_OFFSET);
            view.setScaleX(ANIM_SCALE_FROM);
            view.setScaleY(ANIM_SCALE_FROM);
            ObjectAnimator slide  = ObjectAnimator.ofFloat(view, "translationX", -ANIM_SLIDE_OFFSET, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", ANIM_SCALE_FROM, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", ANIM_SCALE_FROM, 1f);
            OvershootInterpolator overshoot = new OvershootInterpolator(1.2f);
            slide.setInterpolator(new DecelerateInterpolator(2f));
            scaleX.setInterpolator(overshoot);
            scaleY.setInterpolator(overshoot);
            fade.setInterpolator(new DecelerateInterpolator(1.5f));
            set.playTogether(fade, slide, scaleX, scaleY);
            set.setDuration(ANIM_DURATION_MS + 40);

        } else { // TYPE_RECEIVED
            // Slide from left — clean, neutral
            view.setTranslationX(-ANIM_SLIDE_OFFSET);
            ObjectAnimator slide = ObjectAnimator.ofFloat(view, "translationX", -ANIM_SLIDE_OFFSET, 0f);
            slide.setInterpolator(new DecelerateInterpolator(2f));
            fade.setInterpolator(new DecelerateInterpolator(1.5f));
            set.playTogether(fade, slide);
            set.setDuration(ANIM_DURATION_MS);
        }

        set.start();
    }

    private void resetView(View view, int type) {
        view.setAlpha(1f);
        view.setTranslationX(0f);
        if (type == ChatMessage.TYPE_HOST) {
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  YOUR ORIGINAL VIEW HOLDERS (completely untouched)
    // ════════════════════════════════════════════════════════════════

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