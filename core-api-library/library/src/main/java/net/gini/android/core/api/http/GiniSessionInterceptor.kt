package net.gini.android.core.api.http

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.BearerAuthorizatonHeader
import net.gini.android.core.api.requests.SessionCancellationException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor which authenticates API requests with an access token from the
 * [SessionManager].
 *
 * Requests which already carry an `Authorization` header are passed through untouched. This
 * covers:
 * - User Center API requests which use basic authentication (and must not trigger a session
 *   request to avoid infinite recursion),
 * - requests authenticated by the repositories via header maps (during the migration of the
 *   token handling into this interceptor),
 * - requests authenticated by a consumer's own interceptor when the consumer manages
 *   authentication themselves (a consumer's *application* interceptors run before this one).
 *
 * Note: auth added by a consumer via a *network* interceptor
 * (`OkHttpClient.Builder.addNetworkInterceptor`) runs after this interceptor, so the SDK would
 * still request a session (and create an anonymous Gini user) before the consumer's header
 * overwrites the SDK's one. Consumers who authenticate API requests themselves should enable
 * self-managed authentication (`GiniCoreAPIBuilder.setSelfManagedAuthentication`) instead of
 * relying on the pass-through: it prevents this interceptor from being installed at all.
 *
 * The session request is guarded by a [Mutex] so that parallel API calls trigger only one
 * session request at a time. This prevents creating multiple anonymous Gini users when multiple
 * documents are uploaded in parallel on first use.
 *
 * Session failures are thrown as [ApiException] (session errors) or
 * [SessionCancellationException] (session cancellations) which
 * [Resource.Companion.wrapInResource] maps back to the same [Resource.Error] and
 * [Resource.Cancelled] shapes that the repositories returned when they handled tokens
 * themselves.
 *
 * The [SessionManager] is resolved lazily via [sessionManagerProvider] on the first request to
 * avoid initialization order issues between the HTTP client and the session manager (which
 * needs its own HTTP client for the User Center API).
 */
internal class GiniSessionInterceptor(
    private val sessionManagerProvider: () -> SessionManager
) : Interceptor {

    private val sessionMutex = Mutex()

    private val sessionManager: SessionManager by lazy { sessionManagerProvider() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(AUTHORIZATION_HEADER) != null) {
            return chain.proceed(request)
        }
        // Deliberately blocking: OkHttp interceptors are synchronous (cf. okhttp3.Authenticator).
        // If the caller cancels mid-fetch, this completes anyway - the result lands in the
        // session manager's cache, so the work is not wasted.
        val sessionResource = runBlocking {
            sessionMutex.withLock {
                sessionManager.getSession()
            }
        }
        return when (sessionResource) {
            is Resource.Success -> chain.proceed(
                request.newBuilder()
                    .header(
                        AUTHORIZATION_HEADER,
                        BearerAuthorizatonHeader(sessionResource.data.accessToken).value
                    )
                    .build()
            )
            is Resource.Error -> throw ApiException(
                message = sessionResource.message,
                responseStatusCode = sessionResource.responseStatusCode,
                responseBody = sessionResource.responseBody,
                responseHeaders = sessionResource.responseHeaders,
                cause = sessionResource.exception
            )
            is Resource.Cancelled -> throw SessionCancellationException()
        }
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
    }
}
