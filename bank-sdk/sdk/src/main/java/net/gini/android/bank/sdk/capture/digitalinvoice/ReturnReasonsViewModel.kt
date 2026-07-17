package net.gini.android.bank.sdk.capture.digitalinvoice

import androidx.lifecycle.ViewModel
import net.gini.android.capture.network.model.GiniCaptureReturnReason

internal class ReturnReasonsViewModel(
    private val reasons: List<GiniCaptureReturnReason>,
) : ViewModel() {

    val localizedReasons: List<String>
        get() = reasons.map { it.labelInLocalLanguageOrGerman ?: "" }

    fun reasonAt(position: Int): GiniCaptureReturnReason = reasons[position]
}
