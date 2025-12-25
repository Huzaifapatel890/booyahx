package com.booyahx.notifications;

import com.booyahx.R;

public enum NotificationType {

    ROOM_UPDATE(R.drawable.ic_lock),
    TOURNAMENT(R.drawable.ic_check),
    REWARD(R.drawable.ic_star),
    VICTORY(R.drawable.ic_trophy),
    SQUAD(R.drawable.ic_person),
    SYSTEM(R.drawable.ic_settings);

    private final int iconResource;

    NotificationType(int iconResource) {
        this.iconResource = iconResource;
    }

    public int getIconResource() {
        return iconResource;
    }
}