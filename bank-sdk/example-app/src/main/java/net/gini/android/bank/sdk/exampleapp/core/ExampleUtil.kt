package net.gini.android.bank.sdk.exampleapp.core

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.IntentCompat
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.core.api.models.SpecificExtraction

object ExampleUtil {
    fun isIntentActionViewOrSend(intent: Intent): Boolean {
        val action = intent.action
        return Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action
    }

    fun getOpenWithUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        Intent.ACTION_SEND -> listOfNotNull(
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        )
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.filterNotNull()
                .orEmpty()
        else -> emptyList()
    }

    fun hasNoPay5Extractions(extractionNames: Set<String>): Boolean {
        for (extractionName in extractionNames) {
            if (isPay5Extraction(extractionName)) {
                return false
            }
        }
        return true
    }

    fun isPay5Extraction(extractionName: String): Boolean {
        return extractionName == "amountToPay" || extractionName == "bic" || extractionName == "iban" || extractionName == "paymentReference" || extractionName == "paymentRecipient"
    }

    fun getExtractionsBundle(
        extractions: Map<String?, GiniCaptureSpecificExtraction?>?
    ): Bundle? {
        if (extractions == null) {
            return null
        }
        val extractionsBundle = Bundle()
        for ((key, value) in extractions) {
            extractionsBundle.putParcelable(key, value)
        }
        return extractionsBundle
    }

    fun getLegacyExtractionsBundle(
        extractions: Map<String?, SpecificExtraction?>?
    ): Bundle? {
        if (extractions == null) {
            return null
        }
        val extractionsBundle = Bundle()
        for ((key, value) in extractions) {
            extractionsBundle.putParcelable(key, value)
        }
        return extractionsBundle
    }
}