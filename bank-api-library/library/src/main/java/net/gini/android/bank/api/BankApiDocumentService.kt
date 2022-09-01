package net.gini.android.bank.api

import net.gini.android.core.api.DocumentService
import net.gini.android.core.api.Resource
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface BankApiDocumentService: DocumentService {

    @POST("documents/{documentId}/extractions/feedback")
    suspend fun sendFeedback(@HeaderMap bearer: Map<String, String>, @Path("documentId") documentId: String): Resource<ResponseBody>

}