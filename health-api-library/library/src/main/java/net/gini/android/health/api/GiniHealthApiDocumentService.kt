package net.gini.android.health.api

import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.Resource

interface GiniHealthApiDocumentService: DocumentService {

    @POST("documents/{documentId}/extractions/feedback")
    suspend fun sendFeedback(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Resource<ResponseBody>
}