package net.gini.android.core.api.http

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
 */
internal class GiniAuthenticator(
    private val sessionManager: SessionManager
) : Authenticator {

    /**
     * Mutex to prevent parallel token refresh attempts.
     * When multiple requests fail with 401, only one should refresh the token.
     */
    private val refreshMutex = Mutex()

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
        val newAccessToken = try {
            runBlocking {
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
