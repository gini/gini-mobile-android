package net.gini.pay.app.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
import dev.chrisbanes.insetter.applyInsetter
import net.gini.pay.app.R
import net.gini.pay.app.databinding.ActivityReviewBinding
import net.gini.pay.ginipaybusiness.GiniBusiness
import net.gini.pay.ginipaybusiness.review.ReviewConfiguration
import net.gini.pay.ginipaybusiness.review.ReviewFragment
import net.gini.pay.ginipaybusiness.review.ReviewFragmentListener
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReviewActivity : AppCompatActivity() {

    private val viewModel: ReviewViewModel by viewModel()

    private val reviewFragmentListener = object: ReviewFragmentListener {
        override fun onCloseReview() {
            finish()
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
        viewModel.giniBusiness.setSavedStateRegistryOwner(this, viewModel.viewModelScope)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        supportFragmentManager.fragmentFactory = ReviewFragmentFactory(
            viewModel.giniBusiness,
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
        fun getStartIntent(context: Context, pages: List<Uri>): Intent = Intent(context, ReviewActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_URIS, if (pages is ArrayList<Uri>) pages else ArrayList<Uri>().apply { addAll(pages) })
        }

        private val Intent.pageUris: List<Uri>
            get() = getParcelableArrayListExtra<Uri>(EXTRA_URIS)?.toList() ?: emptyList()
    }
}


class ReviewFragmentFactory(private val giniBusiness: GiniBusiness,
                            private val configuration: ReviewConfiguration,
                            private val listener: ReviewFragmentListener
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return ReviewFragment(giniBusiness, configuration, listener)
    }
}