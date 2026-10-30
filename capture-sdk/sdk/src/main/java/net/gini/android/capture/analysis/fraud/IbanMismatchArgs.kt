package net.gini.android.capture.analysis.fraud

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Input of the IBAN mismatch modal: the two IBANs that disagree. */
@Parcelize
internal data class IbanMismatchArgs(
    val qrIban: String,
    val printedIban: String,
) : Parcelable
