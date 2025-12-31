package com.booyahx.notifications;

public class NotificationItem {

    private final String title;
    private final String message;
    private final long timestamp;
    private final NotificationType type;

    public NotificationItem(
            String title,
            String message,
            long timestamp,
            NotificationType type
    ) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public NotificationType getType() {
        return type;
    }

    /**
     * Calculates and returns human-readable time difference
     * e.g., "just now", "5m ago", "3h ago", "2d ago", "1w ago", "3mo ago"
     */
    public String getTimeAgo() {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 0) {
            return "just now";
        }

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 10) {
            return "just now";
        } else if (seconds < 60) {
            return seconds + "s ago";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days < 7) {
            return days + "d ago";
        } else if (weeks < 4) {
            return weeks + "w ago";
        } else if (months < 12) {
            return months + "mo ago";
        } else {
            return years + "y ago";
        }
    }
}