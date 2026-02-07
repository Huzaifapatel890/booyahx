package com.booyahx.socket;

import android.content.Context;

public class MyAppContextHolder {

    private static Context context;

    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
    }

    public static Context get() {
        return context;
    }
}