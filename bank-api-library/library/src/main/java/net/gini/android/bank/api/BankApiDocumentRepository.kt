package net.gini.android.bank.api

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

class BankApiDocumentRepository(
    override val coroutineContext: CoroutineContext,
    private val documentRemoteSource: DocumentRemoteSource,
    private val giniApiType: GiniApiType
) : DocumentRepository<ExtractionsContainer>(coroutineContext, documentRemoteSource, giniApiType) {

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer {
        TODO("Not yet implemented")
    }

}