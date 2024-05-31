package net.gini.android.merchant.sdk.paymentComponent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentComponentViewTest {
    private var context: Context? = null
    private var scenario: ActivityScenario<Activity>? = null
    private lateinit var paymentComponent: PaymentComponent
    private lateinit var paymentComponentListener: PaymentComponent.Listener

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context!!.setTheme(R.style.GiniHealthTheme)
        paymentComponent = PaymentComponent(context!!, mockk())
        paymentComponentListener = mockk(relaxed = true)
        paymentComponent.listener = paymentComponentListener

        // Needed to build the activity in which the custom component will be launched to be tested
        Robolectric.buildActivity(Activity::class.java).create().get()
        scenario = ActivityScenario.launch(
            Intent(context, Activity::class.java)
        )
        scenario?.moveToState(Lifecycle.State.CREATED)
    }

    @After
    fun tearDown() {
        context = null
        scenario!!.close()
    }

    @Test
    fun `calls onMoreInformation method of listener when clicking on info button`() = runTest {
        // Given
        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponent

            // When
            (paymentComponentView.findViewById(R.id.ghs_more_information) as TextView).performClick()
            // Then
            verify {
                paymentComponentListener.onMoreInformationClicked()
            }
        }
    }

    @Test
    fun `calls onBankPickerClicked method of listener when clicking on select bank button`() = runTest {
        // Given
        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponent

            // When
            (paymentComponentView.findViewById(R.id.ghs_select_bank_button) as Button).performClick()

            // Then
            verify {
                paymentComponentListener.onBankPickerClicked()
            }
        }
    }

    @Test
    fun `does not call onPayInvoiceClicked method of listener if no document id is set`() = runTest {
        // Given
        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponent

            // When
            (paymentComponentView.findViewById(R.id.ghs_pay_invoice_button) as Button).performClick()

            // Then
            verify(exactly = 0) { paymentComponentListener.onPayInvoiceClicked("") }
        }
    }

    //TODO - the test below only fails on automatic run in GitHub, to be investigated further
//    @Test
//    fun `calls onPayInvoiceClicked method of listener when clicking on pay invoice button and document id`() = runTest {
//        // Given
//        scenario?.onActivity { activity ->
//            val paymentComponentView = PaymentComponentView(activity, null)
//            paymentComponentView.paymentComponent = paymentComponent
//            paymentComponentView.documentId = "123"
//            paymentComponentView.coroutineScope = CoroutineScope(Dispatchers.Default)
//
//            // When
//            (paymentComponentView.findViewById(R.id.ghs_pay_invoice_button) as Button).performClick()
//
//            // Then
//            verify {
//                paymentComponentListener.onPayInvoiceClicked("123")
//            }
//        }
//    }

    @Test
    fun `disables buttons and deletes document id to reuse`() = runTest {
        // Given
        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponent
            paymentComponentView.documentId = "123"
            paymentComponentView.isPayable = true

            Truth.assertThat(paymentComponentView.documentId).isEqualTo("123")
            Truth.assertThat(paymentComponentView.isPayable).isEqualTo(true)
            Truth.assertThat((paymentComponentView.findViewById(R.id.ghs_pay_invoice_button) as Button).isEnabled).isEqualTo(true)
            Truth.assertThat((paymentComponentView.findViewById(R.id.ghs_select_bank_button) as Button).isEnabled).isEqualTo(true)

            // When
            paymentComponentView.prepareForReuse()

            // Then
            Truth.assertThat(paymentComponentView.documentId).isNull()
            Truth.assertThat(paymentComponentView.isPayable).isEqualTo(false)
            Truth.assertThat((paymentComponentView.findViewById(R.id.ghs_pay_invoice_button) as Button).isEnabled).isEqualTo(false)
            Truth.assertThat((paymentComponentView.findViewById(R.id.ghs_select_bank_button) as Button).isEnabled).isEqualTo(false)
        }
    }
}