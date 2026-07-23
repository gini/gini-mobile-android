package net.gini.android.bank.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.http.GiniHttpClientProvider
import net.gini.android.core.api.internal.GiniCoreAPIBuilder

/**
 * The [GiniBankAPIBuilder] allows you to configure and create a [GiniBankAPI] instance.
 *
 * @constructor Initializes a new builder instance.
 * @param context your application's Context instance (Android)
 * @param clientId your application's client ID for the Gini Bank API
 * @param clientSecret your application's client secret for the Gini Bank API
 * @param emailDomain  the email domain which is used for created Gini users
 * @param sessionManager if not null, then the [SessionManager] instance will be used for session management.
 * If null, then anonymous Gini users will be used.
 */
class GiniBankAPIBuilder @JvmOverloads constructor(
    context: Context,
    clientId: String = "",
    clientSecret: String = "",
    emailDomain: String = "",
    sessionManager: SessionManager? = null
) : GiniCoreAPIBuilder<BankApiDocumentManager, GiniBankAPI, BankApiDocumentRepository,
        ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {

    private val bankApiType = GiniBankApiType(1)

    override fun getGiniApiType(): GiniApiType {
        return bankApiType
    }

    /**
     * Builds the GiniBankAPI instance with the configuration settings of the builder instance.
     *
     * @return The fully configured GiniBankAPI instance.
     */
    override fun build(): GiniBankAPI {
        return GiniBankAPI(createDocumentManager(), getCredentialsStore())
    }

    override fun createDocumentManager(): BankApiDocumentManager {
        return BankApiDocumentManager(getDocumentRepository())
    }

    private fun createDocumentRemoteSource(): BankApiDocumentRemoteSource {
        return BankApiDocumentRemoteSource(
            Dispatchers.IO,
            getApiRetrofit().create(BankApiDocumentService::class.java),
            bankApiType,
            getApiBaseUrl() ?: ""
        )
    }

    private fun createTrackingAnalysisRemoteSource(): TrackingAnalysisRemoteSource {
        return TrackingAnalysisRemoteSource(
            Dispatchers.IO,
            getTrackingAnalyticsApiRetrofit().create(TrackingAnalysisService::class.java)
        )
    }

    override fun createDocumentRepository(): BankApiDocumentRepository {
        return BankApiDocumentRepository(
            createDocumentRemoteSource(),
            getSessionManager(),
            bankApiType,
            createTrackingAnalysisRemoteSource()
        )
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
    override fun setHttpClientProvider(provider: GiniHttpClientProvider): GiniBankAPIBuilder {
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
    override fun setSelfManagedAuthentication(enabled: Boolean): GiniBankAPIBuilder {
        super.setSelfManagedAuthentication(enabled)
        return this
    }
}
