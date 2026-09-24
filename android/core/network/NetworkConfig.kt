package com.nivya.core.network

import com.nivya.BuildConfig

/**
 * Production-safe URL and endpoint resolution utility for Nivya.
 * Handles normalization between API base URL, Retrofit endpoint concatenation,
 * OkHttp token refresh endpoint, and STOMP WebSocket URL.
 */
object NetworkConfig {

    const val PROD_API_BASE_URL = "https://nivya-blbf.onrender.com/api/v1/"
    const val PROD_WS_URL = "wss://nivya-blbf.onrender.com/ws/websocket"

    /**
     * Resolves the base URL for Retrofit.
     * All endpoints in [NivyaApiService] specify paths starting with "api/v1/...".
     * If the configured baseUrl contains "/api/v1", we normalize to the server root with a trailing slash
     * so that Retrofit resolves paths to e.g. "https://nivya-blbf.onrender.com/api/v1/auth/register"
     * instead of duplicate "api/v1/api/v1/...".
     */
    fun getRetrofitBaseUrl(baseUrl: String = BuildConfig.API_BASE_URL): String {
        val clean = baseUrl.trim().trimEnd('/')
        val hostRoot = if (clean.endsWith("/api/v1")) {
            clean.removeSuffix("/api/v1")
        } else {
            clean
        }
        return if (hostRoot.endsWith("/")) hostRoot else "$hostRoot/"
    }

    /**
     * Resolves the token refresh endpoint URL for OkHttp token rotation.
     */
    fun getRefreshUrl(baseUrl: String = BuildConfig.API_BASE_URL): String {
        val clean = baseUrl.trim().trimEnd('/')
        return if (clean.endsWith("/api/v1")) {
            "$clean/auth/refresh"
        } else {
            "$clean/api/v1/auth/refresh"
        }
    }

    /**
     * Resolves the STOMP WebSocket URL from the base URL.
     * Replaces HTTP/HTTPS scheme with WS/WSS and appends the standard STOMP endpoint "/ws/websocket".
     */
    fun getWebSocketUrl(baseUrl: String = BuildConfig.API_BASE_URL): String {
        val clean = baseUrl.trim().trimEnd('/')
        val hostRoot = if (clean.endsWith("/api/v1")) {
            clean.removeSuffix("/api/v1")
        } else {
            clean
        }
        val wsScheme = if (hostRoot.startsWith("https://", ignoreCase = true)) {
            "wss://" + hostRoot.substring(8)
        } else if (hostRoot.startsWith("http://", ignoreCase = true)) {
            "ws://" + hostRoot.substring(7)
        } else {
            hostRoot
        }
        return "${wsScheme.trimEnd('/')}/ws/websocket"
    }

    /**
     * Returns true if the given URL uses a secure transport (https or wss).
     */
    fun isSecure(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.startsWith("https://") || lower.startsWith("wss://")
    }
}
