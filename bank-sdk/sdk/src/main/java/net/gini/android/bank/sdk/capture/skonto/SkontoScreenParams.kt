package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoDiscountPercentageFormatter
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.Amount
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Groups display-related configuration parameters for Skonto screen functions
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class SkontoDisplayConfig(
    val isLandScape: Boolean,
    val isBottomNavigationBarEnabled: Boolean,
    val composableProviderConfig: GiniComposableStyleProviderConfig?,
    val shouldFieldShowKeyboard: Boolean = false,
    val isTablet: Boolean = false,
    val discountPercentageFormatter: SkontoDiscountPercentageFormatter = SkontoDiscountPercentageFormatter(),
)

/**
 * Groups the three navigation lambdas passed from the Fragment into SkontoScreenContent
 * to reduce the parameter count.
 */
internal data class SkontoNavigationHandlers(
    val navigateBack: () -> Unit,
    val navigateToHelp: () -> Unit,
    val navigateToInvoiceScreen: (documentId: String, infoTextLines: List<String>) -> Unit,
)

/**
 * Groups all ViewModel event callbacks for the Skonto screen
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class SkontoScreenCallbacks(
    val onBackClicked: () -> Unit,
    val onHelpClicked: () -> Unit,
    val onProceedClicked: () -> Unit,
    val onInfoBannerClicked: () -> Unit,
    val onInfoDialogDismissed: () -> Unit,
    val onInvoiceClicked: () -> Unit,
    val onDiscountSectionActiveChange: (Boolean) -> Unit,
    val onSkontoAmountChange: (BigDecimal) -> Unit,
    val onDueDateChanged: (LocalDate) -> Unit,
    val onFullAmountChange: (BigDecimal) -> Unit,
    val onSkontoAmountFieldFocused: () -> Unit,
    val onDueDateFieldFocused: () -> Unit,
    val onFullAmountFieldFocused: () -> Unit,
    val onConfirmAttachTransactionDocClicked: (alwaysAttach: Boolean) -> Unit,
    val onCancelAttachTransactionDocClicked: () -> Unit,
)

/**
 * Groups display state for the SkontoSection composable
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class SkontoSectionDisplayState(
    val isActive: Boolean,
    val amount: Amount,
    val dueDate: LocalDate,
    val infoPaymentInDays: Int,
    val infoDiscountValue: BigDecimal,
    val edgeCase: SkontoEdgeCase?,
    val skontoAmountValidationError: SkontoScreenState.Ready.SkontoAmountValidationError?,
    val hideFieldsForTalkBack: Boolean,
    val setHideFieldsForTalkBack: (Boolean) -> Unit,
)

/**
 * Groups callbacks for the SkontoSection composable
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class SkontoSectionCallbacks(
    val onActiveChange: (Boolean) -> Unit,
    val onSkontoAmountChange: (BigDecimal) -> Unit,
    val onDueDateChanged: (LocalDate) -> Unit,
    val onInfoBannerClicked: () -> Unit,
    val onSkontoAmountFieldFocused: () -> Unit,
    val onDueDateFieldFocused: () -> Unit,
)

/**
 * Groups display state for the WithoutSkontoSection composable
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class WithoutSkontoSectionState(
    val isActive: Boolean,
    val amount: Amount,
    val fullAmountValidationError: SkontoScreenState.Ready.FullAmountValidationError?,
    val hideFieldsForTalkBack: Boolean,
)

/**
 * Groups input config and callbacks for SkontoAmountInputSection
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class SkontoAmountInputConfig(
    val onSkontoAmountChange: (BigDecimal) -> Unit,
    val onSkontoAmountFieldFocused: () -> Unit,
    val shouldFieldShowKeyboard: Boolean,
    val hideFieldsForTalkBack: Boolean,
    val isLandScape: Boolean,
)

/**
 * Groups the raw data values needed to render the footer section
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class SkontoFooterDisplayState(
    val totalAmount: Amount,
    val savedAmount: Amount,
    val discountValue: BigDecimal,
    val isSkontoSectionActive: Boolean,
)

/**
 * Groups the already-computed text strings for the footer display
 * to satisfy kotlin:S107 (too many parameters).
 */
internal data class SkontoFooterTexts(
    val discountLabelText: String,
    val totalPriceText: String,
    val savedAmountText: String,
)

