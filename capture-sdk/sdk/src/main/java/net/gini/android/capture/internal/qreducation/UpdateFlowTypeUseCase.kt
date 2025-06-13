package net.gini.android.capture.internal.qreducation

import net.gini.android.capture.internal.qreducation.model.FlowType
import net.gini.android.capture.internal.storage.FlowTypeStorage

internal class UpdateFlowTypeUseCase(
    private val flowTypeStorage: FlowTypeStorage,
) {

    fun execute(flowType: FlowType?) {
        flowTypeStorage.set(flowType)
    }
}
