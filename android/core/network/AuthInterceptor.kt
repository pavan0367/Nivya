package com.nivya.core.network

import com.nivya.core.security.SecureTokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor appending Bearer access token to authenticated requests.
 */
class AuthInterceptor(
    private val tokenStorage: SecureTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for registration/login endpoints
        val path = originalRequest.url.encodedPath
        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenStorage.getAccessToken()
        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
