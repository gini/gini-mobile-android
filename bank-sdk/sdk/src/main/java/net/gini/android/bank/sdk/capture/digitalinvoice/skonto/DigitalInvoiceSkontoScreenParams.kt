package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoResultArgs
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.Amount
import java.math.BigDecimal
import java.time.LocalDate

/** Groups display-related configuration for DigitalInvoiceSkontoFragment functions. */
internal data class DigitalInvoiceSkontoDisplayConfig(
    val isLandScape: Boolean,
    val isBottomNavigationBarEnabled: Boolean,
    val shouldFieldShowKeyboard: Boolean = false,
)

/** Groups navigation callbacks for the DigitalInvoiceSkontoFragment. */
internal data class DigitalInvoiceSkontoNavigationCallbacks(
    val navigateBack: (DigitalInvoiceSkontoResultArgs) -> Unit,
    val navigateToHelpScreen: () -> Unit,
    val navigateToInvoiceScreen: (documentId: String, infoTextLines: List<String>) -> Unit,
)

/** Groups all ViewModel event callbacks for the DigitalInvoiceSkonto screen. */
internal data class DigitalInvoiceSkontoCallbacks(
    val onSkontoAmountChange: (BigDecimal) -> Unit,
    val onDueDateChanged: (LocalDate) -> Unit,
    val onBackClicked: () -> Unit,
    val onInfoBannerClicked: () -> Unit,
    val onInfoDialogDismissed: () -> Unit,
    val onInvoiceClicked: () -> Unit,
    val onHelpClicked: () -> Unit,
    val onSkontoAmountFieldFocused: () -> Unit,
    val onDueDateFieldFocused: () -> Unit,
)

/** Groups display state for the Skonto section in the digital invoice flow. */
internal data class DigitalInvoiceSkontoSectionState(
    val isActive: Boolean,
    val amount: Amount,
    val dueDate: LocalDate,
    val infoPaymentInDays: Int,
    val infoDiscountValue: BigDecimal,
    val edgeCase: SkontoEdgeCase?,
    val skontoAmountValidationError: SkontoScreenState.Ready.SkontoAmountValidationError?,
)

/** Groups callbacks for the Skonto section in the digital invoice flow. */
internal data class DigitalInvoiceSkontoSectionCallbacks(
    val onSkontoAmountChange: (BigDecimal) -> Unit,
    val onDueDateChanged: (LocalDate) -> Unit,
    val onInfoBannerClicked: () -> Unit,
    val onSkontoAmountFieldFocused: () -> Unit,
    val onDueDateFieldFocused: () -> Unit,
)

