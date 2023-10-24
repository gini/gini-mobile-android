package net.gini.android.bank.sdk.exampleapp.ui.pay

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.sdk.exampleapp.core.ResultWrapper
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityPayBinding
import net.gini.android.bank.sdk.pay.getRequestId
import net.gini.android.core.api.models.PaymentRequest

@AndroidEntryPoint
class PayActivity : AppCompatActivity() {

    // Replace PayViewModel with PayViewModelJava to try out
    // using the GiniBank suspending functions from Java
    private val payViewModel: PayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val requestId = getRequestId(intent)
            payViewModel.fetchPaymentRequest(requestId)
        } catch (throwable: IllegalStateException) {
            Toast.makeText(this@PayActivity, throwable.message, Toast.LENGTH_LONG).show()
        }

        lifecycleScope.launchWhenStarted {
            payViewModel.paymentRequest.collect { result ->
                binding.progress.isVisible = result is ResultWrapper.Loading
                when (result) {
                    is ResultWrapper.Success -> {
                        binding.setPaymentDetails(result.value)
                        binding.resolvePayment.isEnabled = true
                    }
                    is ResultWrapper.Error -> {
                        Toast.makeText(this@PayActivity, result.error.message, Toast.LENGTH_LONG).show()
                    }
                    is ResultWrapper.Loading -> {
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            payViewModel.paymentState.collect { result ->
                binding.progress.isVisible = result is ResultWrapper.Loading
                when (result) {
                    is ResultWrapper.Success -> {
                        binding.resolvePayment.isVisible = false
                        binding.returnToPaymentInitiatorApp.isVisible = true
                    }
                    is ResultWrapper.Error -> {
                        Toast.makeText(this@PayActivity, result.error.message, Toast.LENGTH_LONG).show()
                        binding.enablePaymentDetails()
                    }
                    is ResultWrapper.Loading -> {
                    }
                }
            }
        }


        binding.resolvePayment.setOnClickListener {
            binding.disablePaymentDetails()
            payViewModel.onPay(binding.getPaymentDetails())
        }

        binding.returnToPaymentInitiatorApp.setOnClickListener {
            try {
                payViewModel.returnToPaymentInitiatorApp(this@PayActivity)
            } catch (t: Throwable) {
                Toast.makeText(this@PayActivity, t.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ActivityPayBinding.setPaymentDetails(paymentRequest: PaymentRequest) {
        recipient.setText(paymentRequest.recipient)
        iban.setText(paymentRequest.iban)
        amount.setText(paymentRequest.amount.filter { it.isDigit() || it == '.' || it == ',' })
        purpose.setText(paymentRequest.purpose)
    }

    private fun ActivityPayBinding.disablePaymentDetails() {
        recipient.isEnabled = false
        iban.isEnabled = false
        amount.isEnabled = false
        purpose.isEnabled = false
    }

    private fun ActivityPayBinding.enablePaymentDetails() {
        recipient.isEnabled = true
        iban.isEnabled = true
        amount.isEnabled = true
        purpose.isEnabled = true
    }

    private fun ActivityPayBinding.getPaymentDetails(): ResolvePaymentInput = ResolvePaymentInput(
        recipient.text.toString(),
        iban.text.toString(),
        amount.text.toString(),
        purpose.text.toString(),
    )
}
