package net.gini.android.bank.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.internal.KGiniCoreAPIBuilder

class GiniBankAPIBuilder @JvmOverloads constructor(
    context: Context,
    clientId: String = "",
    clientSecret: String = "",
    emailDomain: String = "",
    sessionManager: KSessionManager? = null
) : KGiniCoreAPIBuilder<BankApiDocumentManager, GiniBankAPI, BankApiDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {

    private val bankApiType: GiniBankApiType = GiniBankApiType(1)

    override fun getGiniApiType(): GiniBankApiType {
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
        return BankApiDocumentRemoteSource(Dispatchers.IO, getApiRetrofit().create(BankApiDocumentService::class.java), getGiniApiType(), getSessionManager(), getApiBaseUrl() ?: "")
    }

    override fun createDocumentRepository(): BankApiDocumentRepository {
        return BankApiDocumentRepository(Dispatchers.IO, createDocumentRemoteSource(), getGiniApiType(), getMoshi())
    }
}
