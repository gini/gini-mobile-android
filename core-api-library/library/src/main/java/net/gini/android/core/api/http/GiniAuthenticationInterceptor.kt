package net.gini.android.core.api.http

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Internal use only.
 *
 * OkHttp interceptor that automatically adds Bearer authentication to all requests.
 * Uses [SessionManager] internally to fetch access tokens.
 *
 * This interceptor:
 * - Fetches access tokens from SessionManager when needed
 * - Automatically adds "Authorization: Bearer <token>" header to requests
 * - Skips authentication for specific endpoints (OAuth token, user creation)
 * - Uses a mutex to prevent parallel token fetches (important for first-time user creation)
 */
internal class GiniAuthenticationInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    /**
     * Mutex to prevent coroutines from retrieving access tokens in parallel.
     * This ensures that even when multiple uploads are started, only one user is created.
     * Moved from DocumentRepository to centralize auth logic.
     */
    private val accessTokenMutex = Mutex()

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip authentication for specific endpoints
        if (shouldSkipAuthentication(originalRequest.url.toString(), originalRequest.method)) {
            return chain.proceed(originalRequest)
        }

        // Skip if Authorization header already present (manual override)
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        // Fetch access token synchronously (required in interceptor context)
        val accessToken = try {
            runBlocking {
                accessTokenMutex.withLock {
                    when (val sessionResult = sessionManager.getSession()) {
                        is Resource.Success -> sessionResult.data.accessToken
                        is Resource.Error -> {
                            throw IOException(
                                "Failed to get session: ${sessionResult.exception?.message}",
                                sessionResult.exception
                            )
                        }
                        is Resource.Cancelled -> {
                            throw IOException("Session fetch was cancelled")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw IOException("Authentication failed: ${e.message}", e)
        }

        // Add Bearer token to request
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    /**
     * Determines if authentication should be skipped for a request.
     *
     * Authentication is skipped for:
     * - OAuth token endpoint (/oauth/token)
     * - User creation endpoint (POST to /users)
     */
    private fun shouldSkipAuthentication(url: String, method: String): Boolean {
        return when {
            // OAuth token requests (client credentials, refresh token)
            url.contains("/oauth/token") -> true
            // User creation (anonymous user registration)
            url.contains("/users") && method == "POST" -> true
            else -> false
        }
    }
}
