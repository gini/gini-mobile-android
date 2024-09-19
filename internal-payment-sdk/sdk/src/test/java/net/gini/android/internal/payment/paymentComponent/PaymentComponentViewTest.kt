package net.gini.android.internal.payment.paymentComponent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.utils.GiniLocalization
import net.gini.android.internal.payment.R
import org.junit.After
import org.junit.Assert.assertEquals
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
    private lateinit var paymentComponentBrandedOff: PaymentComponent
    private lateinit var paymentComponentWithLocale: PaymentComponent
    private lateinit var paymentComponentListener: PaymentComponent.Listener
    private lateinit var giniPaymentModule: GiniInternalPaymentModule
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }

    @Before
    fun setUp() {
        every { giniHealthAPI.documentManager } returns documentManager
        context = ApplicationProvider.getApplicationContext()
        context!!.setTheme(R.style.GiniPaymentTheme)
        giniPaymentModule = GiniInternalPaymentModule(context!!, giniHealthAPI)

        paymentComponent = PaymentComponent(context!!, mockk())
        paymentComponentBrandedOff = PaymentComponent(context!!, mockk(), PaymentComponentConfiguration(isPaymentComponentBranded = false))
        paymentComponentWithLocale = PaymentComponent(context!!, giniPaymentModule)
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
    fun `does not call onPayInvoiceClicked method of listener if no document id is set`() = runTest {
        // Given
        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponent

            // When
            (paymentComponentView.findViewById(R.id.gps_pay_invoice_button) as Button).performClick()

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
    fun `hides powered by Gini`() = runTest {
        // Given
        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponentBrandedOff
            paymentComponentView.isPayable = true

            // Then
            Truth.assertThat((paymentComponentView.findViewById<FrameLayout>(R.id.gps_powered_by_gini)!!).isVisible).isEqualTo(false)
        }
    }

    @Test
    fun `shows text values in english if that is set to GiniHealth`() = runTest {
        // Given
        giniPaymentModule.setSDKLanguage(GiniLocalization.ENGLISH, context!!)

        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponentWithLocale
            paymentComponentView.isPayable = true

            // Then
            assertEquals("English text", "More information.", paymentComponentView.findViewById<TextView>(R.id.gps_more_information)!!.text.toString())
            assertEquals("English text", "Pay the invoice", paymentComponentView.findViewById<Button>(R.id.gps_pay_invoice_button)!!.text.toString())
        }
    }

    @Test
    fun `shows text values in german if that is set to GiniHealth`() = runTest {
        // Given
        giniPaymentModule.setSDKLanguage(GiniLocalization.GERMAN, context!!)

        scenario?.onActivity { activity ->
            val paymentComponentView = PaymentComponentView(activity, null)
            paymentComponentView.paymentComponent = paymentComponentWithLocale
            paymentComponentView.isPayable = true

            // Then
            assertEquals("German text", "Mehr Informationen.", paymentComponentView.findViewById<TextView>(R.id.gps_more_information)!!.text.toString())
            assertEquals("German text", "Rechnung bezahlen", paymentComponentView.findViewById<Button>(R.id.gps_pay_invoice_button)!!.text.toString())
        }
    }
}
