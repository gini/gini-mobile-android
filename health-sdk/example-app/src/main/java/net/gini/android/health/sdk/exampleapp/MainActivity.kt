package net.gini.android.health.sdk.exampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.databinding.ActivityMainBinding
import net.gini.android.health.sdk.exampleapp.invoices.ui.InvoicesActivity
import net.gini.android.health.sdk.exampleapp.pager.PagerAdapter
import net.gini.android.health.sdk.exampleapp.review.ReviewActivity
import net.gini.android.health.sdk.exampleapp.upload.UploadActivity
import net.gini.android.health.sdk.requirement.Requirement
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture(), ::photoResult)
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument(), ::importResult)
    private lateinit var binding: ActivityMainBinding

    private val useTestDocument = false
    private val testDocumentId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.takePhoto.setOnClickListener {
            takePhoto()
        }

        binding.importFile.setOnClickListener {
            if (useTestDocument) {
                viewModel.setDocumentForReview(testDocumentId)
                startActivity(ReviewActivity.getStartIntent(this))
            } else {
                importFile()
            }
        }

        binding.pager.adapter = PagerAdapter().apply {
            lifecycleScope.launchWhenStarted {
                viewModel.pages.collect { pages ->
                    submitList(pages)
                }
            }
        }

        TabLayoutMediator(binding.indicator, binding.pager) { _, _ -> }.attach()

        binding.upload.setOnClickListener {
            checkRequirements(
                before = {
                    binding.upload.isEnabled = false
                },
                action = {
                    startActivity(
                        UploadActivity.getStartIntent(
                            this@MainActivity,
                            viewModel.pages.value.map { it.uri })
                    )
                },
                after = {
                    binding.upload.isEnabled = true
                })
        }

        binding.invoicesScreen.setOnClickListener {
            startActivity(Intent(this, InvoicesActivity::class.java))
        }
    }

    private fun importFile() {
        checkRequirements(
            before = {
                binding.importFile.isEnabled = false
            },
            action = {
                importLauncher.launch(arrayOf("image/*", "application/pdf"))
            },
            after = {
                binding.importFile.isEnabled = true
            })
    }

    private fun checkRequirements(before: () -> Unit = {}, action: suspend () -> Unit, after: suspend () -> Unit = {}) {
        binding.loadingIndicator.isVisible = true
        before()
        lifecycleScope.launch {
            try {
                val requirements = viewModel.checkRequirements(packageManager)
                if (requirements.isEmpty()) {
                    action()
                } else {
                    showMissingRequirements(requirements)
                }
            } catch (e: Exception) {
                Log.e("RequirementsCheck", "Failed to check requirements: $e")
                Toast.makeText(
                    this@MainActivity,
                    "Failed to check requirements. See Logcat for details.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.loadingIndicator.isVisible = false
                after()
            }
        }
    }

    private fun takePhoto() {
        takePictureLauncher.launch(viewModel.getNextPageUri(this@MainActivity))
    }

    private fun photoResult(saved: Boolean) {
        if (saved) {
            viewModel.onPhotoSaved()
            binding.upload.isEnabled = true
        }
    }

    private fun importResult(uri: Uri?) {
        uri?.let {
            startActivity(UploadActivity.getStartIntent(this, listOf(it)))
        } ?: run {
            Toast.makeText(this, "No document received", Toast.LENGTH_LONG).show()
        }
    }

    private fun showMissingRequirements(requirements: List<Requirement>) {
        requirements.joinToString(separator = "\n") { requirement ->
            when (requirement) {
                Requirement.NoBank -> getString(R.string.no_bank)
            }
        }.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
    }
}
