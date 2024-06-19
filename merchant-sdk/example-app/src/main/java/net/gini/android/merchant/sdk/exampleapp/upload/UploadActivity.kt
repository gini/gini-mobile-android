package net.gini.android.merchant.sdk.exampleapp.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import net.gini.android.merchant.sdk.exampleapp.R
import net.gini.android.merchant.sdk.exampleapp.databinding.ActivityUploadBinding
import net.gini.android.merchant.sdk.exampleapp.review.ReviewActivity
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
            if (viewModel.uploadState.value is UploadViewModel.UploadState.Success) {
                val documentId = (viewModel.uploadState.value as UploadViewModel.UploadState.Success).documentId
                startActivity(ReviewActivity.getStartIntent(this,
                    pages = intent.pageUris,
                    documentId = documentId
                ))
            } else {
                Snackbar.make(binding.root, getString(R.string.missing_document_id), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun ActivityUploadBinding.updateViews(uploadState: UploadViewModel.UploadState) {
        progress.isVisible = uploadState is UploadViewModel.UploadState.Loading
        message.isVisible = uploadState !is UploadViewModel.UploadState.Loading
        payment.isEnabled = uploadState is UploadViewModel.UploadState.Success
        message.text = when (uploadState) {
            is UploadViewModel.UploadState.Failure -> getString(R.string.upload_failed)
            is UploadViewModel.UploadState.Success -> getString(R.string.upload_succeeded)
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