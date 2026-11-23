package net.gini.android.capture.analysis

import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.analysis.fraud.IBANMismatchArgs
import net.gini.android.capture.analysis.fraud.IBANMismatchIban
import net.gini.android.capture.analysis.fraud.IBANMismatchType
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.internal.iban.formatIbanForSpeech
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.ui.components.GiniComposableStyleProvider
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.Shadows
import java.time.Duration

/**
 * The IBAN mismatch warning as the Analysis screen hosts it: the fragment composes the warning
 * into its own container over the screen, and each of the warning's two ways out first removes
 * the warning and then runs the presenter's action.
 */
@RunWith(AndroidJUnit4::class)
class AnalysisFragmentIbanMismatchTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var koinTestModule: Module
    private val cancelListener: CancelListener = mock()
    private val proceededWith = mutableListOf<String?>()

    @Before
    fun setUp() {
        // GiniBankConfigurationProvider is registered in the SDK's isolated Koin context by the
        // Bank SDK's DI bridge, so a capture-sdk unit test has to provide its own definition.
        koinTestModule = module { single { GiniBankConfigurationProvider() } }
        getGiniCaptureKoin().loadModules(listOf(koinTestModule))
    }

    @After
    fun tearDown() {
        // Koin's unloadModules drops the overriding definition instead of restoring the previous
        // one, and getGiniCaptureKoin() is a process-wide isolated context, so the definition is
        // re-loaded for later test classes running in the same JVM.
        getGiniCaptureKoin().unloadModules(listOf(koinTestModule))
        getGiniCaptureKoin().loadModules(
            listOf(module { single { GiniBankConfigurationProvider() } })
        )
    }

    @Test
    fun `shows the warning over the screen and hides the capture suggestions behind it`() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                // Runs the capture suggestion timer until the first tip has slid into view
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
                assertThat(hintContainer(fragment).visibility).isEqualTo(View.VISIBLE)
                assertThat(warningContainer(fragment).visibility).isEqualTo(View.GONE)

                showIbanMismatch(fragment, QR_VS_PRINTED_ARGS)

                assertThat(warningContainer(fragment).visibility).isEqualTo(View.VISIBLE)
                assertThat(hintContainer(fragment).visibility).isEqualTo(View.GONE)
            }

            warningTitle(R.string.gc_fraud_iban_mismatch_title).assertExists()
            verify(cancelListener, never()).onCancelFlow()
            assertThat(proceededWith).isEmpty()
        }
    }

    @Test
    fun `Cancel transfer removes the warning and cancels the flow`() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment -> showIbanMismatch(fragment, QR_VS_PRINTED_ARGS) }

            button(R.string.gc_cancel_transfer).click()
            composeRule.waitForIdle()

            warningTitle(R.string.gc_fraud_iban_mismatch_title).assertDoesNotExist()
            scenario.onFragment { fragment ->
                assertThat(warningContainer(fragment).visibility).isEqualTo(View.GONE)
            }
            verify(cancelListener).onCancelFlow()
            assertThat(proceededWith).isEmpty()
        }
    }

    @Test
    fun `Proceed anyway without a choice removes the warning and proceeds without an IBAN`() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment -> showIbanMismatch(fragment, COUNTRY_ARGS) }

            button(R.string.gc_proceed_anyway).click()
            composeRule.waitForIdle()

            warningTitle(R.string.gc_fraud_country_mismatch_title).assertDoesNotExist()
            scenario.onFragment { fragment ->
                assertThat(warningContainer(fragment).visibility).isEqualTo(View.GONE)
            }
            assertThat(proceededWith).containsExactly(null)
            verify(cancelListener, never()).onCancelFlow()
        }
    }

    @Test
    fun `the IBAN chosen after Proceed anyway is handed on and the warning is removed`() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment -> showIbanMismatch(fragment, QR_VS_PRINTED_ARGS) }

            button(R.string.gc_proceed_anyway).click()
            composeRule
                .onNode(hasContentDescription(formatIbanForSpeech(PRINTED_IBAN), substring = true) and isSelectable())
                .click()
            button(R.string.gc_fraud_select_iban_continue).click()
            composeRule.waitForIdle()

            warningTitle(R.string.gc_fraud_iban_mismatch_title).assertDoesNotExist()
            scenario.onFragment { fragment ->
                assertThat(warningContainer(fragment).visibility).isEqualTo(View.GONE)
            }
            assertThat(proceededWith).containsExactly(PRINTED_IBAN)
            verify(cancelListener, never()).onCancelFlow()
        }
    }

    @Test
    fun `the warning's buttons take the integrator's composable style`() {
        var styleRequests = 0
        val styleProvider = GiniComposableStyleProvider {
            styleRequests++
            GiniComposableStyleProviderConfig()
        }

        launchFragment(styleProvider).use { scenario ->
            scenario.onFragment { fragment -> showIbanMismatch(fragment, COUNTRY_ARGS) }

            warningTitle(R.string.gc_fraud_country_mismatch_title).assertExists()
            button(R.string.gc_cancel_transfer).assertExists()
            assertThat(styleRequests).isGreaterThan(0)
        }
    }

    @Test
    fun `the chosen IBAN survives recreation while the invoice is being saved`() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                assertThat(fragment.fragmentImpl.chosenIban).isNull()
                fragment.fragmentImpl.processInvoiceSaving(SAVING_ACTION, PRINTED_IBAN)
            }

            scenario.recreate()

            scenario.onFragment { fragment ->
                assertThat(fragment.fragmentImpl.chosenIban).isEqualTo(PRINTED_IBAN)
                assertThat(fragment.fragmentImpl.pendingSavingAction).isEqualTo(SAVING_ACTION)
                assertThat(fragment.fragmentImpl.isInvoiceSavingInProgress).isTrue()
            }
        }
    }

    private fun showIbanMismatch(fragment: AnalysisFragment, args: IBANMismatchArgs) {
        fragment.fragmentImpl.showIbanMismatch(args) { selectedIban -> proceededWith += selectedIban }
    }

    private fun warningContainer(fragment: AnalysisFragment): View =
        fragment.requireView().findViewById(R.id.gc_iban_mismatch_container)

    private fun hintContainer(fragment: AnalysisFragment): View =
        fragment.requireView().findViewById(R.id.gc_analysis_hint_container)

    private fun warningTitle(@StringRes id: Int): SemanticsNodeInteraction =
        composeRule.onNode(
            hasText(string(id)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        )

    private fun button(@StringRes text: Int): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(text)) and hasClickAction())

    // The sheets live in their own windows, so they are driven through the semantics action
    private fun SemanticsNodeInteraction.click(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun launchFragment(
        styleProvider: GiniComposableStyleProvider? = null,
    ): FragmentScenario<AnalysisFragment> {
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setGiniCaptureNetworkService(mock())
            .also { builder -> styleProvider?.let { builder.setGiniComposableStyleProvider(it) } }
            .build()
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
            .setMultiPageDocument(mock())
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)

        val bundle = Bundle().apply {
            putParcelable(
                "GC_ARGS_DOCUMENT",
                mock<ImageDocument>().apply {
                    whenever(isReviewable).thenReturn(true)
                    whenever(type).thenReturn(Document.Type.IMAGE)
                }
            )
            putString("GC_ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE", "")
        }
        return FragmentScenario.launchInContainer(
            fragmentClass = AnalysisFragment::class.java,
            fragmentArgs = bundle,
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return AnalysisFragment().apply {
                        setListener(mock())
                        setBankSDKBridge(mock())
                        setCancelListener(cancelListener)
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                // The fragment's view has just been created
                                Navigation.setViewNavController(fragment.requireView(), mock())
                            }
                        }
                    }
                }
            }
        )
    }

    private companion object {
        const val QR_IBAN = "PL61109010140000071219812874"
        const val PRINTED_IBAN = "DE89370400440532013000"
        const val SAVING_ACTION = "PROCEED"

        val QR_VS_PRINTED_ARGS = IBANMismatchArgs(
            type = IBANMismatchType.QR_VS_PRINTED,
            qrIban = IBANMismatchIban(QR_IBAN),
            printedIbans = listOf(IBANMismatchIban(PRINTED_IBAN)),
        )

        val COUNTRY_ARGS = IBANMismatchArgs(
            type = IBANMismatchType.COUNTRY,
            qrIban = null,
            printedIbans = listOf(IBANMismatchIban(QR_IBAN, hintCountryCode = "PL")),
            senderName = "Elektro Brandt GmbH",
            senderCountryCode = "DE",
        )
    }
}
