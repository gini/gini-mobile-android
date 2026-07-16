package net.gini.android.core.api.mapper

import net.gini.android.core.api.models.Box
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Extraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses the Gini API extractions response JSON into the SDK's extraction models.
 *
 * Use this when you do the API calls yourself (with your own network implementation) and want
 * to map the extractions response body to the same models the SDK produces.
 */
object ExtractionsParser {

    /**
     * Parses a complete extractions response (the response body of
     * `GET /documents/{id}/extractions`) into an [ExtractionsContainer].
     *
     * @param extractionsResponse the extractions response JSON
     * @return The parsed [ExtractionsContainer] with specific and compound extractions.
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun parseExtractionsContainer(extractionsResponse: JSONObject): ExtractionsContainer {
        val candidates = parseCandidates(extractionsResponse.getJSONObject("candidates"))
        val specificExtractions =
            parseSpecificExtractions(extractionsResponse.getJSONObject("extractions"), candidates)
        val compoundExtractions =
            parseCompoundExtractions(extractionsResponse.optJSONObject("compoundExtractions"), candidates)
        return ExtractionsContainer(specificExtractions, compoundExtractions)
    }

    /**
     * Parses the `extractions` object of the extractions response JSON.
     *
     * @param specificExtractionsJson the value of the `extractions` key
     * @param candidates the parsed candidates (see [parseCandidates])
     * @param parseExtraction can be overridden to customize how a single extraction is parsed
     * @return Mapping from extraction names to [SpecificExtraction]s.
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(JSONException::class)
    fun parseSpecificExtractions(
        specificExtractionsJson: JSONObject,
        candidates: Map<String, List<Extraction>>,
        parseExtraction: (JSONObject) -> Extraction = ::parseExtraction
    ): Map<String, SpecificExtraction> {
        val specificExtractions = mutableMapOf<String, SpecificExtraction>()
        val extractionsNameIterator = specificExtractionsJson.keys()
        while (extractionsNameIterator.hasNext()) {
            val extractionName = extractionsNameIterator.next()
            val extractionData = specificExtractionsJson.getJSONObject(extractionName)
            val extraction: Extraction = parseExtraction(extractionData)
            var candidatesForExtraction = listOf<Extraction?>()
            if (extractionData.has("candidates")) {
                val candidatesName = extractionData.getString("candidates")
                if (candidates.containsKey(candidatesName)) {
                    candidatesForExtraction = candidates[candidatesName] ?: emptyList()
                }
            }
            val specificExtraction = SpecificExtraction(
                extractionName, extraction.value,
                extraction.entity, extraction.box,
                candidatesForExtraction
            )
            specificExtractions[extractionName] = specificExtraction
        }

        return specificExtractions
    }

    /**
     * Parses the `compoundExtractions` object of the extractions response JSON.
     *
     * @param compoundExtractionsJson the value of the `compoundExtractions` key (may be null)
     * @param candidates the parsed candidates (see [parseCandidates])
     * @param parseExtraction can be overridden to customize how a single extraction is parsed
     * @return Mapping from extraction names to [CompoundExtraction]s.
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(JSONException::class)
    fun parseCompoundExtractions(
        compoundExtractionsJson: JSONObject?,
        candidates: Map<String, List<Extraction>>,
        parseExtraction: (JSONObject) -> Extraction = ::parseExtraction
    ): Map<String, CompoundExtraction> {
        if (compoundExtractionsJson == null) {
            return emptyMap()
        }
        val compoundExtractions = HashMap<String, CompoundExtraction>()
        val extractionsNameIterator = compoundExtractionsJson.keys()
        while (extractionsNameIterator.hasNext()) {
            val extractionName = extractionsNameIterator.next()
            val specificExtractionMaps: MutableList<Map<String, SpecificExtraction>> = ArrayList()
            val compoundExtractionData = compoundExtractionsJson.getJSONArray(extractionName)
            for (i in 0 until compoundExtractionData.length()) {
                val specificExtractionsData = compoundExtractionData.getJSONObject(i)
                specificExtractionMaps.add(
                    parseSpecificExtractions(specificExtractionsData, candidates, parseExtraction)
                )
            }
            compoundExtractions[extractionName] = CompoundExtraction(extractionName, specificExtractionMaps)
        }
        return compoundExtractions
    }

    /**
     * Parses the `candidates` object of the extractions response JSON into a mapping where the
     * key is the name of the candidates list (e.g. "amounts" or "dates") and the value is a
     * list of extraction instances.
     *
     * @param candidatesJson the value of the `candidates` key
     * @param parseExtraction can be overridden to customize how a single extraction is parsed
     * @return The created mapping as described above.
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(JSONException::class)
    fun parseCandidates(
        candidatesJson: JSONObject,
        parseExtraction: (JSONObject) -> Extraction = ::parseExtraction
    ): Map<String, List<Extraction>> {
        val candidatesByEntity = HashMap<String, List<Extraction>>()
        val entityNameIterator = candidatesJson.keys()
        while (entityNameIterator.hasNext()) {
            val entityName = entityNameIterator.next()
            val candidatesListData = candidatesJson.getJSONArray(entityName)
            val candidates = ArrayList<Extraction>()
            var i = 0
            val length = candidatesListData.length()
            while (i < length) {
                val extractionData = candidatesListData.getJSONObject(i)
                candidates.add(parseExtraction(extractionData))
                i += 1
            }
            candidatesByEntity[entityName] = candidates
        }
        return candidatesByEntity
    }

    /**
     * Parses a single extraction JSON object into an [Extraction] instance.
     *
     * @param extractionJson The JSON data of a single extraction.
     * @return The created [Extraction] instance.
     * @throws JSONException If the JSON does not have the expected structure or contains invalid data.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun parseExtraction(extractionJson: JSONObject): Extraction {
        val entity = extractionJson.getString("entity")
        val value = extractionJson.getString("value")
        // The box is optional for some extractions.
        var box: Box? = null
        if (extractionJson.has("box")) {
            box = Box.fromApiResponse(extractionJson.getJSONObject("box"))
        }
        return Extraction(value, entity, box)
    }
}
