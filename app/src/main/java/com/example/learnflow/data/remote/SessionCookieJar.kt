package com.example.learnflow.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

object SessionCookieJar : CookieJar {

    private val cookies = mutableMapOf<String, List<Cookie>>()

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        synchronized(this.cookies) {
            this.cookies[url.host] = cookies
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return synchronized(cookies) {
            cookies[url.host] ?: emptyList()
        }
    }
}