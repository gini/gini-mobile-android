package net.gini.android.capture.review.multipage

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.tabs.TabLayout
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.mockk.every
import io.mockk.mockk
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.camera.CameraFragment
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.saveinvoiceslocally.GetSaveInvoicesLocallyFeatureEnabledUseCase
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/**
 * Runs the real [MultiPageReviewFragment] under Robolectric with an actual multi-page document
 * and checks how it handles its pages: the initial page, deleting pages, and the navigation to
 * the camera and the zoom screen.
 */
@RunWith(AndroidJUnit4::class)
class MultiPageReviewFragmentPagesTest {

    private val getSaveInvoicesLocallyFeatureEnabledUseCase =
        mockk<GetSaveInvoicesLocallyFeatureEnabledUseCase>()
    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
        single { getSaveInvoicesLocallyFeatureEnabledUseCase }
    }

    private lateinit var navController: NavController

    @Before
    fun setUp() {
        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))
        every { getSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true
        GiniCapture.Builder().setMultiPageEnabled(true).build()
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
        navController = mock()
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))
    }

    @Test
    fun `selects the last page initially when there are several pages`() {
        storeMultiPageDocument(pageCount = 3)

        launchReviewFragment { fragment ->
            val tabIndicator = fragment.requireView().findViewById<TabLayout>(R.id.gc_tab_indicator)

            assertThat(tabIndicator.tabCount).isEqualTo(3)
            assertThat(tabIndicator.selectedTabPosition).isEqualTo(2)
            assertThat(tabIndicator.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `hides the page indicator when there is only one page`() {
        storeMultiPageDocument(pageCount = 1)

        launchReviewFragment { fragment ->
            val tabIndicator = fragment.requireView().findViewById<TabLayout>(R.id.gc_tab_indicator)

            assertThat(tabIndicator.visibility).isEqualTo(View.INVISIBLE)
        }
    }

    @Test
    fun `keeps the selected page in sync when scrolling settles on another page`() {
        storeMultiPageDocument(pageCount = 3)

        launchReviewFragment { fragment ->
            val view = fragment.requireView()
            val recyclerView = view.findViewById<RecyclerView>(R.id.gc_pager_recycler_view)
            val tabIndicator = view.findViewById<TabLayout>(R.id.gc_tab_indicator)

            recyclerView.smoothScrollToPosition(0)
            idleWithDelayedTasks()

            assertThat(tabIndicator.selectedTabPosition).isEqualTo(0)
        }
    }

    @Test
    fun `removes a deleted page and keeps the remaining ones`() {
        val multiPageDocument = storeMultiPageDocument(pageCount = 3)

        launchReviewFragment { fragment ->
            fragment.onDeleteDocument(multiPageDocument.documents[1])
            idleWithDelayedTasks()

            val tabIndicator = fragment.requireView().findViewById<TabLayout>(R.id.gc_tab_indicator)
            assertThat(multiPageDocument.documents).hasSize(2)
            assertThat(tabIndicator.tabCount).isEqualTo(2)
        }
    }

    @Test
    fun `goes back to the camera for a first page when the only page was deleted`() {
        val multiPageDocument = storeMultiPageDocument(pageCount = 1)

        launchReviewFragment { fragment ->
            fragment.onDeleteDocument(multiPageDocument.documents[0])

            assertThat(multiPageDocument.documents).isEmpty()
            assertThat(
                GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.multiPageDocument
            ).isNull()
            verify(navController).navigate(MultiPageReviewFragmentDirections.toCameraFragmentForFirstPage())
        }
    }

    @Test
    fun `opens the camera for adding pages when the add pages button is clicked`() {
        storeMultiPageDocument(pageCount = 2)

        launchReviewFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.gc_add_page_button).performClick()

            // The generated directions cannot be compared with equals() while their argument is unset
            val expectedActionId = MultiPageReviewFragmentDirections.toCameraFragmentForAddingPages().actionId
            verify(navController).navigate(argThat<NavDirections> { actionId == expectedActionId })
        }
    }

    @Test
    fun `opens the zoom screen when a page is clicked`() {
        val multiPageDocument = storeMultiPageDocument(pageCount = 2)
        val document = multiPageDocument.documents[0]

        launchReviewFragment { fragment ->
            fragment.onPageClicked(document)

            verify(navController).navigate(MultiPageReviewFragmentDirections.toZoomInPreviewFragment(document))
        }
    }

    @Test
    fun `goes back when the top bar close button is clicked`() {
        storeMultiPageDocument(pageCount = 2)

        launchReviewFragment { fragment ->
            val toolbar = fragment.requireView().findViewById<Toolbar>(R.id.gc_navigation_bar)

            toolbar.children.filterIsInstance<ImageButton>().first().performClick()

            verify(navController).popBackStack()
        }
    }

    @Test
    fun `keeps showing the pages after the camera reported that the last page should not be scrolled to`() {
        storeMultiPageDocument(pageCount = 3)

        launchReviewFragment { fragment ->
            fragment.parentFragmentManager.setFragmentResult(
                CameraFragment.REQUEST_KEY,
                Bundle().apply { putBoolean(CameraFragment.RESULT_KEY_SHOULD_SCROLL_TO_LAST_PAGE, false) }
            )
            idleWithDelayedTasks()

            val tabIndicator = fragment.requireView().findViewById<TabLayout>(R.id.gc_tab_indicator)
            assertThat(tabIndicator.tabCount).isEqualTo(3)
        }
    }

    @Test
    fun `clears the pages when the screen is left without proceeding`() {
        GiniCapture.Builder()
            .setMultiPageEnabled(true)
            .setGiniCaptureNetworkService(mockk<GiniCaptureNetworkService>(relaxed = true))
            .build()
        storeMultiPageDocument(pageCount = 2)

        launchReviewFragment { }

        assertThat(
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.multiPageDocument
        ).isNull()
    }

    @Test
    fun `keeps the pages when the screen is left to analyze them`() {
        val multiPageDocument = storeMultiPageDocument(pageCount = 2)

        launchReviewFragment { fragment ->
            fragment.onNextButtonClicked()
        }

        assertThat(
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.multiPageDocument
        ).isSameInstanceAs(multiPageDocument)
    }

    private fun storeMultiPageDocument(pageCount: Int): ImageMultiPageDocument {
        val multiPageDocument = ImageMultiPageDocument(
            Document.Source.newCameraSource(), Document.ImportMethod.NONE
        )
        repeat(pageCount) { multiPageDocument.addDocument(ImageDocumentFake() as ImageDocument) }
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
            .setMultiPageDocument(multiPageDocument)
        return multiPageDocument
    }

    private fun launchReviewFragment(block: (MultiPageReviewFragment) -> Unit) {
        FragmentScenario.launchInContainer(
            fragmentClass = MultiPageReviewFragment::class.java,
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
                    MultiPageReviewFragment().apply {
                        setCancelListener(mock())
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                Navigation.setViewNavController(fragment.requireView(), navController)
                            }
                        }
                    }
            }
        ).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            idleWithDelayedTasks()
            scenario.onFragment(block)
        }
    }

    /**
     * The fragment attaches its scroll listener and highlights the page with a delay.
     */
    private fun idleWithDelayedTasks() {
        ShadowLooper.idleMainLooper(DELAYED_TASKS_MILLIS, TimeUnit.MILLISECONDS)
    }

    private companion object {
        const val DELAYED_TASKS_MILLIS = 1_000L
    }
}
