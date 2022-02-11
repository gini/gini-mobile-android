package net.gini.android.bank.sdk.screenapiexample.pay

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.sdk.screenapiexample.databinding.ActivityPayBinding
import net.gini.android.bank.sdk.screenapiexample.util.ResultWrapper
import net.gini.android.bank.sdk.pay.getRequestId
import org.koin.androidx.viewmodel.ext.android.viewModel

class PayActivity : AppCompatActivity() {

    // Replace PayViewModelKotlin with PayViewModelJava to try out
    // using the GiniBank suspending functions from Java
    private val viewModel: PayViewModelJava by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val requestId = getRequestId(intent)
            viewModel.fetchPaymentRequest(requestId)
        } catch (throwable: IllegalStateException) {
            Toast.makeText(this@PayActivity, throwable.message, Toast.LENGTH_LONG).show()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.paymentRequest.collect { result ->
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
            viewModel.paymentState.collect { result ->
                binding.progress.isVisible = result is ResultWrapper.Loading
                when (result) {
                    is ResultWrapper.Success -> {
                        binding.resolvePayment.isVisible = false
                        binding.returnToPaymentInitiatorApp.isVisible = true
                    }
                    is ResultWrapper.Error -> {
                        Toast.makeText(this@PayActivity, result.error.message, Toast.LENGTH_LONG).show()
                    }
                    is ResultWrapper.Loading -> {
                    }
                }
            }
        }


        binding.resolvePayment.setOnClickListener {
            binding.disablePaymentDetails()
            viewModel.onPay(binding.getPaymentDetails())
        }

        binding.returnToPaymentInitiatorApp.setOnClickListener {
            try {
                viewModel.returnToPaymentInitiatorApp(this@PayActivity)
            } catch (t: Throwable) {
                Toast.makeText(this@PayActivity, t.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ActivityPayBinding.setPaymentDetails(paymentRequest: PaymentRequest) {
        recipient.setText(paymentRequest.recipient)
        iban.setText(paymentRequest.iban)
        amount.setText(paymentRequest.amount)
        purpose.setText(paymentRequest.purpose)
    }

    private fun ActivityPayBinding.disablePaymentDetails() {
        recipient.isEnabled = false
        iban.isEnabled = false
        amount.isEnabled = false
        purpose.isEnabled = false
    }

    private fun ActivityPayBinding.getPaymentDetails(): ResolvePaymentInput = ResolvePaymentInput(
        recipient.text.toString(),
        iban.text.toString(),
        amount.text.toString(),
        purpose.text.toString(),
    )
}
