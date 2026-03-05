package net.gini.android.health.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.http.GiniHttpClientProvider
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.health.api.http.HealthApiAcceptHeaderInterceptor
import okhttp3.OkHttpClient

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
    private val context: Context,
    clientId: String = "",
    clientSecret: String = "",
    emailDomain: String = "",
    sessionManager: SessionManager? = null,
    apiVersion: Int = API_VERSION
) : GiniCoreAPIBuilder<HealthApiDocumentManager, GiniHealthAPI, HealthApiDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {

    private val healthApiType = GiniHealthApiType(apiVersion)
    private var customHttpClientProvider: GiniHttpClientProvider? = null

    init {
        // Set a wrapper that will add Health API interceptor to whatever client the parent creates
        super.setHttpClientProvider(HealthApiHttpClientProviderWrapper(healthApiType))
    }

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
        return HealthApiDocumentRepository(createDocumentRemoteSource(), healthApiType)
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
        // Store the custom provider
        customHttpClientProvider = provider
        // Wrap it to add Health API interceptor
        super.setHttpClientProvider(HealthApiHttpClientProviderWrapper(healthApiType, provider))
        return this
    }

    /**
     * Internal wrapper that adds Health API-specific Accept header interceptor.
     * 
     * This wrapper:
     * 1. Gets the base HTTP client (either from custom provider or lets parent create default)
     * 2. Adds the Health API Accept header interceptor on top
     * 3. Returns the enhanced client
     * 
     * This ensures all parent configurations (TrustManager, cache, timeouts, auth) are preserved.
     */
    private inner class HealthApiHttpClientProviderWrapper(
        private val giniApiType: GiniApiType,
        private val baseProvider: GiniHttpClientProvider? = null
    ) : GiniHttpClientProvider {
        
        override fun provideOkHttpClient(): OkHttpClient {
            // Get the base client - either from custom provider or create empty base
            val baseClient = baseProvider?.provideOkHttpClient() 
                ?: OkHttpClient()
            
            // Add Health API-specific Accept header interceptor
            // Important: Add as interceptor (not network interceptor) to run BEFORE parent's SDK interceptors
            return baseClient.newBuilder()
                .addInterceptor(HealthApiAcceptHeaderInterceptor(giniApiType))
                .build()
        }
    }

    companion object {
        const val API_VERSION = 4
    }
}
