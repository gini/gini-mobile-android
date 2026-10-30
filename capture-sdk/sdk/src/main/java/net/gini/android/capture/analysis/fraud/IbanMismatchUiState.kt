package net.gini.android.capture.analysis.fraud

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * UI state of the mismatch modal and the selection sheet stacked on it. One holder for both
 * layers so they are restored together after rotation and process death.
 */
@Parcelize
internal data class IbanMismatchUiState(
    val qrIban: String,
    val printedIban: String,
    val selectedIban: String,
    val isSheetVisible: Boolean,
) : Parcelable {

    companion object {
        fun initial(args: IbanMismatchArgs) = IbanMismatchUiState(
            qrIban = args.qrIban,
            printedIban = args.printedIban,
            selectedIban = args.qrIban,
            isSheetVisible = false,
        )
    }
}
