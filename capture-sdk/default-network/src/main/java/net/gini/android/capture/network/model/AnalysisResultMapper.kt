package net.gini.android.capture.network.model

import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.capture.network.AnalysisResult
import net.gini.android.core.api.models.Document

fun toAnalysisResult(
    compositeDocument: Document,
    allExtractions: ExtractionsContainer
): AnalysisResult {

    val extractions =
        SpecificExtractionMapper.mapToGiniCapture(allExtractions.specificExtractions)
    val compoundExtractions =
        CompoundExtractionsMapper.mapToGiniCapture(allExtractions.compoundExtractions)
    val returnReasons =
        ReturnReasonsMapper.mapToGiniCapture(allExtractions.returnReasons)

    return AnalysisResult(
        compositeDocument.id,
        compositeDocument.filename,
        extractions,
        compoundExtractions,
        returnReasons

    )
}