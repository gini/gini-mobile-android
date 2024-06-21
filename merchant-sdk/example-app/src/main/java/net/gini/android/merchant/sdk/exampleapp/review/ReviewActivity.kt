package net.gini.android.merchant.sdk.exampleapp.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.exampleapp.databinding.ActivityReviewBinding
import net.gini.android.merchant.sdk.review.ReviewFragment
import net.gini.android.merchant.sdk.review.ReviewFragmentListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

class ReviewActivity : AppCompatActivity() {

    private val viewModel: ReviewViewModel by viewModel()

    private val reviewFragmentListener = object : ReviewFragmentListener {
        override fun onCloseReview() {
            LOG.debug("on close clicked")
            finish()
        }

        override fun onToTheBankButtonClicked(paymentProviderName: String) {
            LOG.debug("to the bank button clicked with payment provider: {}", paymentProviderName)
            lifecycleScope.launch {
                viewModel.giniMerchant.eventsFlow.collect { event ->
                    when (event) {
                        GiniMerchant.MerchantSDKEvents.OnLoading -> {
                            LOG.debug("opening bank app")
                        }

                        is GiniMerchant.MerchantSDKEvents.OnFinishedWithPaymentRequestCreated -> {
                            LOG.debug("launching bank app: {}", event.paymentProviderName)
                            cancel()
                        }

                        is GiniMerchant.MerchantSDKEvents.OnErrorOccurred -> {
                            LOG.error( "failed to open bank app:", event.throwable)
                            cancel()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * Set it to `true` to show the close button instead of the toolbar.
     *
     * When 'false' then also set the dimen resource `gpb_page_padding_top` to equal
     * `toolbar_height` to prevent the toolbar overlapping the top of the page.
     */
    private val showCloseButton = true

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.giniMerchant.setSavedStateRegistryOwner(this, viewModel.viewModelScope)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        val binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.isGone = showCloseButton

        binding.toolbar.applyInsetter {
            type(statusBars = true) {
                padding(top = true)
            }
        }

        binding.reviewFragment.applyInsetter {
            type(statusBars = true, navigationBars = true) {
                padding(top = true, bottom = true)
            }
        }

        binding.close.setOnClickListener { finish() }

        // Reattach the listener to the ReviewFragment if it is being shown (in case of configuration changes)
        supportFragmentManager.findFragmentByTag(REVIEW_FRAGMENT_TAG)?.let {
            (it as? ReviewFragment)?.listener = reviewFragmentListener
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ReviewActivity::class.java)

        private const val EXTRA_URIS = "EXTRA_URIS"
        private const val EXTRA_DOCUMENT_ID = "EXTRA_DOCUMENT_ID"

        fun getStartIntent(context: Context, pages: List<Uri> = emptyList(), documentId: String): Intent =
            Intent(context, ReviewActivity::class.java).apply {
                putParcelableArrayListExtra(
                    EXTRA_URIS,
                    if (pages is ArrayList<Uri>) pages else ArrayList<Uri>().apply { addAll(pages) })
                putExtra(EXTRA_DOCUMENT_ID, documentId)
            }

        private val Intent.pageUris: List<Uri>
            get() = getParcelableArrayListExtra<Uri>(EXTRA_URIS)?.toList() ?: emptyList()

        private val Intent.documentId: String
            get() = getStringExtra(EXTRA_DOCUMENT_ID) ?: throw Exception("Document ID not found in intent")
    }
}

private const val REVIEW_FRAGMENT_TAG = "payment_review_fragment"
