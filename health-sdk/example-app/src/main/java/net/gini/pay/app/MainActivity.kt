package net.gini.pay.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import java.util.*
import kotlinx.coroutines.flow.collect
import net.gini.pay.app.databinding.ActivityMainBinding
import net.gini.pay.app.pager.PagerAdapter
import net.gini.pay.app.review.ReviewActivity
import net.gini.pay.app.upload.UploadActivity
import net.gini.pay.ginipaybusiness.requirement.Requirement
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
        }
    }

    private fun importResult(uri: Uri) {
        startActivity(UploadActivity.getStartIntent(this, listOf(uri)))
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