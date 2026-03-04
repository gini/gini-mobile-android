package net.gini.android.core.api.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.SessionManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Internal use only.
 *
 * OkHttp authenticator that handles token refresh when a request receives 401 Unauthorized.
 * Uses [SessionManager] to refresh the session and retry the request with a new token.
 *
 * This authenticator:
 * - Intercepts 401 responses
 * - Fetches a fresh token from SessionManager
 * - Retries the request with the new token
 * - Gives up after one retry to avoid infinite loops
 *
 * ## Threading and Performance Considerations
 *
 * **Important:** This authenticator uses `runBlocking` to bridge between OkHttp's synchronous
 * authenticator API and the asynchronous `SessionManager.getSession()` suspend function.
 * This blocks the calling thread (typically an OkHttp worker thread) during token refresh.
 *
 * **Performance Impact:**
 * - Token refresh involves a network round-trip to the auth server (~100-500ms typical)
 * - During refresh, the OkHttp worker thread is blocked and unavailable for other requests
 * - If multiple requests fail with 401 simultaneously, the [refreshMutex] ensures only
 *   one refresh happens; other threads wait for it to complete
 * - This is acceptable because 401 responses are rare (only when tokens expire)
 *
 * **Why runBlocking is necessary:**
 * OkHttp authenticators must be synchronous, but `SessionManager` is designed as a
 * suspend function. We cannot make the authenticator async without changing OkHttp's API.
 *
 * **Trade-off:** Brief thread blocking during rare token refresh is acceptable for
 * automatic 401 handling without manual retry logic in business code.
 */
internal class GiniAuthenticator(
    private val sessionManager: SessionManager
) : Authenticator {

    /**
     * Mutex to prevent parallel token refresh attempts.
     * 
     * When multiple requests fail with 401 simultaneously (e.g., token expires while
     * multiple API calls are in flight), only one should trigger a token refresh.
     * The other requests will wait for the refresh to complete and retry with the new token.
     * 
     * **Important:** This is separate from [GiniAuthenticationInterceptor.accessTokenMutex]
     * because they serve different purposes:
     * - `accessTokenMutex`: Prevents parallel first-time authentication (user creation)
     * - `refreshMutex`: Prevents parallel token refresh on 401 responses
     * 
     * **Why separate mutexes?** Sharing a single mutex would serialize all authentication
     * operations, causing unnecessary bottlenecks. The interceptor mutex handles the common
     * case (adding tokens to requests), while this mutex handles the rare case (refreshing
     * expired tokens). Keeping them separate allows better concurrency.
     * 
     * **Thread Safety:** This mutex is internal (not private) to allow visibility in tests
     * that verify concurrent 401 handling, though it should never be accessed directly
     * by production code.
     */
    internal val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Check if this is a 401 Unauthorized response
        if (response.code != 401) {
            return null // Not an auth issue, let other error handlers deal with it
        }

        // Prevent infinite retry loop - if we already retried once, give up
        if (responseCount(response) >= 2) {
            return null // Failed after retry, give up
        }

        // Fetch fresh token synchronously
        // Use Dispatchers.IO to avoid blocking the main thread pool
        val newAccessToken = try {
            runBlocking(Dispatchers.IO) {
                refreshMutex.withLock {
                    when (val sessionResult = sessionManager.getSession()) {
                        is Resource.Success -> sessionResult.data.accessToken
                        is Resource.Error -> {
                            // Token refresh failed, give up
                            null
                        }
                        is Resource.Cancelled -> {
                            null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            null
        }

        // If we couldn't get a new token, give up
        if (newAccessToken == null) {
            return null
        }

        // Retry the request with new token
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    /**
     * Counts how many times this request has been retried due to 401.
     * Used to prevent infinite retry loops.
     */
    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}
