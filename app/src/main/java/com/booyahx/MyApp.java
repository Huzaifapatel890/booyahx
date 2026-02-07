package com.booyahx;

import android.app.Application;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.booyahx.socket.MyAppContextHolder;
import com.booyahx.socket.SocketManager;

public class MyApp extends Application implements DefaultLifecycleObserver {

    private static boolean isForeground = false;
    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // ✅ FIX 1: Initialize context holder
        MyAppContextHolder.init(this);

        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(this);
    }

    public static MyApp getInstance() {
        return instance;
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        isForeground = true;
        // ✅ FIX 2: Only connect if socket exists (initialized with token in DashboardActivity)
        if (SocketManager.getSocket() != null) {
            SocketManager.connect();
        }
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        isForeground = false;
        SocketManager.disconnect();
    }

    public static boolean isAppInForeground() {
        return isForeground;
    }
}