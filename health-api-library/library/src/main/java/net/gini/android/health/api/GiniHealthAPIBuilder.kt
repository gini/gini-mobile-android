package net.gini.android.health.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.http.GiniHttpClientProvider
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.models.ExtractionsContainer

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * The [GiniHealthAPIBuilder] allows you to configure and create a [GiniHealthAPI] instance.
 *
 * @constructor Initializes a new builder instance.
 * @param context your application's Context instance (Android)
 * @param clientId your application's client ID for the Gini Health API
 * @param clientSecret your application's client secret for the Gini Health API
 * @param emailDomain  the email domain which is used for created Gini users
 * @param sessionManager if not null, then the [SessionManager] instance will be used for session management. If null, then anonymous Gini users will be used.
 */
class GiniHealthAPIBuilder @JvmOverloads constructor(
    // Not stored: the base builder keeps only the application context, so an Activity context
    // can't be leaked through the session interceptor's builder reference
    context: Context,
    clientId: String = "",
    clientSecret: String = "",
    emailDomain: String = "",
    sessionManager: SessionManager? = null,
    apiVersion: Int = API_VERSION
) : GiniCoreAPIBuilder<HealthApiDocumentManager, GiniHealthAPI, HealthApiDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {

    private val healthApiType = GiniHealthApiType(apiVersion)

    override fun getGiniApiType(): GiniApiType {
        return healthApiType
    }

    /**
     * Builds the GiniHealthAPI instance with the configuration settings of the builder instance.
     *
     * @return The fully configured GiniHealthAPI instance.
     */
    override fun build(): GiniHealthAPI {
        return GiniHealthAPI(createDocumentManager(), getCredentialsStore())
    }

    override fun createDocumentManager(): HealthApiDocumentManager {
        return HealthApiDocumentManager(getDocumentRepository())
    }

    private fun createDocumentRemoteSource(): HealthApiDocumentRemoteSource {
        return HealthApiDocumentRemoteSource(Dispatchers.IO, getApiRetrofit().create(HealthApiDocumentService::class.java), healthApiType, getApiBaseUrl() ?: "")
    }

    override fun createDocumentRepository(): HealthApiDocumentRepository {
        return HealthApiDocumentRepository(createDocumentRemoteSource(), getSessionManager(), healthApiType)
    }

    /**
     * Set a custom [GiniHttpClientProvider] to provide a configured OkHttpClient.
     *
     * This allows full control over HTTP client configuration including TLS/SSL settings,
     * proxies, custom interceptors, logging, and more.
     *
     * @param provider A [GiniHttpClientProvider] implementation
     * @return The builder instance to enable chaining
     * @see net.gini.android.core.api.http.DefaultGiniHttpClientProvider
     */
    override fun setHttpClientProvider(provider: GiniHttpClientProvider): GiniHealthAPIBuilder {
        super.setHttpClientProvider(provider)
        return this
    }

    /**
     * Enable self-managed authentication: the SDK will not authenticate API requests and no
     * [SessionManager] (or client credentials) are required.
     *
     * When enabled, your [GiniHttpClientProvider]'s OkHttpClient is responsible for adding the
     * `Authorization` header to API requests (for example with your own application or network
     * interceptor - either works, because the SDK installs no authentication of its own in this
     * mode). Your access token is never passed through the SDK.
     *
     * A custom [GiniHttpClientProvider] must be set via [setHttpClientProvider], otherwise
     * building will throw an [IllegalStateException]. Disabled by default.
     *
     * @param enabled pass `true` to authenticate API requests yourself
     * @return The builder instance to enable chaining
     */
    override fun setSelfManagedAuthentication(enabled: Boolean): GiniHealthAPIBuilder {
        super.setSelfManagedAuthentication(enabled)
        return this
    }

    companion object {
        const val API_VERSION = 5
    }
}
