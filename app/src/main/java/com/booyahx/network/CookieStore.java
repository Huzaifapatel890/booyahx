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

    private final CookieManager cookieManager;

    public CookieStore() {
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            try {
                HttpCookie httpCookie = new HttpCookie(cookie.name(), cookie.value());
                httpCookie.setPath(cookie.path());
                httpCookie.setDomain(cookie.domain());
                cookieManager.getCookieStore().add(url.uri(), httpCookie);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<HttpCookie> stored = cookieManager.getCookieStore().get(url.uri());
        List<Cookie> cookies = new ArrayList<>();

        if (stored != null) {
            for (HttpCookie httpCookie : stored) {
                cookies.add(new Cookie.Builder()
                        .name(httpCookie.getName())
                        .value(httpCookie.getValue())
                        .domain(url.host())
                        .path("/")
                        .build());
            }
        }

        return cookies;
    }
}