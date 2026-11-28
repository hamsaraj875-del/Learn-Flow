
package com.example.learnflow.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

object SessionCookieJar : CookieJar {

    private const val TAG = "SessionCookieJar"
    private const val PREFS_NAME = "session_cookies"

    private lateinit var preferences: SharedPreferences

    @Synchronized
    fun initialize(context: Context) {
        if (!::preferences.isInitialized) {
            preferences = context.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            Log.d(TAG, "Cookie jar initialized")
        }
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        synchronized(this) {
            if (!::preferences.isInitialized) {
                Log.e(TAG, "Cookie jar is not initialized")
                return
            }

            val existing = loadForRequest(url)
                .associateBy { it.name }
                .toMutableMap()

            cookies.forEach { cookie ->
                if (cookie.expiresAt <= System.currentTimeMillis()) {
                    existing.remove(cookie.name)
                } else {
                    existing[cookie.name] = cookie
                }
            }

            preferences.edit()
                .putString(
                    url.host,
                    existing.values.joinToString("\n") {
                        it.toString()
                    }
                )
                .apply()

            Log.d(
                TAG,
                "Received ${cookies.size} cookie(s) from ${url.host}"
            )
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(this) {
            if (!::preferences.isInitialized) {
                Log.e(TAG, "Cookie jar is not initialized")
                return emptyList()
            }

            val saved = preferences.getString(url.host, null)
                ?: return emptyList()

            val cookies = saved.split("\n")
                .mapNotNull { Cookie.parse(url, it) }
                .filter { it.expiresAt > System.currentTimeMillis() }

            Log.d(
                TAG,
                "Sending ${cookies.size} cookie(s) to ${url.host}"
            )

            return cookies
        }
    }

    @Synchronized
    fun clearCookies() {
        if (::preferences.isInitialized) {
            preferences.edit().clear().apply()
            Log.d(TAG, "Cookies cleared")
        }
    }
}
