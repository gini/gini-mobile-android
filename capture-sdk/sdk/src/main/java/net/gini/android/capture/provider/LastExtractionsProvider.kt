package net.gini.android.capture.provider

import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

class LastExtractionsProvider {

    private var extractions = mutableMapOf<String, GiniCaptureSpecificExtraction>()

    fun update(extractions: MutableMap<String, GiniCaptureSpecificExtraction>) {
        this.extractions = extractions.toMutableMap()
    }

    fun provide() : MutableMap<String, GiniCaptureSpecificExtraction> = this.extractions
}
