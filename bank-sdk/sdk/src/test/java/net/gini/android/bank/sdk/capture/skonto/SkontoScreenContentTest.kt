package net.gini.android.bank.sdk.capture.skonto

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.util.currencyFormatterWithoutSymbol
import net.gini.android.capture.Amount
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.theme.GiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Renders the Skonto screen's ready state under Robolectric and checks the semantics the
 * BrowserStack UI automation relies on: every [SkontoTestTags] tag has to be present in both
 * orientations, and the tagged controls have to forward their interactions.
 *
 * The footer is the reason both orientations are covered — it renders inside the scrolling
 * column in landscape and in the `Scaffold`'s bottom bar in portrait, each with its own composable.
 */
@RunWith(AndroidJUnit4::class)
class SkontoScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `portrait tags the discount section, both inputs and the bottom bar footer`() {
        setReadyState(isLandScape = false)

        assertAllTagsPresent()
    }

    @Test
    fun `landscape tags the discount section, both inputs and the in-column footer`() {
        setReadyState(isLandScape = true)

        assertAllTagsPresent()
    }

    @Test
    fun `the discount switch reflects the section state and forwards a toggle`() {
        var toggledTo: Boolean? = null
        setReadyState(
            isLandScape = false,
            isSkontoSectionActive = true,
            callbacks = noOpCallbacks().copy(onDiscountSectionActiveChange = { toggledTo = it }),
        )

        composeRule.onNodeWithTag(SkontoTestTags.DISCOUNT_SWITCH, useUnmergedTree = true)
            .assertIsOn()
            .performClick()

        assertThat(toggledTo).isFalse()
    }

    @Test
    fun `an inactive discount section shows the switch off`() {
        setReadyState(isLandScape = false, isSkontoSectionActive = false)

        composeRule.onNodeWithTag(SkontoTestTags.DISCOUNT_SWITCH, useUnmergedTree = true)
            .assertIsOff()
    }

    @Test
    fun `the proceed button forwards the click in portrait`() {
        var proceedClicks = 0
        setReadyState(
            isLandScape = false,
            callbacks = noOpCallbacks().copy(onProceedClicked = { proceedClicks++ }),
        )

        composeRule.onNodeWithTag(SkontoTestTags.PROCEED_BUTTON, useUnmergedTree = true)
            .performClick()

        assertThat(proceedClicks).isEqualTo(1)
    }

    @Test
    fun `the proceed button forwards the click in landscape`() {
        var proceedClicks = 0
        setReadyState(
            isLandScape = true,
            callbacks = noOpCallbacks().copy(onProceedClicked = { proceedClicks++ }),
        )

        // In landscape the footer is part of the scrolling column, not the bottom bar
        composeRule.onNodeWithTag(SkontoTestTags.PROCEED_BUTTON, useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        assertThat(proceedClicks).isEqualTo(1)
    }

    private fun assertAllTagsPresent() {
        listOf(
            SkontoTestTags.DISCOUNT_TITLE,
            SkontoTestTags.DISCOUNT_SWITCH,
            SkontoTestTags.FINAL_AMOUNT_FIELD,
            SkontoTestTags.EXPIRY_DATE_FIELD,
            SkontoTestTags.FOOTER_TOTAL,
            SkontoTestTags.PROCEED_BUTTON,
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists(
                "Expected a node tagged '$tag'"
            )
        }
    }

    private fun setReadyState(
        isLandScape: Boolean,
        isSkontoSectionActive: Boolean = true,
        callbacks: SkontoScreenCallbacks = noOpCallbacks(),
    ) {
        composeRule.setContent {
            GiniTheme {
                ScreenReadyState(
                    state = readyState(isSkontoSectionActive),
                    amountFormatter = AmountFormatter(currencyFormatterWithoutSymbol()),
                    callbacks = callbacks,
                    displayConfig = SkontoDisplayConfig(
                        isLandScape = isLandScape,
                        composableProviderConfig = GiniComposableStyleProviderConfig(),
                    ),
                )
            }
        }
    }

    private fun readyState(isSkontoSectionActive: Boolean) = SkontoScreenState.Ready(
        isSkontoSectionActive = isSkontoSectionActive,
        paymentInDays = 14,
        skontoPercentage = BigDecimal("3"),
        skontoAmount = Amount.parse("97:EUR"),
        discountDueDate = LocalDate.of(2026, 10, 15),
        fullAmount = Amount.parse("100:EUR"),
        totalAmount = Amount.parse("97:EUR"),
        paymentMethod = SkontoData.SkontoPaymentMethod.PayPal,
        edgeCase = null,
        edgeCaseInfoDialogVisible = false,
        savedAmount = Amount.parse("3:EUR"),
        transactionDialogVisible = false,
        skontoAmountValidationError = null,
        fullAmountValidationError = null,
    )

    private fun noOpCallbacks() = SkontoScreenCallbacks(
        onBackClicked = {},
        onHelpClicked = {},
        onProceedClicked = {},
        onInfoBannerClicked = {},
        onInfoDialogDismissed = {},
        onInvoiceClicked = {},
        onDiscountSectionActiveChange = {},
        onSkontoAmountChange = {},
        onDueDateChanged = {},
        onFullAmountChange = {},
        onSkontoAmountFieldFocused = {},
        onDueDateFieldFocused = {},
        onFullAmountFieldFocused = {},
        onConfirmAttachTransactionDocClicked = {},
        onCancelAttachTransactionDocClicked = {},
    )
}
