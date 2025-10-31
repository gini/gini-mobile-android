package net.gini.android.capture.saveinvoiceslocally

import android.net.Uri
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.document.ImageMultiPageDocumentMemoryStore
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Tests the behavior of SaveInvoicesFeatureEvaluator.
 * SaveInvoicesFeatureEvaluator should return true only for valid documents.
 *
 * Here is the definition of Valid document
 * 1. The Gini Capture SDK instance must be initialized.
 * 2. The Save Invoices Locally feature must be enabled in the Gini Capture SDK.
 * 3. The Save Invoices Locally 'feature flag' must be enabled.
 * 4. There must be at least one document in the multi-page document that
 *    was not imported via the picker or "Open with", means only documents with importMethod
 *    NONE are valid.
 * */
@RunWith(AndroidJUnit4::class)
class SaveInvoicesFeatureEvaluatorTest {

    private lateinit var  mockGiniBankConfigurationProvider : GiniBankConfigurationProvider
    private lateinit var mockGiniCapture: GiniCapture
    private lateinit var mockInternal: GiniCapture.Internal
    private lateinit var mockMemoryStore: ImageMultiPageDocumentMemoryStore
    private lateinit var mockMultiPageDocument: ImageMultiPageDocument
    private val mockGetSaveInvoicesLocallyFeatureEnabledUseCase = mockk<GetSaveInvoicesLocallyFeatureEnabledUseCase>()
    private lateinit var koinTestModule : Module

    @Before
    fun setup() {

        mockGiniBankConfigurationProvider = mockk<GiniBankConfigurationProvider>()

        koinTestModule = module {
            single { mockGiniBankConfigurationProvider }
            single { mockGetSaveInvoicesLocallyFeatureEnabledUseCase }
        }

        mockGiniCapture = mockk(relaxed = true)
        mockInternal = mockk(relaxed = true)
        mockMemoryStore = mockk(relaxed = true)
        mockMultiPageDocument = mockk(relaxed = true)

        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))

        every { mockGiniCapture.internal() } returns mockInternal
        every { mockInternal.imageMultiPageDocumentMemoryStore } returns mockMemoryStore
        every { mockMemoryStore.multiPageDocument } returns mockMultiPageDocument
        GiniCaptureHelper.setGiniCaptureInstance(mockGiniCapture)

    }

    @After
    fun tearDown() {
        unmockkAll()
        GiniCaptureHelper.setGiniCaptureInstance(null)
        CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))
    }


    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when GiniCapture is not initialized`() {

        GiniCaptureHelper.setGiniCaptureInstance(null)

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when feature is not enabled in SDK`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns false
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true
        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when feature flag is disabled`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns false 

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when documents list is empty`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true
        every { mockMultiPageDocument.documents } returns emptyList()

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when multiPageDocument is null`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true
        every { mockMemoryStore.multiPageDocument } returns null

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when all documents have null uri`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true

        val mockDoc = createMockDocument(uri = null, importMethod = Document.ImportMethod.PICKER)
        every { mockMultiPageDocument.documents } returns listOf(mockDoc)

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when all documents are from PICKER`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true

        val mockUri = mockk<Uri>()
        val mockDoc = createMockDocument(uri = mockUri, importMethod = Document.ImportMethod.PICKER)
        every { mockMultiPageDocument.documents } returns listOf(mockDoc)

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns false when all documents are from OPEN_WITH`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true

        val mockUri = mockk<Uri>()
        val mockDoc =
            createMockDocument(uri = mockUri, importMethod = Document.ImportMethod.OPEN_WITH)
        every { mockMultiPageDocument.documents } returns listOf(mockDoc)

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertFalse(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns true with mixed documents when at least one is valid`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true

        val validUri = mockk<Uri>()

        val validDoc =
            createMockDocument(uri = validUri, importMethod = Document.ImportMethod.NONE)
        val pickerDoc =
            createMockDocument(uri = validUri, importMethod = Document.ImportMethod.PICKER)
        val openWithDoc =
            createMockDocument(uri = validUri, importMethod = Document.ImportMethod.OPEN_WITH)

        every { mockMultiPageDocument.documents } returns listOf(pickerDoc, validDoc, openWithDoc)

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertTrue(result)
    }

    @Test
    fun `shouldShowSaveInvoicesLocallyView returns true when the document is valid`() {

        every { mockGiniCapture.saveInvoicesEnabled } returns true
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true

        val validUri = mockk<Uri>()
        val mockDoc = createMockDocument(uri = validUri, importMethod = Document.ImportMethod.NONE)

        every { mockMultiPageDocument.documents } returns listOf(mockDoc)

        val result = SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView()

        assertTrue(result)
    }

    @Test
    fun `shouldSaveInvoicesLocally returns true when view is visible and switch is on`() {
        val result = SaveInvoicesFeatureEvaluator.shouldSaveInvoicesLocally(
            isViewVisible = View.VISIBLE,
            isSwitchOn = true
        )

        assertTrue(result)
    }

    @Test
    fun `shouldSaveInvoicesLocally returns false when view is not visible`() {
        val result = SaveInvoicesFeatureEvaluator.shouldSaveInvoicesLocally(
            isViewVisible = View.GONE,
            isSwitchOn = true
        )

        assertFalse(result)
    }

    @Test
    fun `shouldSaveInvoicesLocally returns false when switch is off`() {
        val result = SaveInvoicesFeatureEvaluator.shouldSaveInvoicesLocally(
            isViewVisible = View.VISIBLE,
            isSwitchOn = false
        )

        assertFalse(result)
    }

    @Test
    fun `shouldSaveInvoicesLocally returns false when both conditions are not met`() {
        val result = SaveInvoicesFeatureEvaluator.shouldSaveInvoicesLocally(
            isViewVisible = View.GONE,
            isSwitchOn = false
        )

        assertFalse(result)
    }

    // Helper method to create a mock ImageDocument for testing

    private fun createMockDocument(uri: Uri?, importMethod: Document.ImportMethod): ImageDocument {
        val mockDoc = mockk<ImageDocument>(relaxed = true)
        every { mockDoc.uri } returns uri
        every { mockDoc.importMethod } returns importMethod
        return mockDoc
    }

}
