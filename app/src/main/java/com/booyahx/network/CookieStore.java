package com.booyahx.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public final class CookieStore implements CookieJar {

    private static final CookieStore INSTANCE = new CookieStore();

    private final Map<String, List<Cookie>> cookieMap = new HashMap<>();

    private CookieStore() {}

    public static CookieStore getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        cookieMap.put(url.host(), cookies);
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieMap.get(url.host());
        return cookies != null ? new ArrayList<>(cookies) : new ArrayList<>();
    }
}