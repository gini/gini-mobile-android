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
        // setup sdk with default configuration
        binding.layoutFeatureToggle.switchSetupSdkWithDefaultConfiguration.isChecked =
            configuration.isDefaultSDKConfigurationsEnabled
        // file import
        binding.layoutFeatureToggle.switchOpenWith.isChecked = configuration.isFileImportEnabled
        // Capture SDK
        binding.layoutFeatureToggle.switchCaptureSdk.isChecked = configuration.isCaptureSDK
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


    private fun ActivityConfigurationBinding.configureDefaultSetup() {
        layoutFeatureToggle.switchSetupSdkWithDefaultConfiguration.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isDefaultSDKConfigurationsEnabled = isChecked) }
            if (isChecked) configurationViewModel.setupSDKWithDefaultConfigurations()
        }
    }

    private fun ActivityConfigurationBinding.configureFileImport() {
        binding.layoutFeatureToggle.switchOpenWith.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isFileImportEnabled = isChecked) }
        }
    }

    private fun ActivityConfigurationBinding.configureCaptureAndQr() {
        // Capture SDK testing
        binding.layoutFeatureToggle.switchCaptureSdk.setOnCheckedChangeListener { _, isChecked ->
            updateConfig {
                it.copy(
                    isCaptureSDK = isChecked
                )
            }
        }

        // QR code scanning
        binding.layoutFeatureToggle.switchQrCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            updateConfig {
                it.copy(
                    isQrCodeEnabled = isChecked
                )
            }
            if (!isChecked) {
                updateConfig {
                    it.copy(
                        isOnlyQrCodeEnabled = false
                    )
                }
            }

        }
        // only QR code scanning
        binding.layoutFeatureToggle.switchOnlyQRCodeScanning.setOnCheckedChangeListener { _, isChecked ->
            updateConfig {
                it.copy(
                    isOnlyQrCodeEnabled = isChecked
                )
            }
            if (isChecked) {
                updateConfig {
                    it.copy(
                        isQrCodeEnabled = true
                    )
                }
            }
        }
    }

    private fun ActivityConfigurationBinding.configureMultiPage() {
        binding.layoutFeatureToggle.switchMultiPage.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isMultiPageEnabled = isChecked) }
        }
    }

    private fun ActivityConfigurationBinding.configureFlash() {
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
    }

    private fun ActivityConfigurationBinding.configureFileImportSetup(){
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
    }

    private fun ActivityConfigurationBinding.configureNavigationBar() {
        // enable bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isBottomNavigationBarEnabled = isChecked) }
        }

        // enable Help screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowHelpScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isHelpScreensCustomBottomNavBarEnabled = isChecked) }
        }

        // enable Error screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchShowErrorScreenCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy(isErrorScreensCustomBottomNavBarEnabled = isChecked) }
            }

        // enable camera screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchCameraScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isCameraBottomNavBarEnabled = isChecked) }
        }

        // enable review screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchReviewScreenCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isReviewScreenCustomBottomNavBarEnabled = isChecked) }
        }

        // enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoCustomBottomNavbar.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isSkontoCustomNavBarEnabled = isChecked) }
        }

        // enable skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchSkontoHelpCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy(isSkontoHelpCustomNavBarEnabled = isChecked) }
            }

        // enable digital invoice skonto screens custom bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceSkontoCustomBottomNavbar
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy(isDigitalInvoiceSkontoCustomNavBarEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureOnBoardingScreens() {
        // enable onboarding screens at first launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtFirstRun.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isOnboardingAtFirstRunEnabled = isChecked) }
        }
        // enable onboarding at every launch
        binding.layoutOnboardingToggles.switchOnboardingScreensAtEveryLaunch.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isOnboardingAtEveryLaunchEnabled = isChecked) }
        }
        // enable custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingPages.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isCustomOnboardingPagesEnabled = isChecked) }
        }
        // enable align corners onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingAlignCornersPage.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isAlignCornersInCustomOnboardingEnabled = isChecked) }
        }
        // enable lighting in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingLightingPage.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isLightingInCustomOnboardingEnabled = isChecked) }
        }
        // enable QR code in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingQRCodePage.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isQRCodeInCustomOnboardingEnabled = isChecked) }
        }
        // enable multi page in custom onboarding pages
        binding.layoutOnboardingToggles.switchCustomOnboardingMultiPage.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isMultiPageInCustomOnboardingEnabled = isChecked) }
        }
    }
    private fun ActivityConfigurationBinding.configureSwitchOnboardingCustomNavBar(){
        binding.layoutBottomNavigationToggles.switchOnboardingCustomNavBar.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy( isCustomNavigationBarInCustomOnboardingEnabled = isChecked) }
        }
    }
    private fun ActivityConfigurationBinding.configureCustomLoadingIndicator(){
        // enable button's custom loading indicator
        binding.layoutGeneralUiCustomizationToggles.switchButtonsCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy( isButtonsCustomLoadingIndicatorEnabled = isChecked) }
        }

        // enable screen's custom loading indicator
        binding.layoutAnalysisToggles.switchScreenCustomLoadingIndicator.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy( isScreenCustomLoadingIndicatorEnabled = isChecked) }
        }
    }

    private fun ActivityConfigurationBinding.configureHelpScreen(){
        // enable supported format help screen
        binding.layoutHelpToggles.switchSupportedFormatsScreen.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy( isSupportedFormatsHelpScreenEnabled = isChecked) }
        }

        // enable custom help items
        binding.layoutHelpToggles.switchCustomHelpMenuItems.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy( isCustomHelpItemsEnabled = isChecked) }
        }
    }

    private fun ActivityConfigurationBinding.configureCustomNavigationController() {
        binding.layoutGeneralUiCustomizationToggles.switchCustomNavigationController
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isCustomNavBarEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureCustomPrimaryComposeButton(){
        binding.layoutGeneralUiCustomizationToggles.switchCustomPrimaryComposeButton
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isCustomPrimaryComposeButtonEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureEventTrackerToggles() {
        binding.layoutFeatureToggle.switchEventTracker
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isEventTrackerEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureErrorLogger(){
        // enable Gini error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchGiniErrorLogger
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isGiniErrorLoggerEnabled = isChecked) }
            }

        // enable custom error logger
        binding.layoutDebugDevelopmentOptionsToggles.switchCustomErrorLogger
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isCustomErrorLoggerEnabled = isChecked) }
            }
    }
    private fun ActivityConfigurationBinding.configureEditTextImportedFileSizeBytesLimit() {
        binding.layoutDebugDevelopmentOptionsToggles.editTextImportedFileSizeBytesLimit
            .doAfterTextChanged {
                if (it.toString().isNotEmpty()) {
                    updateConfig { config -> config.copy(  importedFileSizeBytesLimit = it.toString().toInt()) }
                }
            }
    }

    private fun ActivityConfigurationBinding.configureEditTextClientIdAndClientSecret(){

        binding.layoutDebugDevelopmentOptionsToggles.editTextClientId.doAfterTextChanged {
            updateConfig { configuration ->  configuration.copy( clientId = it.toString()) }

            if (
                it.toString().isNotEmpty() &&
                binding.layoutDebugDevelopmentOptionsToggles.editTextClientSecret.toString()
                    .isNotEmpty()
            ) {
                applyClientSecretAndClientId()
            }
        }
        binding.layoutDebugDevelopmentOptionsToggles.editTextClientSecret.doAfterTextChanged {
            updateConfig { configuration ->  configuration.copy( clientSecret = it.toString()) }

            if (it.toString().isNotEmpty() &&
                binding.layoutDebugDevelopmentOptionsToggles.editTextClientId.toString()
                    .isNotEmpty()
            ) {
                applyClientSecretAndClientId()
            }
        }
    }

    private fun ActivityConfigurationBinding.configureSwitchReturnAssistantFeature(){
        binding.layoutFeatureToggle.switchReturnAssistantFeature
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isReturnAssistantEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureSwitchReturnReasonsDialog(){
        binding.layoutReturnAssistantToggles.switchReturnReasonsDialog
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isReturnReasonsEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureDigitalInvoice(){
        // Digital invoice onboarding custom illustration
        binding.layoutReturnAssistantToggles.switchDigitalInvoiceOnboardingCustomIllustration
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isDigitalInvoiceOnboardingCustomIllustrationEnabled = isChecked) }
            }

        // Digital invoice help bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceHelpBottomNavigationBar
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isDigitalInvoiceHelpBottomNavigationBarEnabled = isChecked) }
            }

        // Digital invoice onboarding bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceOnboardingBottomNavigationBar
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isDigitalInvoiceOnboardingBottomNavigationBarEnabled = isChecked) }
            }

        // Digital invoice bottom navigation bar
        binding.layoutBottomNavigationToggles.switchDigitalInvoiceBottomNavigationBar
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isDigitalInvoiceBottomNavigationBarEnabled = isChecked) }
            }

    }

    private fun ActivityConfigurationBinding.configureSwitchAllowScreenshots(){
        binding.layoutDebugDevelopmentOptionsToggles.switchAllowScreenshots
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isAllowScreenshotsEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureSwitchDebugMode(){
        binding.layoutDebugDevelopmentOptionsToggles.switchDebugMode
            .setOnCheckedChangeListener { _, isChecked ->
                updateConfig { it.copy( isDebugModeEnabled = isChecked) }
            }
    }

    private fun ActivityConfigurationBinding.configureSkontoFeature(){
        binding.layoutFeatureToggle.switchSkontoFeature.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy( isSkontoEnabled = isChecked) }
        }
    }

    private fun ActivityConfigurationBinding.configureTransactionDocsFeature(){
        binding.layoutFeatureToggle.switchTransactionDocsFeature.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(isTransactionDocsEnabled = isChecked) }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun setConfigurationFeatures() {
        // setup sdk with default configuration
        with(binding) {
            // setup sdk with default configuration
            configureDefaultSetup()
            // file import
            configureFileImport()
            // Capture SDK and QR code
            configureCaptureAndQr()
            // enable multi page
            configureMultiPage()
            // enable flash toggle and flash on by default
            configureFlash()
            // set import document type support
            configureFileImportSetup()
            // enable bottom navigation bar and related options
            configureNavigationBar()
            // enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android
            // configure onboarding screens
            configureOnBoardingScreens()
            // enable custom navigation bar in custom onboarding pages
            configureSwitchOnboardingCustomNavBar()
            // enable custom loading indicators
            configureCustomLoadingIndicator()
            // enable help screen options
            configureHelpScreen()
            // enable custom navigation bar
            configureCustomNavigationController()
            // enable custom primary button in compose
            configureCustomPrimaryComposeButton()
            // enable event tracker
            configureEventTrackerToggles()
            // enable error logger
            configureErrorLogger()
            // set imported file size bytes limit
            configureEditTextImportedFileSizeBytesLimit()
            // set client id and client secret
            configureEditTextClientIdAndClientSecret()
            // enable return assistant
            configureSwitchReturnAssistantFeature()
            // enable return reasons dialog
            configureSwitchReturnReasonsDialog()
            // configure digital invoice options
            configureDigitalInvoice()
            // Allow screenshots
            configureSwitchAllowScreenshots()
            // Debug mode
            configureSwitchDebugMode()
            // enable Skonto
            configureSkontoFeature()
            // enable Transaction docs
            configureTransactionDocsFeature()
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

    private inline fun updateConfig(crossinline block: (Configuration) -> Configuration) {
        configurationViewModel.setConfiguration(block(configurationViewModel.configurationFlow.value))
    }

}