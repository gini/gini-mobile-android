package net.gini.android.core.api.test

import net.gini.android.core.api.DocumentService
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * [DocumentService] for tests which adds the HTTP annotations for [sendFeedback]: the core
 * [DocumentService] leaves it unannotated because each API type overrides it with its own
 * route (cf. the bank and health api library document services).
 */
internal interface TestDocumentService : DocumentService {

    @POST("documents/{documentId}/extractions")
    override suspend fun sendFeedback(
        @HeaderMap bearer: Map<String, String>,
        @Path("documentId") id: String,
        @Body params: RequestBody
    ): Response<ResponseBody>
}
