package net.gini.android.bank.api

import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.GiniApiType
import net.gini.android.core.api.authorization.apimodels.SessionToken
import kotlin.coroutines.CoroutineContext

class BankApiDocumentRemoteSource(
    override var coroutineContext: CoroutineContext,
    private val documentService: DocumentService,
    private val giniApiType: GiniApiType,
    private val sessionToken: SessionToken,
    baseUriString: String
): DocumentRemoteSource(coroutineContext, documentService, giniApiType, sessionToken, baseUriString) {

}