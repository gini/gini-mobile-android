package net.gini.android.capture.screen.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.capture.screen.R
import net.gini.android.capture.screen.databinding.ActivityConfigurationBinding
import net.gini.android.capture.screen.ui.MainActivity.Companion.CONFIGURATION_BUNDLE
import net.gini.android.capture.screen.ui.data.Configuration

@AndroidEntryPoint
class ConfigurationActivity : AppCompatActivity(R.layout.activity_configuration) {

    private lateinit var binding: ActivityConfigurationBinding
    private lateinit var configuration : Configuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        configuration = intent.getParcelableExtra(CONFIGURATION_BUNDLE) ?: Configuration()

        updateUIWithConfigurationObject()
        setConfigurationFeatures()
        binding.buttonCloseConfiguration.setOnClickListener {
            val returnIntent = Intent().putExtra(CONFIGURATION_BUNDLE, configuration)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    private fun updateUIWithConfigurationObject() {
        //binding.switchOpenWith.isEnabled = configuration.isQrCodeEnabled
        binding.switchQrCodeScanning.isChecked = configuration.isQrCodeEnabled
        binding.switchOnlyQRCodeScanning.isChecked = configuration.isOnlyQrCodeEnabled

        binding.switchMultiPage.isChecked = configuration.isMultiPageEnabled

        binding.switchFlashToggle.isChecked = configuration.isFlashToggleEnabled
        binding.switchFlashOnByDefault.isChecked = configuration.isFlashOnByDefault
    }

    private fun setConfigurationFeatures() {
        //TODO: open with is not set yet!
        binding.switchOpenWith.setOnCheckedChangeListener { _, isChecked ->

        }

        binding.switchQrCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isQrCodeEnabled = isChecked)
        }
        binding.switchOnlyQRCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isOnlyQrCodeEnabled = isChecked)
        }
        binding.switchMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isMultiPageEnabled = isChecked)
        }
        binding.switchFlashToggle.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isFlashToggleEnabled = isChecked)
        }
        binding.switchFlashOnByDefault.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isFlashOnByDefault = isChecked)
        }
        //TODO: implement file import functionality


        binding.switchShowBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isBottomNavigationBarEnabled = isChecked)
        }
        binding.switchShowHelpScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isHelpScreensCustomBottomNavBarEnabled = isChecked)
        }
        binding.switchCameraScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isCameraBottomNavBarEnabled = isChecked)
        }
        binding.switchReviewScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configuration = configuration.copy(isReviewScreenCustomBottomNavBarEnabled = isChecked)
        }




    }



}