package net.gini.android.capture.internal.storage

import net.gini.android.capture.internal.qreducation.model.FlowType

internal class FlowTypeStorage {

    private var flowType: FlowType? = null

    fun set(flowType: FlowType?) {
        this.flowType = flowType
    }

    fun get() = flowType

}
