package net.gini.pay.app.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import net.gini.pay.app.R
import net.gini.pay.app.databinding.ActivityUploadBinding
import net.gini.pay.app.review.ReviewActivity
import net.gini.pay.app.upload.UploadViewModel.UploadState
import org.koin.androidx.viewmodel.ext.android.viewModel

class UploadActivity : AppCompatActivity() {

    private val viewModel: UploadViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            viewModel.uploadDocuments(contentResolver, intent.pageUris)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uploadState.collect { uploadState ->
                binding.updateViews(uploadState)
            }
        }

        binding.payment.setOnClickListener {
            startActivity(ReviewActivity.getStartIntent(this, intent.pageUris))
        }
    }

    private fun ActivityUploadBinding.updateViews(uploadState: UploadState) {
        progress.isVisible = uploadState is UploadState.Loading
        message.isVisible = uploadState !is UploadState.Loading
        payment.isEnabled = uploadState is UploadState.Success
        message.text = when (uploadState) {
            is UploadState.Failure -> getString(R.string.upload_failed)
            is UploadState.Success -> getString(R.string.upload_succeeded)
            else -> ""
        }
    }

    companion object {
        private const val EXTRA_URIS = "EXTRA_URIS"
        fun getStartIntent(context: Context, pages: List<Uri>): Intent = Intent(context, UploadActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_URIS, if (pages is ArrayList<Uri>) pages else ArrayList<Uri>().apply { addAll(pages) })
        }

        private val Intent.pageUris: List<Uri>
            get() = getParcelableArrayListExtra<Uri>(EXTRA_URIS)?.toList() ?: emptyList()
    }
}