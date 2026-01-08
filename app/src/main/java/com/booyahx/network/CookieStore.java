package com.booyahx.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public final class CookieStore implements CookieJar {

    private static final CookieStore INSTANCE = new CookieStore();
    private final ConcurrentHashMap<String, List<Cookie>> store = new ConcurrentHashMap<>();

    private CookieStore() {}

    public static CookieStore getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        List<Cookie> existing = store.getOrDefault(url.host(), new ArrayList<>());

        // Remove expired cookies
        long now = System.currentTimeMillis();
        Iterator<Cookie> it = existing.iterator();
        while (it.hasNext()) {
            if (it.next().expiresAt() < now) {
                it.remove();
            }
        }

        // Replace cookies by name
        for (Cookie newCookie : cookies) {
            it = existing.iterator();
            while (it.hasNext()) {
                Cookie old = it.next();
                if (old.name().equals(newCookie.name())) {
                    it.remove();
                }
            }
            existing.add(newCookie);
        }

        store.put(url.host(), existing);
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = store.get(url.host());
        if (cookies == null) return new ArrayList<>();

        long now = System.currentTimeMillis();
        List<Cookie> valid = new ArrayList<>();

        for (Cookie c : cookies) {
            if (c.expiresAt() >= now && c.matches(url)) {
                valid.add(c);
            }
        }
        return valid;
    }

    // Helper method to clear cookies (useful for logout)
    public synchronized void clearAll() {
        store.clear();
    }

    public synchronized void clearForHost(String host) {
        store.remove(host);
    }
}