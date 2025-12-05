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
import net.gini.android.bank.sdk.exampleapp.ui.data.ExampleAppBankConfiguration
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed
import net.gini.android.capture.util.SharedPreferenceHelper
import net.gini.android.capture.util.SharedPreferenceHelper.SAF_STORAGE_URI_KEY
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

    private fun updateUIWithConfigurationObject(configuration: ExampleAppBankConfiguration) {
        // setup sdk with default configuration
        binding.layoutFeatureToggle.switchSetupSdkWithDefaultConfiguration.isChecked =
            configuration.isDefaultSDKConfigurationsEnabled
        // file import
        binding.layoutFeatureToggle.switchOpenWith.isChecked = configuration.isFileImportEnabled
        // Capture SDK
        binding.layoutFeatureToggle.switchCaptureSdk.isChecked = configuration.isCaptureSDK
        // Saving Invoices Locally
        binding.layoutFeatureToggle.switchSaveInvoicesLocallyFeature.isChecked = configuration.saveInvoicesLocallyEnabled
        // QR code scanning
        binding.layoutFeatureToggle.switchQrCodeScanning.isChecked = configuration.isQrCodeEnabled
        // only QR code scanning
        binding.layoutFeatureToggle.switchOnlyQRCodeScanning.isChecked =
            configuration.isOnlyQrCodeEnabled

        // enable multi page
        binding.layoutFeatureToggle.switchMultiPage.isChecked = configuration.isMultiPageEnabled
        // enable flash toggle
        binding.layoutCameraToggles.switchDisplayFlashButton.isChecked =
            configuration.isFlashButtonDisplayed
        // enable flash on by default
        binding.layoutCameraToggles.switchFlashOnByDefault.isChecked =
            configuration.isFlashDefaultStateEnabled
        // set import document type support
        val checkButtonId = when (configuration.documentImportEnabledFileTypes) {
            DocumentImportEnabledFileTypes.NONE -> R.id.btn_fileImportDisabled
            DocumentImportEnabledFileTypes.PDF -> R.id.btn_fileImportOnlyPdf
            DocumentImportEnabledFileTypes.PDF_AND_IMAGES -> R.id.btn_fileImportPdfAndImage
            else -> R.id.btn_fileImportOnlyPdf
        }
        binding.layoutFeatureToggle.toggleBtnFileImportSetup.check(checkButtonId)
        // enable bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowBottomNavbar.isChecked =
            configuration.isBottomNavigationBarEnabled
        // enable Help screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowHelpScreenCustomBottomNavbar.isChecked =
            configuration.isHelpScreensCustomBottomNavBarEnabled
        // enable Error screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowErrorScreenCustomBottomNavbar.isChecked =
            configuration.isErrorScreensCustomBottomNavBarEnabled
        // enable camera screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchCameraScreenCustomBottomNavbar.isChecked =
            configuration.isCameraBottomNavBarEnabled
        // enable review screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchReviewScreenCustomBottomNavbar.isChecked =
            configuration.isReviewScreenCustomBottomNavBarEnabled
        // enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoCustomBottomNavbar.isChecked =
            configuration.isSkontoCustomNavBarEnabled
        // enable skonto help screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoHelpCustomBottomNavbar.isChecked =
            configuration.isSkontoHelpCustomNavBarEnabled

        // enable digital invoice skonto screen custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceSkontoCustomBottomNavbar.isChecked =
            configuration.isDigitalInvoiceSkontoCustomNavBarEnabled

        // enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android

        // enable onboarding screens at first launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtFirstRun.isChecked =
            configuration.isOnboardingAtFirstRunEnabled
        // enable onboarding at every launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtEveryLaunch.isChecked =
            configuration.isOnboardingAtEveryLaunchEnabled
        // enable custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingPages.isChecked =
            configuration.isCustomOnboardingPagesEnabled
        // enable align corners onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingAlignCornersPage.isChecked =
            configuration.isAlignCornersInCustomOnboardingEnabled
        // enable lighting in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingLightingPage.isChecked =
            configuration.isLightingInCustomOnboardingEnabled
        // enable QR code in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingQRCodePage.isChecked =
            configuration.isQRCodeInCustomOnboardingEnabled
        // enable multi page in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingMultiPage.isChecked =
            configuration.isMultiPageInCustomOnboardingEnabled
        // enable custom navigation bar in custom onboarding pages
        binding.layoutBottomNavigationToggles.switchOnboardingCustomNavBar.isChecked =
            configuration.isCustomNavigationBarInCustomOnboardingEnabled
        // enable button's custom loading indicator
        binding.layoutGeneralUiCustomizationToggles.switchButtonsCustomLoadingIndicator.isChecked =
            configuration.isButtonsCustomLoadingIndicatorEnabled
        // enable screen's custom loading indicator
        binding.layoutAnalysisToggles.switchScreenCustomLoadingIndicator.isChecked =
            configuration.isScreenCustomLoadingIndicatorEnabled
        // enable supported format help screen
        binding.layoutHelpToggles.switchSupportedFormatsScreen.isChecked =
            configuration.isSupportedFormatsHelpScreenEnabled
        // enable custom help items
        binding.layoutHelpToggles.switchCustomHelpMenuItems.isChecked =
            configuration.isCustomHelpItemsEnabled
        // enable custom navigation bar
        binding.layoutGeneralUiCustomizationToggles.switchCustomNavigationController.isChecked =
            configuration.isCustomNavBarEnabled
        // enable custom primary button in compose
        binding.layoutGeneralUiCustomizationToggles.switchCustomPrimaryComposeButton.isChecked =
            configuration.isCustomPrimaryComposeButtonEnabled
        // enable event tracker
        binding.layoutFeatureToggle.switchEventTracker.isChecked =
            configuration.isEventTrackerEnabled
        // enable Gini error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchGiniErrorLogger.isChecked =
            configuration.isGiniErrorLoggerEnabled
        // enable custom error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchCustomErrorLogger.isChecked =
            configuration.isCustomErrorLoggerEnabled
        // set imported file size bytes limit
        binding.layoutDebugDevelopmentOptionsToggles.editTextImportedFileSizeBytesLimit.hint =
            configuration.importedFileSizeBytesLimit.toString()

        // enable return assistant
        binding.layoutFeatureToggle.switchReturnAssistantFeature.isChecked =
            configuration.isReturnAssistantEnabled

        // enable payment hints
        binding.layoutFeatureToggle.switchSetupAlreadyPaidHintEnabled.isChecked =
            configuration.isAlreadyPaidHintEnabled

        // enable payment due hint
        binding.layoutFeatureToggle.switchPaymentDueHint.isChecked =
            configuration.isPaymentDueHintEnabled

        // set payment due hint threshold days
        binding.layoutFeatureToggle.editTextPaymentDueHintThresholdDays.hint =
            configuration.paymentDueHintThresholdDays.toString()

        // enable credit note hint
        binding.layoutFeatureToggle.switchCreditNoteHint.isChecked =
            configuration.isCreditNoteHintEnabled

        // enable return reasons dialog
        binding.layoutReturnAssistantToggles.switchReturnReasonsDialog.isChecked =
            configuration.isReturnReasonsEnabled

        // Digital invoice onboarding custom illustration
        binding.layoutReturnAssistantToggles.switchDigitalInvoiceOnboardingCustomIllustration.isChecked =
            configuration.isDigitalInvoiceOnboardingCustomIllustrationEnabled

        // Digital invoice help bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceHelpBottomNavigationBar.isChecked =
            configuration.isDigitalInvoiceHelpBottomNavigationBarEnabled

        // Digital invoice onboarding bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceOnboardingBottomNavigationBar.isChecked =
            configuration.isDigitalInvoiceOnboardingBottomNavigationBarEnabled

        // Digital invoice bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceBottomNavigationBar.isChecked =
            configuration.isDigitalInvoiceBottomNavigationBarEnabled

        // Allow screenshots
        binding.layoutDebugDevelopmentOptionsToggles.switchAllowScreenshots.isChecked =
            configuration.isAllowScreenshotsEnabled

        // Debug mode
        binding.layoutDebugDevelopmentOptionsToggles.switchDebugMode.isChecked =
            configuration.isDebugModeEnabled

        // enable skonto
        binding.layoutFeatureToggle.switchSkontoFeature.isChecked = configuration.isSkontoEnabled

        // enable transaction docs
        binding.layoutFeatureToggle.switchTransactionDocsFeature.isChecked =
            configuration.isTransactionDocsEnabled

        binding.layoutTransactionDocsToggles.switchAlwaysAttachDocs.isChecked =
            configurationViewModel.getAlwaysAttachSetting(this)

        binding.layoutDebugDevelopmentOptionsToggles.editTextClientId.hint = configuration.clientId

        binding.layoutDebugDevelopmentOptionsToggles.editTextClientSecret.hint =
            configuration.clientSecret
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun setConfigurationFeatures() {
        // setup sdk with default configuration
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

        // file import
        binding.layoutFeatureToggle.switchOpenWith.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isFileImportEnabled = isChecked
                )
            )
        }

        // Capture SDK testing
        binding.layoutFeatureToggle.switchCaptureSdk.setOnCheckedChangeListener { _ , isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCaptureSDK = isChecked
                )
            )
        }

        // QR code scanning
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
        // only QR code scanning
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
        // enable multi page
        binding.layoutFeatureToggle.switchMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isMultiPageEnabled = isChecked
                )
            )
        }
        // enable flash toggle
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
        // enable flash on by default
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
        // set import document type support
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

        // enable bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isBottomNavigationBarEnabled = isChecked
                )
            )
        }

        // enable Help screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowHelpScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isHelpScreensCustomBottomNavBarEnabled = isChecked
                )
            )
        }

        // enable Error screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles
            .switchShowErrorScreenCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isErrorScreensCustomBottomNavBarEnabled = isChecked
                    )
                )
            }

        // enable camera screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchCameraScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCameraBottomNavBarEnabled = isChecked
                )
            )
        }

        // enable review screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchReviewScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isReviewScreenCustomBottomNavBarEnabled = isChecked
                )
            )
        }

        // enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isSkontoCustomNavBarEnabled = isChecked
                )
            )
        }

        // enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoHelpCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isSkontoHelpCustomNavBarEnabled = isChecked
                    )
                )
            }

        // enable digital invoice skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceSkontoCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isDigitalInvoiceSkontoCustomNavBarEnabled = isChecked
                    )
                )
            }

        // enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android

        // enable onboarding screens at first launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtFirstRun.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnboardingAtFirstRunEnabled = isChecked
                )
            )
        }

        // enable onboarding at every launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtEveryLaunch.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isOnboardingAtEveryLaunchEnabled = isChecked
                )
            )
        }

        // enable custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingPages.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomOnboardingPagesEnabled = isChecked
                )
            )
        }
        // enable align corners onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingAlignCornersPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isAlignCornersInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // enable lighting in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingLightingPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isLightingInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // enable QR code in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingQRCodePage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isQRCodeInCustomOnboardingEnabled = isChecked
                )
            )
        }

        // enable multi page in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingMultiPage.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isMultiPageInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // enable custom navigation bar in custom onboarding pages
        binding.layoutBottomNavigationToggles.switchOnboardingCustomNavBar.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomNavigationBarInCustomOnboardingEnabled = isChecked
                )
            )
        }
        // enable button's custom loading indicator
        binding.layoutGeneralUiCustomizationToggles.switchButtonsCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isButtonsCustomLoadingIndicatorEnabled = isChecked
                )
            )
        }

        // enable screen's custom loading indicator
        binding.layoutAnalysisToggles.switchScreenCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isScreenCustomLoadingIndicatorEnabled = isChecked
                )
            )
        }

        //enable payment hints for showing warning
        binding.layoutFeatureToggle.switchSetupAlreadyPaidHintEnabled.setOnCheckedChangeListener{ _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isAlreadyPaidHintEnabled = isChecked
                )
            )
        }

        //enable payment due hint for showing warning
        binding.layoutFeatureToggle.switchPaymentDueHint.setOnCheckedChangeListener{ _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isPaymentDueHintEnabled = isChecked
                )
            )
        }

        // set payment due hint threshold days
        binding.layoutFeatureToggle.editTextPaymentDueHintThresholdDays
            .doAfterTextChanged {
                if (it.toString().isNotEmpty()) {
                    configurationViewModel.setConfiguration(
                        configurationViewModel.configurationFlow.value.copy(
                            paymentDueHintThresholdDays = it.toString().toInt()
                        )
                    )
                }
            }

        //enable credit note hint for showing warning
        binding.layoutFeatureToggle.switchCreditNoteHint.setOnCheckedChangeListener{ _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCreditNoteHintEnabled = isChecked
                )
            )
        }

        // enable supported format help screen
        binding.layoutHelpToggles.switchSupportedFormatsScreen.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isSupportedFormatsHelpScreenEnabled = isChecked
                )
            )
        }

        // enable custom help items
        binding.layoutHelpToggles.switchCustomHelpMenuItems.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isCustomHelpItemsEnabled = isChecked
                )
            )
        }

        // enable custom navigation bar
        binding.layoutGeneralUiCustomizationToggles.switchCustomNavigationController
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isCustomNavBarEnabled = isChecked
                    )
                )
            }

        // enable custom primary button in compose
        binding.layoutGeneralUiCustomizationToggles.switchCustomPrimaryComposeButton
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isCustomPrimaryComposeButtonEnabled = isChecked
                    )
                )
            }


        // enable event tracker
        binding.layoutFeatureToggle.switchEventTracker
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isEventTrackerEnabled = isChecked
                    )
                )
            }

        // for internal testing: To simulate the SAF first time experience, in which the picker
        // will be shown
        binding.layoutFeatureToggle.btnRemoveSafData.setOnClickListener {
            SharedPreferenceHelper.saveString(SAF_STORAGE_URI_KEY, "", this)
        }
        // For testing Save Invoices Locally SDK flag, this is how clients can enable/disable it
        binding.layoutFeatureToggle.switchSaveInvoicesLocallyFeature
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        saveInvoicesLocallyEnabled = isChecked
                    )
                )
            }

        // enable Gini error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchGiniErrorLogger
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isGiniErrorLoggerEnabled = isChecked
                    )
                )
            }

        // enable custom error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchCustomErrorLogger
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isCustomErrorLoggerEnabled = isChecked
                    )
                )
            }

        // set imported file size bytes limit
        binding.layoutDebugDevelopmentOptionsToggles.editTextImportedFileSizeBytesLimit
            .doAfterTextChanged {
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
            if (
                it.toString().isNotEmpty() &&
                binding.layoutDebugDevelopmentOptionsToggles.editTextClientSecret.toString()
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
            if (it.toString().isNotEmpty() &&
                binding.layoutDebugDevelopmentOptionsToggles.editTextClientId.toString()
                    .isNotEmpty()
            ) {
                applyClientSecretAndClientId()
            }
        }

        // enable return assistant
        binding.layoutFeatureToggle.switchReturnAssistantFeature
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isReturnAssistantEnabled = isChecked
                    )
                )
            }

        // enable return reasons dialog
        binding.layoutReturnAssistantToggles.switchReturnReasonsDialog
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isReturnReasonsEnabled = isChecked
                    )
                )
            }

        // Digital invoice onboarding custom illustration
        binding.layoutReturnAssistantToggles.switchDigitalInvoiceOnboardingCustomIllustration
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isDigitalInvoiceOnboardingCustomIllustrationEnabled = isChecked
                    )
                )
            }

        // Digital invoice help bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceHelpBottomNavigationBar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isDigitalInvoiceHelpBottomNavigationBarEnabled = isChecked
                    )
                )
            }

        // Digital invoice onboarding bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceOnboardingBottomNavigationBar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isDigitalInvoiceOnboardingBottomNavigationBarEnabled = isChecked
                    )
                )
            }

        // Digital invoice bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceBottomNavigationBar
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isDigitalInvoiceBottomNavigationBarEnabled = isChecked
                    )
                )
            }

        // Allow screenshots
        binding.layoutDebugDevelopmentOptionsToggles.switchAllowScreenshots
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isAllowScreenshotsEnabled = isChecked
                    )
                )
            }

        // Debug mode
        binding.layoutDebugDevelopmentOptionsToggles.switchDebugMode
            .setOnCheckedChangeListener { _, isChecked ->
                configurationViewModel.setConfiguration(
                    configurationViewModel.configurationFlow.value.copy(
                        isDebugModeEnabled = isChecked
                    )
                )
            }

        // enable Skonto
        binding.layoutFeatureToggle.switchSkontoFeature.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isSkontoEnabled = isChecked
                )
            )
        }

        // enable transaction docs
        binding.layoutFeatureToggle.switchTransactionDocsFeature.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setConfiguration(
                configurationViewModel.configurationFlow.value.copy(
                    isTransactionDocsEnabled = isChecked
                )
            )
        }

        // Transaction docs always attach checked
        binding.layoutTransactionDocsToggles.switchAlwaysAttachDocs.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setAlwaysAttachSetting(this, isChecked)
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