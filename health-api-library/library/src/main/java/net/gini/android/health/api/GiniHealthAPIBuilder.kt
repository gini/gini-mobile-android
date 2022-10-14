package net.gini.android.health.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.internal.KGiniCoreAPIBuilder
import net.gini.android.core.api.models.ExtractionsContainer

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

class GiniHealthAPIBuilder @JvmOverloads constructor(
    context: Context,
    clientId: String = "",
    clientSecret: String = "",
    emailDomain: String = "",
    sessionManager: KSessionManager? = null
) : KGiniCoreAPIBuilder<HealthApiDocumentManager, GiniHealthAPI, HealthApiDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {

    private val healthApiType = GiniHealthApiType(3)

    override fun getGiniApiType(): GiniApiType {
        return healthApiType
    }

    /**
     * Builds the GiniBankAPI instance with the configuration settings of the builder instance.
     *
     * @return The fully configured GiniBankAPI instance.
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