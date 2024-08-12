package net.gini.android.bank.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.internal.GiniCoreAPIBuilder

/**
 * The [GiniBankAPIBuilder] allows you to configure and create a [GiniBankAPI] instance.
 *
 * @constructor Initializes a new builder instance.
 * @param context your application's Context instance (Android)
 * @param clientId your application's client ID for the Gini Bank API
 * @param clientSecret your application's client secret for the Gini Bank API
 * @param emailDomain  the email domain which is used for created Gini users
 * @param sessionManager if not null, then the [SessionManager] instance will be used for session management. If null, then anonymous Gini users will be used.
 */
class GiniBankAPIBuilder @JvmOverloads constructor(
    context: Context,
    clientId: String = "",
    clientSecret: String = "",
    emailDomain: String = "",
    sessionManager: SessionManager? = null
) : GiniCoreAPIBuilder<BankApiDocumentManager, GiniBankAPI, BankApiDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {

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
        return BankApiDocumentRemoteSource(Dispatchers.IO, getApiRetrofit().create(BankApiDocumentService::class.java), bankApiType, getApiBaseUrl() ?: "")
    }

    private fun createTrackingAnalysisRemoteSource(): TrackingAnalysisRemoteSource {
        return TrackingAnalysisRemoteSource(Dispatchers.IO,getTrackingAnalyticsApiRetrofit().create(TrackingAnalysisService::class.java))
    }

    override fun createDocumentRepository(): BankApiDocumentRepository {
        return BankApiDocumentRepository(createDocumentRemoteSource(), getSessionManager(), bankApiType, createTrackingAnalysisRemoteSource())
    }
}
