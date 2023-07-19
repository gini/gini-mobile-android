package net.gini.android.capture.screen.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.capture.screen.R
import net.gini.android.capture.screen.databinding.ActivityConfigurationBinding
import net.gini.android.capture.screen.ui.MainActivity.Companion.CONFIGURATION_BUNDLE
import net.gini.android.capture.screen.ui.data.Configuration


@AndroidEntryPoint
class ConfigurationActivity : AppCompatActivity(R.layout.activity_configuration) {

    private lateinit var binding: ActivityConfigurationBinding
    private lateinit var configuration: Configuration
    private lateinit var configurationViewModel: ConfigurationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurationViewModel = ViewModelProvider(this)[ConfigurationViewModel::class.java]

        configuration = intent.getParcelableExtra(CONFIGURATION_BUNDLE) ?: Configuration()
        configurationViewModel.setConfiguration(configuration)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                configurationViewModel.configurationFlow.collect {
                    configuration = it
                }
            }
        }

        updateUIWithConfigurationObject()
        setConfigurationFeatures()
        binding.buttonCloseConfiguration.setOnClickListener {
            returnToMainActivity()
        }
    }

    override fun onBackPressed() {
        returnToMainActivity()
    }

    private fun returnToMainActivity() {
        val returnIntent = Intent().putExtra(CONFIGURATION_BUNDLE, configuration)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    private fun updateUIWithConfigurationObject() {
        //binding.switchOpenWith.isEnabled = configuration.isQrCodeEnabled
        binding.switchQrCodeScanning.isChecked = configuration.isQrCodeEnabled
        binding.switchOnlyQRCodeScanning.isChecked = configuration.isOnlyQrCodeEnabled

        binding.switchMultiPage.isChecked = configuration.isMultiPageEnabled

        binding.switchFlashToggle.isChecked = configuration.isFlashToggleEnabled
        binding.switchFlashOnByDefault.isChecked = configuration.isFlashOnByDefault
        binding.switchShowBottomNavbar.isChecked = configuration.isBottomNavigationBarEnabled
    }

    private fun setConfigurationFeatures() {
        //TODO: open with is not set yet!
        binding.switchOpenWith.setOnCheckedChangeListener { _, isChecked ->

        }

        binding.switchQrCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(configuration.copy(isQrCodeEnabled = isChecked))

        }
        binding.switchOnlyQRCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(configuration.copy(isOnlyQrCodeEnabled = isChecked))
        }
        binding.switchMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(configuration.copy(isMultiPageEnabled = isChecked))
        }
        binding.switchFlashToggle.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(configuration.copy(isFlashToggleEnabled = isChecked))
        }
        binding.switchFlashOnByDefault.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(configuration.copy(isFlashOnByDefault = isChecked))
        }
        //TODO: implement file import functionality


        binding.switchShowBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(configuration.copy(isBottomNavigationBarEnabled = isChecked))
        }
        binding.switchShowHelpScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configuration.copy(
                    isHelpScreensCustomBottomNavBarEnabled = isChecked
                )
            )
        }
        binding.switchCameraScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(configuration.copy(isCameraBottomNavBarEnabled = isChecked))
        }
        binding.switchReviewScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configuration.copy(
                    isReviewScreenCustomBottomNavBarEnabled = isChecked
                )
            )
        }


    }


}