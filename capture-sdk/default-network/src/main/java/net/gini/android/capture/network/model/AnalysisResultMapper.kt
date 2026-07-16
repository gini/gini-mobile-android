package net.gini.android.capture.network.model

import net.gini.android.bank.api.mapper.BankExtractionsParser
import net.gini.android.capture.network.AnalysisResult
import org.json.JSONException
import org.json.JSONObject

/**
 * Maps the Gini Bank API extractions response JSON to an [AnalysisResult].
 *
 * Use this when you implement the
 * [GiniCaptureNetworkService][net.gini.android.capture.network.GiniCaptureNetworkService]
 * yourself (with your own network implementation) and need to map the extractions response
 * body of `GET /documents/{id}/extractions` to the [AnalysisResult] expected by the Gini
 * Capture SDK.
 */
object AnalysisResultMapper {

    /**
     * Creates an [AnalysisResult] from the extractions response body of a Gini Bank API
     * document.
     *
     * @param documentId the id of the (composite) document the extractions belong to
     * @param documentFilename the filename of the document
     * @param extractionsResponseJson the raw response body of `GET /documents/{id}/extractions`
     * @return The [AnalysisResult] with the mapped extractions, compound extractions and
     * return reasons.
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun fromExtractionsJson(
        documentId: String,
        documentFilename: String,
        extractionsResponseJson: String
    ): AnalysisResult {
        val extractionsContainer =
            BankExtractionsParser.parseExtractionsContainer(JSONObject(extractionsResponseJson))
        return AnalysisResult(
            documentId,
            documentFilename,
            SpecificExtractionMapper.mapToGiniCapture(extractionsContainer.specificExtractions),
            CompoundExtractionsMapper.mapToGiniCapture(extractionsContainer.compoundExtractions),
            ReturnReasonsMapper.mapToGiniCapture(extractionsContainer.returnReasons)
        )
    }
}
