package net.gini.android.merchant.sdk.paymentComponent

import androidx.fragment.app.testing.launchFragmentInContainer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.merchant.sdk.paymentComponentBottomSheet.PaymentComponentBottomSheet
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentComponentBottomSheetTest {
    private lateinit var paymentComponent: PaymentComponent
    private lateinit var paymentComponentListener: PaymentComponent.Listener

    @Before
    fun setUp() {
        paymentComponent = mockk(relaxed = true)
        paymentComponentListener = mockk(relaxed = true)
        every { paymentComponent.listener } returns paymentComponentListener
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(
            mockk(relaxed = true)
        ))
    }

    @Test
    fun `calls onMoreInformation method of listener when clicking on info button`() = runTest {
        // Given
        val paymentComponentBottomSheet = PaymentComponentBottomSheet.newInstance(paymentComponent, false, mockk(relaxed = true))
        launchFragmentInContainer {
            paymentComponentBottomSheet
        }

        // When
        paymentComponentBottomSheet.paymentComponentView.getMoreInformationLabel().performClick()

        // Then
        verify { paymentComponentListener.onMoreInformationClicked() }
    }

    @Test
    fun `calls onBankPickerClicked method of listener when clicking on select bank button`() = runTest {
        // Given
        val paymentComponentBottomSheet = PaymentComponentBottomSheet.newInstance(paymentComponent, false, mockk(relaxed = true))

        launchFragmentInContainer {
            paymentComponentBottomSheet
        }

        // When
        paymentComponentBottomSheet.paymentComponentView.getBankPickerButton().performClick()

        // Then
        verify { paymentComponentListener.onBankPickerClicked() }
    }
}