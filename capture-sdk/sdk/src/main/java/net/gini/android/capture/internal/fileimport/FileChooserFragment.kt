package net.gini.android.capture.internal.fileimport

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcFragmentFileChooserBinding
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAdapter
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAppItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAppWrapperItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSectionItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSeparatorItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSpanSizeLookup
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.internal.util.autoCleared
import net.gini.android.capture.internal.util.disallowScreenshots
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

private const val ARG_DOCUMENT_IMPORT_FILE_TYPES = "GC_EXTRA_IN_DOCUMENT_IMPORT_FILE_TYPES"
private const val GRID_SPAN_COUNT_PHONE = 3
private const val GRID_SPAN_COUNT_TABLET = 6

/**
 * Internal use only.
 */
class FileChooserFragment : BottomSheetDialogFragment() {
    private var docImportEnabledFileTypes: DocumentImportEnabledFileTypes? = null
    private var binding: GcFragmentFileChooserBinding by autoCleared()
    private var chooseFileLauncher: ActivityResultLauncher<Intent>? = null
    private var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var userAnalyticsEventTracker: UserAnalyticsEventTracker? = null
    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.Camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userAnalyticsEventTracker =
            UserAnalytics.getAnalyticsEventTracker()
        arguments?.let {
            docImportEnabledFileTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(
                    ARG_DOCUMENT_IMPORT_FILE_TYPES,
                    DocumentImportEnabledFileTypes::class.java
                )
            } else {
                it.getSerializable(ARG_DOCUMENT_IMPORT_FILE_TYPES) as? DocumentImportEnabledFileTypes
            }
        }
        setupFileChooserListener()
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GcFragmentFileChooserBinding.inflate(inflater)
        setupFileProvidersView()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val bottomSheetDialog = dialog as? BottomSheetDialog
        bottomSheetDialog?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            peekHeight = 0
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            dialog?.window?.disallowScreenshots()
        }
    }

    private fun setupFileProvidersView() {
        binding.gcFileProviders.layoutManager =
            GridLayoutManager(requireContext(), getGridSpanCount())
    }

    private fun setupFileChooserListener() {
        chooseFileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                findNavController().popBackStack()
                when (result.resultCode) {
                    RESULT_OK -> {
                        result.data?.let { data ->
                            setFragmentResult(REQUEST_KEY, Bundle().apply {
                                putParcelable(RESULT_KEY, FileChooserResult.FilesSelected(data))
                            })
                        } ?: run {
                            setFragmentResult(REQUEST_KEY, Bundle().apply {
                                putParcelable(
                                    RESULT_KEY, FileChooserResult.Error(
                                        GiniCaptureError(
                                            GiniCaptureError.ErrorCode.DOCUMENT_IMPORT,
                                            "Activity result data was null."
                                        )
                                    )
                                )
                            })
                        }
                    }

                    RESULT_CANCELED -> setFragmentResult(REQUEST_KEY, Bundle().apply {
                        putParcelable(RESULT_KEY, FileChooserResult.Cancelled)
                    })

                    else -> setFragmentResult(REQUEST_KEY, Bundle().apply {
                        putParcelable(
                            RESULT_KEY, FileChooserResult.Error(
                                GiniCaptureError(
                                    GiniCaptureError.ErrorCode.DOCUMENT_IMPORT,
                                    "Unexpected result code for activity result."
                                )
                            )
                        )
                    })
                }
            }

        val photoPickType =
            if (FeatureConfiguration.isMultiPageEnabled()) {
                ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
            } else {
                ActivityResultContracts.PickVisualMedia()
            }

        pickMedia = registerForActivityResult(photoPickType) { activityResultUriList ->
            findNavController().popBackStack()
            val resultList = handleUriList(activityResultUriList)
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putParcelable(RESULT_KEY, resultList)
            })
        }
    }

    private fun handleUriList(activityResultUriList: Any?): FileChooserResult {
        return if (activityResultUriList == null) {
            FileChooserResult.Cancelled
        } else {
            try {
                val uriList = when (activityResultUriList) {
                    is Uri -> listOf(activityResultUriList)
                    is List<*> -> {
                        activityResultUriList.filterIsInstance<Uri>().takeIf {
                            it.size == activityResultUriList.size
                        } ?: throw IllegalArgumentException("List contains non-Uri elements")
                    }
                    else -> throw IllegalArgumentException("uri is neither Uri nor List<Uri>")
                }
                if (uriList.isNotEmpty()) {
                    FileChooserResult.FilesSelectedUri(uriList)
                } else {
                    FileChooserResult.Cancelled
                }
            } catch (e: IllegalArgumentException) {
                FileChooserResult.Error(
                    GiniCaptureError(
                        GiniCaptureError.ErrorCode.DOCUMENT_IMPORT,
                        e.message
                    )
                )
            }
        }
    }

    private fun getGridSpanCount(): Int =
        if (ContextHelper.isTablet(requireContext())) GRID_SPAN_COUNT_TABLET else GRID_SPAN_COUNT_PHONE

    override fun onResume() {
        super.onResume()
        populateFileProviders()
    }

    private fun populateFileProviders() {
        val providerItems: MutableList<ProvidersItem> = ArrayList()
        var imageProviderItems: List<ProvidersItem> = arrayListOf()
        var pdfProviderItems: List<ProvidersItem> = ArrayList()
        if (shouldShowImageProviders()) {
            val imagePickerResolveInfos = queryImagePickers(requireContext())
            val imageProviderResolveInfos = queryImageProviders(requireContext())

            imageProviderItems =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(requireContext())
                ) {
                    getPhotoPickerProvider()
                } else {
                    getImageProviderItems(imagePickerResolveInfos, imageProviderResolveInfos)
                }
        }
        if (shouldShowPdfProviders()) {
            val pdfProviderResolveInfos = queryPdfProviders(requireContext())
            pdfProviderItems = getPdfProviderItems(pdfProviderResolveInfos)
        }
        providerItems.addAll(imageProviderItems)
        if (!imageProviderItems.isEmpty() && !pdfProviderItems.isEmpty()) {
            providerItems.add(ProvidersSeparatorItem())
        }
        providerItems.addAll(pdfProviderItems)
        (binding.gcFileProviders.layoutManager as GridLayoutManager).spanSizeLookup =
            ProvidersSpanSizeLookup(providerItems, getGridSpanCount())
        binding.gcFileProviders.adapter =
            ProvidersAdapter(requireContext(), providerItems) { item ->
                if (item in imageProviderItems) {
                    userAnalyticsEventTracker?.trackEvent(
                        UserAnalyticsEvent.UPLOAD_PHOTOS_TAPPED,
                        setOf(UserAnalyticsEventProperty.Screen(screenName))
                    )

                } else {
                    userAnalyticsEventTracker?.trackEvent(
                        UserAnalyticsEvent.UPLOAD_DOCUMENTS_TAPPED,
                        setOf(UserAnalyticsEventProperty.Screen(screenName))
                    )

                }
                launchApp(item)
            }
    }

    private fun launchApp(item: ProvidersItem) {
        when (item) {
            is ProvidersAppItem -> {
                item.intent.setClassName(
                    item.resolveInfo.activityInfo.packageName,
                    item.resolveInfo.activityInfo.name
                )
                chooseFileLauncher?.launch(item.intent)
            }

            is ProvidersAppWrapperItem -> {
                pickMedia?.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.SingleMimeType(
                            MimeType.IMAGE_WILDCARD.asString()
                        )
                    )
                )
            }
        }
    }

    private fun getImageProviderItems(
        imagePickerResolveInfos: List<ResolveInfo>,
        imageProviderResolveInfos: List<ResolveInfo>
    ): List<ProvidersItem> = mutableListOf<ProvidersItem>().apply {
        if (imagePickerResolveInfos.isNotEmpty()
            || imageProviderResolveInfos.isNotEmpty()
        ) {
            add(ProvidersSectionItem(getString(R.string.gc_file_chooser_fotos_section_header)))

            val imagePickerIntent = createImagePickerIntent()
            for (imagePickerResolveInfo in imagePickerResolveInfos) {
                add(ProvidersAppItem(imagePickerIntent, imagePickerResolveInfo))
            }

            val getImageDocumentIntent = createGetImageDocumentIntent()
            for (imageProviderResolveInfo in imageProviderResolveInfos) {
                add(ProvidersAppItem(getImageDocumentIntent, imageProviderResolveInfo))
            }
        }
    }

    private fun getPhotoPickerProvider(): List<ProvidersItem> {
        val providerList =
            ContextCompat.getDrawable(requireContext(), (R.drawable.gc_photo_picker_app_icon))
                ?.let { image ->
                    listOf(
                        ProvidersSectionItem(getString(R.string.gc_file_chooser_fotos_section_header)),
                        ProvidersAppWrapperItem(
                            image,
                            getString(R.string.gc_file_chooser_fotos_section_header)
                        )
                    )
                } ?: run {
                emptyList()
            }
        return providerList
    }

    private fun getPdfProviderItems(pdfProviderResolveInfos: List<ResolveInfo>): List<ProvidersItem> =
        mutableListOf<ProvidersItem>().apply {
            if (pdfProviderResolveInfos.isNotEmpty()) {

                val getPdfDocumentIntent = if (getEInvoiceFeatureEnabledUseCase.invoke()) {
                    add(ProvidersSectionItem(getString(R.string.gc_file_chooser_pdfs_xmls_section_header)))
                    createGetPdfAndXmlDocumentIntent()
                } else {
                    add(ProvidersSectionItem(getString(R.string.gc_file_chooser_pdfs_section_header)))
                    createGetPdfDocumentIntent()
                }
                for (pdfProviderResolveInfo in pdfProviderResolveInfos) {
                    add(ProvidersAppItem(getPdfDocumentIntent, pdfProviderResolveInfo))
                }
            }
        }

    private fun shouldShowImageProviders(): Boolean {
        return (docImportEnabledFileTypes == DocumentImportEnabledFileTypes.IMAGES
                || docImportEnabledFileTypes == DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
    }

    private fun shouldShowPdfProviders(): Boolean {
        return (docImportEnabledFileTypes == DocumentImportEnabledFileTypes.PDF
                || docImportEnabledFileTypes == DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
    }

    companion object {
        const val REQUEST_KEY = "GC_FILE_CHOOSER_REQUEST_KEY"
        const val RESULT_KEY = "GC_FILE_CHOOSER_RESULT_BUNDLE_KEY"
        private val getEInvoiceFeatureEnabledUseCase: GetEInvoiceFeatureEnabledUseCase
                by getGiniCaptureKoin().inject()

        @JvmStatic
        fun newInstance(docImportEnabledFileTypes: DocumentImportEnabledFileTypes) =
            FileChooserFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DOCUMENT_IMPORT_FILE_TYPES, docImportEnabledFileTypes)
                }
            }

        @SuppressLint(
            "QueryPermissionsNeeded",
            "SDK documentation informs clients to declare the <queries> element in their manifest"
        )
        @JvmStatic
        private fun queryImagePickers(context: Context): List<ResolveInfo> {
            val intent = createImagePickerIntent()
            return context.packageManager.queryIntentActivities(intent, 0)
        }

        private fun createImagePickerIntent(): Intent =
            Intent(Intent.ACTION_PICK).apply {
                setDataAndType(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MimeType.IMAGE_WILDCARD.asString()
                )
                if (FeatureConfiguration.isMultiPageEnabled()) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }

        @JvmStatic
        fun canChooseFiles(context: Context): Boolean {
            val imagePickerResolveInfos = queryImagePickers(context)
            val imageProviderResolveInfos = queryImageProviders(context)
            val pdfProviderResolveInfos = queryPdfProviders(context)
            return (imagePickerResolveInfos.isNotEmpty()
                    || imageProviderResolveInfos.isNotEmpty()
                    || pdfProviderResolveInfos.isNotEmpty())
        }

        @SuppressLint(
            "QueryPermissionsNeeded",
            "SDK documentation informs clients to declare the <queries> element in their manifest"
        )
        private fun queryImageProviders(context: Context): List<ResolveInfo> {
            val intent = createGetImageDocumentIntent()
            return context.packageManager.queryIntentActivities(intent, 0)
        }

        private fun createGetImageDocumentIntent(): Intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = MimeType.IMAGE_WILDCARD.asString()
                if (FeatureConfiguration.isMultiPageEnabled()) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }

        @SuppressLint(
            "QueryPermissionsNeeded",
            "SDK documentation informs clients to declare the <queries> element in their manifest"
        )
        private fun queryPdfProviders(context: Context): List<ResolveInfo> {
            val intent =
                if (getEInvoiceFeatureEnabledUseCase.invoke()) {
                    createGetPdfAndXmlDocumentIntent()
                } else {
                    createGetPdfDocumentIntent()
                }
            return context.packageManager.queryIntentActivities(intent, 0)
        }

        private fun createGetPdfDocumentIntent(): Intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = MimeType.APPLICATION_PDF.asString()
                if (FeatureConfiguration.isMultiPageEnabled()) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }

        private fun createGetPdfAndXmlDocumentIntent(): Intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("application/pdf", "text/xml", "application/xml")
                )
                if (FeatureConfiguration.isMultiPageEnabled()) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }
    }

}

@Parcelize
sealed class FileChooserResult : Parcelable {
    data class FilesSelected(val dataIntent: Intent) : FileChooserResult()
    data class FilesSelectedUri(val list: List<Uri>) : FileChooserResult()
    data class Error(val error: GiniCaptureError) : FileChooserResult()
    object Cancelled : FileChooserResult()
}
