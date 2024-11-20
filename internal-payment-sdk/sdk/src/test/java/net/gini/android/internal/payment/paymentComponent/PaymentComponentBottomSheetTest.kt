package net.gini.android.internal.payment.paymentComponent

import android.widget.Button
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.paymentComponentBottomSheet.PaymentComponentBottomSheet
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
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(
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

    @Test
    fun `disables buttons and deletes document id to reuse`() = runTest {
        // Given
        val paymentComponentBottomSheet = PaymentComponentBottomSheet.newInstance(paymentComponent, false, mockk(relaxed = true))

        launchFragmentInContainer {
            paymentComponentBottomSheet
        }
        paymentComponentBottomSheet.paymentComponentView.paymentComponent = paymentComponent
        paymentComponentBottomSheet.paymentComponentView.documentId = "123"

        Truth.assertThat(paymentComponentBottomSheet.paymentComponentView.documentId).isEqualTo("123")
        Truth.assertThat((paymentComponentBottomSheet.paymentComponentView.findViewById(R.id.gps_pay_invoice_button) as Button).isEnabled).isEqualTo(true)
        Truth.assertThat((paymentComponentBottomSheet.paymentComponentView.findViewById(R.id.gps_select_bank_button) as Button).isEnabled).isEqualTo(true)

        // When
        paymentComponentBottomSheet.paymentComponentView.prepareForReuse()

        // Then
        Truth.assertThat(paymentComponentBottomSheet.paymentComponentView.documentId).isNull()
        Truth.assertThat((paymentComponentBottomSheet.paymentComponentView.findViewById(R.id.gps_pay_invoice_button) as Button).isEnabled).isEqualTo(false)
        Truth.assertThat((paymentComponentBottomSheet.paymentComponentView.findViewById(R.id.gps_select_bank_button) as Button).isEnabled).isEqualTo(false)
    }
}