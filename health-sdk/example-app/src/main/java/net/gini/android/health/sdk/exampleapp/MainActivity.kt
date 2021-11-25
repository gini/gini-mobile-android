package net.gini.android.health.sdk.exampleapp

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import java.util.*
import kotlinx.coroutines.flow.collect
import net.gini.android.health.sdk.exampleapp.databinding.ActivityMainBinding
import net.gini.android.health.sdk.exampleapp.pager.PagerAdapter
import net.gini.android.health.sdk.exampleapp.upload.UploadActivity
import net.gini.android.health.sdk.requirement.Requirement
import net.gini.android.health.sdk.exampleapp.R
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture(), ::photoResult)
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument(), ::importResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.takePhoto.setOnClickListener {
            takePhoto()
        }

        binding.importFile.setOnClickListener {
            importFile()
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
            val requirements = viewModel.checkRequirements(packageManager)
            if (requirements.isEmpty()) {
                startActivity(UploadActivity.getStartIntent(this, viewModel.pages.value.map { it.uri }))
            } else {
                showMissingRequirements(requirements)
            }
        }
    }

    private fun importFile() {
        val requirements = viewModel.checkRequirements(packageManager)
        if (requirements.isNotEmpty()) {
            showMissingRequirements(requirements)
        }
        // Let's ignore it for import to see what happens
        importLauncher.launch(arrayOf("image/*", "application/pdf"))
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