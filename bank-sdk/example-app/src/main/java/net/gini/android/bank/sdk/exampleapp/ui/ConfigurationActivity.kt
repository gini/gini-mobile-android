package net.gini.android.bank.sdk.exampleapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.DefaultNetworkServicesProvider
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityConfigurationBinding
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity.Companion.CAMERA_PERMISSION_BUNDLE
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity.Companion.CONFIGURATION_BUNDLE
import net.gini.android.bank.sdk.exampleapp.ui.data.Configuration
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed
import javax.inject.Inject

@AndroidEntryPoint
class ConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigurationBinding

    @Inject
    lateinit var defaultNetworkServicesProvider: DefaultNetworkServicesProvider

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurationViewModel.setConfiguration(
            intent.getParcelableExtra(CONFIGURATION_BUNDLE)!!
        )
        configurationViewModel.disableCameraPermission(
            intent.getBooleanExtra(CAMERA_PERMISSION_BUNDLE, false) ?: false
        )

        setupActionBar()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    configurationViewModel.configurationFlow.collect {
                        updateUIWithConfigurationObject(it)
                    }
                }
                launch {
                    configurationViewModel.disableCameraPermissionFlow.collect {
                        binding.layoutDebugDevelopmentOptionsToggles.switchDisableCameraPermission.isChecked =
                            it
                    }
                }

            }
        }

        binding.layoutDebugDevelopmentOptionsToggles.switchDisableCameraPermission.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.disableCameraPermission(isChecked)
        }

        setConfigurationFeatures()
        handleOnBackPressed()
    }

    private fun setupActionBar() {
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
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
            Intent()
                .putExtra(CONFIGURATION_BUNDLE, configurationViewModel.configurationFlow.value)
                .putExtra(
                    CAMERA_PERMISSION_BUNDLE,
                    configurationViewModel.disableCameraPermissionFlow.value
                )
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    private fun updateUIWithConfigurationObject(configuration: Configuration) {
        // 0 setup sdk with default configuration
        binding.layoutFeatureToggle.switchSetupSdkWithDefaultConfiguration.isChecked =
            configuration.isDefaultSDKConfigurationsEnabled
        // 1 file import
        binding.layoutFeatureToggle.switchOpenWith.isChecked = configuration.isFileImportEnabled
        // 2 QR code scanning
        binding.layoutFeatureToggle.switchQrCodeScanning.isChecked = configuration.isQrCodeEnabled
        // 3 only QR code scanning
        binding.layoutFeatureToggle.switchOnlyQRCodeScanning.isChecked =
            configuration.isOnlyQrCodeEnabled

        // 4 enable multi page
        binding.layoutFeatureToggle.switchMultiPage.isChecked = configuration.isMultiPageEnabled
        // 5 enable flash toggle
        binding.layoutCameraToggles.switchDisplayFlashButton.isChecked =
            configuration.isFlashButtonDisplayed
        // 6 enable flash on by default
        binding.layoutCameraToggles.switchFlashOnByDefault.isChecked =
            configuration.isFlashDefaultStateEnabled
        // 7 set import document type support
        val checkButtonId = when (configuration.documentImportEnabledFileTypes) {
            DocumentImportEnabledFileTypes.NONE -> R.id.btn_fileImportDisabled
            DocumentImportEnabledFileTypes.PDF -> R.id.btn_fileImportOnlyPdf
            DocumentImportEnabledFileTypes.PDF_AND_IMAGES -> R.id.btn_fileImportPdfAndImage
            else -> R.id.btn_fileImportOnlyPdf
        }
        binding.layoutFeatureToggle.toggleBtnFileImportSetup.check(checkButtonId)
        // 8 enable bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowBottomNavbar.isChecked =
            configuration.isBottomNavigationBarEnabled
        // 9 enable Help screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowHelpScreenCustomBottomNavbar.isChecked =
            configuration.isHelpScreensCustomBottomNavBarEnabled
        // 10 enable camera screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchCameraScreenCustomBottomNavbar.isChecked =
            configuration.isCameraBottomNavBarEnabled
        // 11 enable review screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchReviewScreenCustomBottomNavbar.isChecked =
            configuration.isReviewScreenCustomBottomNavBarEnabled
        // 39 enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoCustomBottomNavbar.isChecked =
            configuration.isSkontoCustomNavBarEnabled
        // 41 enable skonto help screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoHelpCustomBottomNavbar.isChecked =
            configuration.isSkontoHelpCustomNavBarEnabled

        // 42 enable digital invoice skonto screen custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceSkontoCustomBottomNavbar.isChecked =
            configuration.isDigitalInvoiceSkontoCustomNavBarEnabled

        // 12 enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android

        // 13 enable onboarding screens at first launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtFirstRun.isChecked =
            configuration.isOnboardingAtFirstRunEnabled
        // 14 enable onboarding at every launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtEveryLaunch.isChecked =
            configuration.isOnboardingAtEveryLaunchEnabled
        // 15 enable custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingPages.isChecked =
            configuration.isCustomOnboardingPagesEnabled
        // 16 enable align corners onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingAlignCornersPage.isChecked =
            configuration.isAlignCornersInCustomOnboardingEnabled
        // 17 enable lighting in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingLightingPage.isChecked =
            configuration.isLightingInCustomOnboardingEnabled
        // 18 enable QR code in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingQRCodePage.isChecked =
            configuration.isQRCodeInCustomOnboardingEnabled
        // 19 enable multi page in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingMultiPage.isChecked =
            configuration.isMultiPageInCustomOnboardingEnabled
        // 20 enable custom navigation bar in custom onboarding pages
        binding.layoutBottomNavigationToggles.switchOnboardingCustomNavBar.isChecked =
            configuration.isCustomNavigationBarInCustomOnboardingEnabled
        // 21 enable button's custom loading indicator
        binding.layoutGeneralUiCustomizationToggles.switchButtonsCustomLoadingIndicator.isChecked =
            configuration.isButtonsCustomLoadingIndicatorEnabled
        // 22 enable screen's custom loading indicator
        binding.layoutAnalysisToggles.switchScreenCustomLoadingIndicator.isChecked =
            configuration.isScreenCustomLoadingIndicatorEnabled
        // 23 enable supported format help screen
        binding.layoutHelpToggles.switchSupportedFormatsScreen.isChecked =
            configuration.isSupportedFormatsHelpScreenEnabled
        // 24 enable custom help items
        binding.layoutHelpToggles.switchCustomHelpMenuItems.isChecked =
            configuration.isCustomHelpItemsEnabled
        // 25 enable custom navigation bar
        binding.layoutGeneralUiCustomizationToggles.switchCustomNavigationController.isChecked =
            configuration.isCustomNavBarEnabled
        // 26 enable event tracker
        binding.layoutFeatureToggle.switchEventTracker.isChecked =
            configuration.isEventTrackerEnabled
        // 27 enable Gini error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchGiniErrorLogger.isChecked =
            configuration.isGiniErrorLoggerEnabled
        // 28 enable custom error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchCustomErrorLogger.isChecked =
            configuration.isCustomErrorLoggerEnabled
        // 29 set imported file size bytes limit
        binding.layoutDebugDevelopmentOptionsToggles.editTextImportedFileSizeBytesLimit.hint =
            configuration.importedFileSizeBytesLimit.toString()

        // 31 enable return assistant
        binding.layoutFeatureToggle.switchReturnAssistantFeature.isChecked =
            configuration.isReturnAssistantEnabled

        // 32 enable return reasons dialog
        binding.layoutReturnAssistantToggles.switchReturnReasonsDialog.isChecked =
            configuration.isReturnReasonsEnabled

        // 33 Digital invoice onboarding custom illustration
        binding.layoutReturnAssistantToggles.switchDigitalInvoiceOnboardingCustomIllustration.isChecked =
            configuration.isDigitalInvoiceOnboardingCustomIllustrationEnabled

        // 34 Digital invoice help bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceHelpBottomNavigationBar.isChecked =
            configuration.isDigitalInvoiceHelpBottomNavigationBarEnabled

        // 35 Digital invoice onboarding bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceOnboardingBottomNavigationBar.isChecked =
            configuration.isDigitalInvoiceOnboardingBottomNavigationBarEnabled

        // 36 Digital invoice bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceBottomNavigationBar.isChecked =
            configuration.isDigitalInvoiceBottomNavigationBarEnabled

        // Allow screenshots
        binding.layoutDebugDevelopmentOptionsToggles.switchAllowScreenshots.isChecked =
            configuration.isAllowScreenshotsEnabled

        // 37 Debug mode
        binding.layoutDebugDevelopmentOptionsToggles.switchDebugMode.isChecked =
            configuration.isDebugModeEnabled

        // 40 enable skonto
        binding.layoutFeatureToggle.switchSkontoFeature.isChecked = configuration.isSkontoEnabled

        // 43 enable transaction docs
        binding.layoutFeatureToggle.switchTransactionDocsFeature.isChecked =
            configuration.isTransactionDocsEnabled


        binding.layoutDebugDevelopmentOptionsToggles.editTextClientId.hint = configuration.clientId

        binding.layoutDebugDevelopmentOptionsToggles.editTextClientSecret.hint =
            configuration.clientSecret
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun setConfigurationFeatures() {
        // 0 setup sdk with default configuration
        binding.layoutFeatureToggle.switchSetupSdkWithDefaultConfiguration.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isDefaultSDKConfigurationsEnabled = isChecked
                )
            )
            if (isChecked) {
                configurationViewModel.setupSDKWithDefaultConfigurations()
            }
        }

        // 1 file import
        binding.layoutFeatureToggle.switchOpenWith.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isFileImportEnabled = isChecked
                )
            )
        }

        // 2 QR code scanning
        binding.layoutFeatureToggle.switchQrCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isQrCodeEnabled = isChecked
                )
            )
            if (!isChecked) {
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isOnlyQrCodeEnabled = false
                    )
                )
            }

        }
        // 3 only QR code scanning
        binding.layoutFeatureToggle.switchOnlyQRCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnlyQrCodeEnabled = isChecked
                )
            )
            if (isChecked) {
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isQrCodeEnabled = true
                    )
                )
            }
        }
        // 4 enable multi page
        binding.layoutFeatureToggle.switchMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isMultiPageEnabled = isChecked
                )
            )
        }
        // 5 enable flash toggle
        binding.layoutCameraToggles.switchDisplayFlashButton.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isFlashButtonDisplayed = isChecked
                )
            )
            if (!isChecked) {
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isFlashDefaultStateEnabled = false
                    )
                )
            }
        }
        // 6 enable flash on by default
        binding.layoutCameraToggles.switchFlashOnByDefault.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isFlashDefaultStateEnabled = isChecked
                )
            )
            if (isChecked) {
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isFlashButtonDisplayed = true
                    )
                )
            }
        }
        // 7 set import document type support
        binding.layoutFeatureToggle.toggleBtnFileImportSetup.addOnButtonCheckedListener { toggleButton, checkedId, isChecked ->
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
        binding.layoutBottomNavigationToggles.switchShowBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isBottomNavigationBarEnabled = isChecked
                )
            )
        }

        // 9 enable Help screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowHelpScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isHelpScreensCustomBottomNavBarEnabled = isChecked
                )
            )
        }

        // 10 enable camera screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchCameraScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCameraBottomNavBarEnabled = isChecked
                )
            )
        }

        // 11 enable review screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchReviewScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isReviewScreenCustomBottomNavBarEnabled = isChecked
                )
            )
        }

        // 39 enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isSkontoCustomNavBarEnabled = isChecked
                )
            )
        }

        // 41 enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoHelpCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isSkontoHelpCustomNavBarEnabled = isChecked
                    )
                )
            }

        // 42 enable digital invoice skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceSkontoCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isDigitalInvoiceSkontoCustomNavBarEnabled = isChecked
                    )
                )
            }

        // 12 enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android

        // 13 enable onboarding screens at first launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtFirstRun.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnboardingAtFirstRunEnabled = isChecked
                )
            )
        }

        // 14 enable onboarding at every launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtEveryLaunch.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnboardingAtEveryLaunchEnabled = isChecked
                )
            )
        }

        // 15 enable custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingPages.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomOnboardingPagesEnabled = isChecked
                )
            )
        }
        // 16 enable align corners onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingAlignCornersPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isAlignCornersInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 17 enable lighting in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingLightingPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isLightingInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 18 enable QR code in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingQRCodePage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isQRCodeInCustomOnboardingEnabled = isChecked
                )
            )
        }

        // 19 enable multi page in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isMultiPageInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 20 enable custom navigation bar in custom onboarding pages
        binding.layoutBottomNavigationToggles.switchOnboardingCustomNavBar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomNavigationBarInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // 21 enable button's custom loading indicator
        binding.layoutGeneralUiCustomizationToggles.switchButtonsCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isButtonsCustomLoadingIndicatorEnabled = isChecked
                )
            )
        }

        // 22 enable screen's custom loading indicator
        binding.layoutAnalysisToggles.switchScreenCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isScreenCustomLoadingIndicatorEnabled = isChecked
                )
            )
        }

        // 23 enable supported format help screen
        binding.layoutHelpToggles.switchSupportedFormatsScreen.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isSupportedFormatsHelpScreenEnabled = isChecked
                )
            )
        }

        // 24 enable custom help items
        binding.layoutHelpToggles.switchCustomHelpMenuItems.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomHelpItemsEnabled = isChecked
                )
            )
        }

        // 25 enable custom navigation bar
        binding.layoutGeneralUiCustomizationToggles.switchCustomNavigationController.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomNavBarEnabled = isChecked
                )
            )
        }

        // 26 enable event tracker
        binding.layoutFeatureToggle.switchEventTracker.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isEventTrackerEnabled = isChecked
                )
            )
        }

        // 27 enable Gini error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchGiniErrorLogger.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isGiniErrorLoggerEnabled = isChecked
                )
            )
        }

        // 28 enable custom error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchCustomErrorLogger.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomErrorLoggerEnabled = isChecked
                )
            )
        }

        // 29 set imported file size bytes limit
        binding.layoutDebugDevelopmentOptionsToggles.editTextImportedFileSizeBytesLimit.doAfterTextChanged {
            if (it.toString().isNotEmpty()) {
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        importedFileSizeBytesLimit = it.toString().toInt()
                    )
                )
            }
        }

        binding.layoutDebugDevelopmentOptionsToggles.editTextClientId.doAfterTextChanged {
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    clientId = it.toString()
                )
            )
            if (it.toString()
                    .isNotEmpty() && binding.layoutDebugDevelopmentOptionsToggles.editTextClientSecret.toString()
                    .isNotEmpty()
            ) {
                applyClientSecretAndClientId()
            }
        }
        binding.layoutDebugDevelopmentOptionsToggles.editTextClientSecret.doAfterTextChanged {
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    clientSecret = it.toString()
                )
            )
            if (it.toString()
                    .isNotEmpty() && binding.layoutDebugDevelopmentOptionsToggles.editTextClientId.toString()
                    .isNotEmpty()
            ) {
                applyClientSecretAndClientId()
            }
        }

        // 31 enable return assistant
        binding.layoutFeatureToggle.switchReturnAssistantFeature.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isReturnAssistantEnabled = isChecked
                )
            )
        }

        // 32 enable return reasons dialog
        binding.layoutReturnAssistantToggles.switchReturnReasonsDialog.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isReturnReasonsEnabled = isChecked
                )
            )
        }

        // 33 Digital invoice onboarding custom illustration
        binding.layoutReturnAssistantToggles.switchDigitalInvoiceOnboardingCustomIllustration.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isDigitalInvoiceOnboardingCustomIllustrationEnabled = isChecked
                )
            )
        }

        // 34 Digital invoice help bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceHelpBottomNavigationBar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isDigitalInvoiceHelpBottomNavigationBarEnabled = isChecked
                )
            )
        }

        // 35 Digital invoice onboarding bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceOnboardingBottomNavigationBar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isDigitalInvoiceOnboardingBottomNavigationBarEnabled = isChecked
                )
            )
        }

        // 36 Digital invoice bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceBottomNavigationBar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isDigitalInvoiceBottomNavigationBarEnabled = isChecked
                )
            )
        }

        // Allow screenshots
        binding.layoutDebugDevelopmentOptionsToggles.switchAllowScreenshots.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isAllowScreenshotsEnabled = isChecked
                )
            )
        }

        // 37 Debug mode
        binding.layoutDebugDevelopmentOptionsToggles.switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isDebugModeEnabled = isChecked
                )
            )
        }

        // 40 enable Skonto
        binding.layoutFeatureToggle.switchSkontoFeature.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isSkontoEnabled = isChecked
                )
            )
        }

        // 43 enable transaction docs
        binding.layoutFeatureToggle.switchTransactionDocsFeature.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isTransactionDocsEnabled = isChecked
                )
            )
        }
    }

    private fun applyClientSecretAndClientId() {
        val configurationFlow = configurationViewModel.configurationFlow.value
        defaultNetworkServicesProvider.reinitNetworkServices(
            configurationFlow.clientId,
            configurationFlow.clientSecret
        )
    }

}