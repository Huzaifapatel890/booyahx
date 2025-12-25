package com.booyahx.notifications;

public class NotificationItem {

    private final String title;
    private final String message;
    private final String time;
    private final NotificationType type;

    public NotificationItem(
            String title,
            String message,
            String time,
            NotificationType type
    ) {
        this.title = title;
        this.message = message;
        this.time = time;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public NotificationType getType() { return type; }
}