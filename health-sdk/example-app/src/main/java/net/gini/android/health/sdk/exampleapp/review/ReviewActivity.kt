package net.gini.android.health.sdk.exampleapp.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityReviewBinding
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.review.ReviewConfiguration
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.review.ReviewFragmentListener
import org.koin.androidx.scope.activityScope
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReviewActivity : AppCompatActivity() {

    private val viewModel: ReviewViewModel by viewModel()

    private val reviewFragmentListener = object: ReviewFragmentListener {
        override fun onCloseReview() {
            Log.i("review_events", "on close clicked")
            finish()
        }

        override fun onNextClicked(paymentProviderName: String) {
            Log.i("review_events", "pay button clicked with payment provider: $paymentProviderName")
            lifecycleScope.launch {
                viewModel.giniHealth.openBankState.collect { paymentState ->
                    when (paymentState) {
                        GiniHealth.PaymentState.Loading -> {
                            Log.i("open_bank_state", "opening bank app")
                        }
                        is GiniHealth.PaymentState.Success -> {
                            Log.i("open_bank_state", "launching bank app: ${paymentState.paymentRequest.bankApp.name}")
                            cancel()
                        }
                        is GiniHealth.PaymentState.Error -> {
                            Log.e("open_bank_state", "failed to open bank app: ${paymentState.throwable}")
                            cancel()
                        }
                        GiniHealth.PaymentState.NoAction -> {}
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
        viewModel.giniHealth.setSavedStateRegistryOwner(this, viewModel.viewModelScope)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        supportFragmentManager.fragmentFactory = ReviewFragmentFactory(
            viewModel.giniHealth,
            ReviewConfiguration(showCloseButton = showCloseButton),
            reviewFragmentListener
        )

        super.onCreate(savedInstanceState)

        val binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.isGone = showCloseButton

        binding.toolbar.applyInsetter {
            type(statusBars = true) {
                padding(top = true)
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.review_fragment, ReviewFragment::class.java, null)
            }
        }

        binding.close.setOnClickListener { finish() }
    }

    companion object {
        private const val EXTRA_URIS = "EXTRA_URIS"
        fun getStartIntent(context: Context, pages: List<Uri> = emptyList()): Intent = Intent(context, ReviewActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_URIS, if (pages is ArrayList<Uri>) pages else ArrayList<Uri>().apply { addAll(pages) })
        }

        private val Intent.pageUris: List<Uri>
            get() = getParcelableArrayListExtra<Uri>(EXTRA_URIS)?.toList() ?: emptyList()
    }
}


class ReviewFragmentFactory(private val giniHealth: GiniHealth,
                            private val configuration: ReviewConfiguration,
                            private val listener: ReviewFragmentListener
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return ReviewFragment(giniHealth, configuration, listener)
    }
}