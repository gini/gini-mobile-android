package net.gini.android.capture.screen.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed
import net.gini.android.capture.screen.R
import net.gini.android.capture.screen.databinding.ActivityConfigurationBinding
import net.gini.android.capture.screen.ui.MainActivity.Companion.CONFIGURATION_BUNDLE
import net.gini.android.capture.screen.ui.data.Configuration


@AndroidEntryPoint
class ConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigurationBinding
    lateinit var configurationViewModel: ConfigurationViewModel
    // TODO: use viewModels and remove ViewModelProvider(this)[ConfigurationViewModel::class.java]
    //val configurationViewModel1 by viewModels<ConfigurationViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurationViewModel = ViewModelProvider(this)[ConfigurationViewModel::class.java]

        configurationViewModel.setConfiguration(
            intent.getParcelableExtra(CONFIGURATION_BUNDLE) ?: Configuration()
        )

        setupActionBar()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                configurationViewModel.configurationFlow.collect {
                    updateUIWithConfigurationObject(it)
                }
            }
        }


        setConfigurationFeatures()
        handleOnBackPressed()
    }

    private fun setupActionBar() {
        supportActionBar?.setHomeAsUpIndicator(R.drawable.gc_close)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun handleOnBackPressed() {
        interceptOnBackPressed(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToMainActivity()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                returnToMainActivity()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun returnToMainActivity() {
        val returnIntent =
            Intent().putExtra(CONFIGURATION_BUNDLE, configurationViewModel.configurationFlow.value)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    private fun updateUIWithConfigurationObject(configuration: Configuration) {
        // 1 open with
        binding.switchOpenWith.isChecked = configuration.isFileImportEnabled
        // 2 QR code scanning
        binding.switchQrCodeScanning.isChecked = configuration.isQrCodeEnabled
        // 3 only QR code scanning
        binding.switchOnlyQRCodeScanning.isChecked = configuration.isOnlyQrCodeEnabled
        // 4 enable multi page
        binding.switchMultiPage.isChecked = configuration.isMultiPageEnabled
        // 5 enable flash toggle
        binding.switchFlashToggle.isChecked = configuration.isFlashToggleEnabled
        // 6 enable flash on by default
        binding.switchFlashOnByDefault.isChecked = configuration.isFlashOnByDefault
        // 7 set import document type support
        val checkButtonId = when (configuration.documentImportEnabledFileTypes) {
            DocumentImportEnabledFileTypes.NONE -> R.id.btn_fileImportDisabled
            DocumentImportEnabledFileTypes.PDF -> R.id.btn_fileImportOnlyPdf
            DocumentImportEnabledFileTypes.PDF_AND_IMAGES -> R.id.btn_fileImportPdfAndImage
            else -> R.id.btn_fileImportOnlyPdf
        }
        binding.toggleBtnFileImportSetup.check(checkButtonId)

        // 8 enable bottom navigation bar
        binding.switchShowBottomNavbar.isChecked = configuration.isBottomNavigationBarEnabled
        // 9 enable Help screens custom bottom navigation bar

        // 10 enable camera screens custom bottom navigation bar

        // 11 enable review screens custom bottom navigation bar

        // 12 enable image picker screens custom bottom navigation bar

        // 13 enable onboarding screens at first launch
        binding.switchOnboardingScreensAtFirstRun.isChecked =
            configuration.isOnboardingAtFirstRunEnabled

        // 14 enable onboarding at every launch
        binding.switchOnboardingScreensAtEveryLaunch.isChecked =
            configuration.isOnboardingAtEveryLaunchEnabled

        // 15 enable custom onboarding pages
        binding.switchCustomOnboardingPages.isChecked =
            configuration.isCustomOnboardingPagesEnabled
        // 16 enable align corners onboarding pages
        binding.switchCustomOnboardingAlignCornersPage.isChecked =
            configuration.isAlignCornersInCustomOnboardingEnabled

        // 17 enable lighting in custom onboarding pages
        binding.switchCustomOnboardingLightingPage.isChecked =
            configuration.isLightingInCustomOnboardingEnabled
        // 18 enable QR code in custom onboarding pages
        binding.switchCustomOnboardingQRCodePage.isChecked =
            configuration.isQRCodeInCustomOnboardingEnabled
        // 19 enable multi page in custom onboarding pages
        binding.switchCustomOnboardingMultiPage.isChecked =
            configuration.isMultiPageInCustomOnboardingEnabled
        // 20 enable custom navigation bar in custom onboarding pages

        // 21 enable button's custom loading indicator
        binding.switchButtonsCustomLoadingIndicator.isChecked =
            configuration.isButtonsCustomLoadingIndicatorEnabled

        // 22 enable screen's custom loading indicator
        binding.switchScreenCustomLoadingIndicator.isChecked =
            configuration.isScreenCustomLoadingIndicatorEnabled

        // 23 enable supported format help screen
        binding.switchSupportedFormatsScreen.isChecked =
            configuration.isSupportedFormatsHelpScreenEnabled

        // 24 enable custom help items
        binding.switchCustomHelpMenuItems.isChecked =
            configuration.isCustomHelpItemsEnabled

        // 25 enable custom navigation bar
        binding.switchCustomNavigationController.isChecked =
            configuration.isCustomNavBarEnabled

        // 26 enable event tracker
        binding.switchEventTracker.isChecked =
            configuration.isEventTrackerEnabled

        // 27 enable Gini error logger
        binding.switchGiniErrorLogger.isChecked =
            configuration.isGiniErrorLoggerEnabled

    }

    private fun setConfigurationFeatures() {
        // 1 open with
        binding.switchOpenWith.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isFileImportEnabled = isChecked
                )
            )
        }

        // 2 QR code scanning
        binding.switchQrCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isQrCodeEnabled = isChecked
                )
            )

        }
        // 3 only QR code scanning
        binding.switchOnlyQRCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnlyQrCodeEnabled = isChecked
                )
            )
        }
        // 4 enable multi page
        binding.switchMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isMultiPageEnabled = isChecked
                )
            )
        }
        // 5 enable flash toggle
        binding.switchFlashToggle.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isFlashToggleEnabled = isChecked
                )
            )
        }
        // 6 enable flash on by default
        binding.switchFlashOnByDefault.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isFlashOnByDefault = isChecked
                )
            )
        }
        // 7 set import document type support
        binding.toggleBtnFileImportSetup.addOnButtonCheckedListener { toggleButton, checkedId, isChecked ->
            val checked = toggleButton.checkedButtonId
            configurationViewModel.setConfiguration(
                when (checked) {
                    R.id.btn_fileImportDisabled -> configurationViewModel.configurationFlow.value.copy(
                        documentImportEnabledFileTypes = DocumentImportEnabledFileTypes.NONE
                    )

                    R.id.btn_fileImportOnlyPdf -> configurationViewModel.configurationFlow.value.copy(
                        documentImportEnabledFileTypes = DocumentImportEnabledFileTypes.PDF
                    )

                    R.id.btn_fileImportPdfAndImage -> configurationViewModel.configurationFlow.value.copy(
                        documentImportEnabledFileTypes = DocumentImportEnabledFileTypes.PDF_AND_IMAGES
                    )

                    else -> configurationViewModel.configurationFlow.value.copy(
                        documentImportEnabledFileTypes = DocumentImportEnabledFileTypes.NONE
                    )
                }
            )
        }

        // 8 enable bottom navigation bar
        binding.switchShowBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isBottomNavigationBarEnabled = isChecked
                )
            )
        }

        // 9 enable Help screens custom bottom navigation bar
        binding.switchShowHelpScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isHelpScreensCustomBottomNavBarEnabled = isChecked
                )
            )
        }

        // 10 enable camera screens custom bottom navigation bar
        binding.switchCameraScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCameraBottomNavBarEnabled = isChecked
                )
            )
        }

        // 11 enable review screens custom bottom navigation bar
        binding.switchReviewScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isReviewScreenCustomBottomNavBarEnabled = isChecked
                )
            )
        }

        // 12 enable image picker screens custom bottom navigation bar
        binding.switchImagePickerScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isReviewScreenCustomBottomNavBarEnabled = isChecked
                )
            )
        }

        // 13 enable onboarding screens at first launch
        binding.switchOnboardingScreensAtFirstRun.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnboardingAtFirstRunEnabled = isChecked
                )
            )
        }

        // 14 enable onboarding at every launch
        binding.switchOnboardingScreensAtEveryLaunch.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnboardingAtEveryLaunchEnabled = isChecked
                )
            )
        }

        // 15 enable custom onboarding pages
        binding.switchCustomOnboardingPages.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomOnboardingPagesEnabled = isChecked
                )
            )
        }
        // 16 enable align corners onboarding pages
        binding.switchCustomOnboardingAlignCornersPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isAlignCornersInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 17 enable lighting in custom onboarding pages
        binding.switchCustomOnboardingLightingPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isLightingInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 18 enable QR code in custom onboarding pages
        binding.switchCustomOnboardingQRCodePage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isQRCodeInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 19 enable multi page in custom onboarding pages
        binding.switchCustomOnboardingMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isMultiPageInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 20 enable custom navigation bar in custom onboarding pages


        // 21 enable button's custom loading indicator
        binding.switchButtonsCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isButtonsCustomLoadingIndicatorEnabled = isChecked
                )
            )
        }

        // 22 enable screen's custom loading indicator
        binding.switchScreenCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isScreenCustomLoadingIndicatorEnabled = isChecked
                )
            )
        }

        // 23 enable supported format help screen
        binding.switchSupportedFormatsScreen.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isSupportedFormatsHelpScreenEnabled = isChecked
                )
            )
        }

        // 24 enable custom help items
        binding.switchCustomHelpMenuItems.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomHelpItemsEnabled = isChecked
                )
            )
        }

        // 25 enable custom navigation bar
        binding.switchCustomNavigationController.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomNavBarEnabled = isChecked
                )
            )
        }

        // 26 enable event tracker
        binding.switchEventTracker.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isEventTrackerEnabled = isChecked
                )
            )
        }

        // 27 enable Gini error logger
        binding.switchGiniErrorLogger.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isGiniErrorLoggerEnabled = isChecked
                )
            )
        }


    }


}