package com.booyahx.network;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookieStore implements CookieJar {

    private static final CookieStore INSTANCE = new CookieStore();
    private final CookieManager manager;

    private CookieStore() {
        manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    public static CookieStore getInstance() {
        return INSTANCE;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie c : cookies) {
            manager.getCookieStore()
                    .add(url.uri(), new HttpCookie(c.name(), c.value()));
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<HttpCookie> stored = manager.getCookieStore().get(url.uri());
        List<Cookie> out = new ArrayList<>();

        for (HttpCookie hc : stored) {
            out.add(new Cookie.Builder()
                    .name(hc.getName())
                    .value(hc.getValue())
                    .domain(url.host())
                    .path("/")
                    .build());
        }
        return out;
    }
}