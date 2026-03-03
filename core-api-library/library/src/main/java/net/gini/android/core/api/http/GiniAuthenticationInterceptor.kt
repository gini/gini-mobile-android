package net.gini.android.core.api.http

import kotlinx.coroutines.Dispatchers
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
 *
 * ## Threading and Performance Considerations
 *
 * **Important:** This interceptor uses `runBlocking` to bridge between OkHttp's synchronous
 * interceptor API and the asynchronous `SessionManager.getSession()` suspend function.
 * This blocks the calling thread (typically an OkHttp worker thread) during token fetch.
 *
 * **Performance Impact:**
 * - Under normal operation, tokens are cached and fetched quickly (~milliseconds)
 * - First-time user creation may take longer (network round-trip)
 * - Under high load with many parallel requests, thread pool contention is possible
 * - The [accessTokenMutex] serializes token fetches to prevent duplicate user creation
 *
 * **Why runBlocking is necessary:**
 * OkHttp interceptors must be synchronous, but `SessionManager` is designed as a
 * suspend function to integrate with coroutine-based repositories and services.
 * Alternative approaches (pre-fetching tokens, synchronous SessionManager) would
 * complicate the architecture or break existing async patterns.
 *
 * **Trade-off:** We accept blocking a worker thread briefly for the benefit of
 * automatic authentication and cleaner architecture (no manual token passing).
 */
internal class GiniAuthenticationInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    /**
     * Mutex to prevent coroutines from retrieving access tokens in parallel.
     * 
     * This ensures that even when multiple uploads are started simultaneously, only one
     * anonymous user is created on the backend. Without this mutex, parallel document
     * uploads could trigger multiple user creation requests, leading to race conditions.
     * 
     * **Thread Safety:** This mutex is internal (not private) to allow visibility in tests
     * that verify concurrent request handling, though it should never be accessed directly
     * by production code.
     * 
     * Moved from DocumentRepository to centralize auth logic in the interceptor layer.
     */
    internal val accessTokenMutex = Mutex()

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
        // Use Dispatchers.IO to avoid blocking the main thread pool
        val accessToken = try {
            runBlocking(Dispatchers.IO) {
                accessTokenMutex.withLock {
                    when (val sessionResult = sessionManager.getSession()) {
                        is Resource.Success -> sessionResult.data.accessToken
                        is Resource.Error -> {
                            throw IOException(
                                "Failed to get session for ${originalRequest.method} ${originalRequest.url}: ${sessionResult.exception?.message}",
                                sessionResult.exception
                            )
                        }
                        is Resource.Cancelled -> {
                            throw IOException("Session fetch was cancelled for ${originalRequest.method} ${originalRequest.url}")
                        }
                    }
                }
            }
        } catch (e: IOException) {
            // Re-throw IOExceptions directly (already have descriptive error messages)
            throw e
        } catch (e: Exception) {
            // Only wrap unexpected exceptions (e.g. NullPointerException, IllegalStateException)
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
            url.endsWith("/users") && method == "POST" -> true
            else -> false
        }
    }
}
