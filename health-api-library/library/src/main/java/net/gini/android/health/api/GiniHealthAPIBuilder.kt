package net.gini.android.health.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.SessionManager
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
    context: Context,
    clientId: String = "",
    clientSecret: String = "",
    emailDomain: String = "",
    sessionManager: SessionManager? = null
) : GiniCoreAPIBuilder<HealthApiDocumentManager, GiniHealthAPI, HealthApiDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {

    private val healthApiType = GiniHealthApiType(3)

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
}