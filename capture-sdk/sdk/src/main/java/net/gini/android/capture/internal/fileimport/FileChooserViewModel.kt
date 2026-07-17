package net.gini.android.capture.internal.fileimport

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.R
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAppItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAppWrapperItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSectionItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSeparatorItem
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

/**
 * ViewModel for the file chooser screen.
 *
 * Contains the provider list building, intent resolution decisions, grid item assembly and
 * activity result routing which used to live in [FileChooserFragment]. The fragment only renders
 * the provider items and executes the one-shot [sideEffects].
 *
 * Internal use only.
 */
internal class FileChooserViewModel(
    private val app: Application,
    private val docImportEnabledFileTypes: DocumentImportEnabledFileTypes?
) : ViewModel() {

    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.Camera

    private val _providerItems = MutableStateFlow<List<ProvidersItem>>(emptyList())

    /**
     * The items to be shown in the providers grid.
     */
    val providerItems: StateFlow<List<ProvidersItem>> = _providerItems.asStateFlow()

    private val _sideEffects = Channel<FileChooserSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<FileChooserSideEffect> = _sideEffects.receiveAsFlow()

    private var imageProviderItems: List<ProvidersItem> = emptyList()

    /**
     * Rebuilds the provider items. Must be called from the fragment's `onResume()` (the provider
     * list was populated there before the MVVM migration as well).
     */
    fun updateProviders() {
        val providerItems: MutableList<ProvidersItem> = ArrayList()
        var imageProviderItems: List<ProvidersItem> = arrayListOf()
        var pdfProviderItems: List<ProvidersItem> = ArrayList()
        if (shouldShowImageProviders()) {
            val imagePickerResolveInfos = queryImagePickers(app)
            val imageProviderResolveInfos = queryImageProviders(app)

            imageProviderItems =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(app)
                ) {
                    getPhotoPickerProvider()
                } else {
                    getImageProviderItems(imagePickerResolveInfos, imageProviderResolveInfos)
                }
        }
        if (shouldShowPdfProviders()) {
            val pdfProviderResolveInfos = queryPdfProviders(app)
            pdfProviderItems = getPdfProviderItems(pdfProviderResolveInfos)
        }
        providerItems.addAll(imageProviderItems)
        if (!imageProviderItems.isEmpty() && !pdfProviderItems.isEmpty()) {
            providerItems.add(ProvidersSeparatorItem())
        }
        providerItems.addAll(pdfProviderItems)
        this.imageProviderItems = imageProviderItems
        _providerItems.value = providerItems
    }

    fun onProviderItemClicked(item: ProvidersItem) {
        val userAnalyticsEventTracker = UserAnalytics.getAnalyticsEventTracker()
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

    private fun launchApp(item: ProvidersItem) {
        when (item) {
            is ProvidersAppItem -> {
                item.intent.setClassName(
                    item.resolveInfo.activityInfo.packageName,
                    item.resolveInfo.activityInfo.name
                )
                sendSideEffect(FileChooserSideEffect.LaunchFileChooser(item.intent))
            }

            is ProvidersAppWrapperItem -> {
                sendSideEffect(FileChooserSideEffect.LaunchPhotoPicker)
            }
        }
    }

    /**
     * Routes the result of the file chooser activity to a [FileChooserResult].
     */
    fun onFileChooserActivityResult(resultCode: Int, data: Intent?) {
        val result = when (resultCode) {
            RESULT_OK -> data?.let { FileChooserResult.FilesSelected(it) }
                ?: FileChooserResult.Error(
                    GiniCaptureError(
                        GiniCaptureError.ErrorCode.DOCUMENT_IMPORT,
                        "Activity result data was null."
                    )
                )

            RESULT_CANCELED -> FileChooserResult.Cancelled

            else -> FileChooserResult.Error(
                GiniCaptureError(
                    GiniCaptureError.ErrorCode.DOCUMENT_IMPORT,
                    "Unexpected result code for activity result."
                )
            )
        }
        sendSideEffect(FileChooserSideEffect.ReturnResult(result))
    }

    /**
     * Routes the result of the photo picker to a [FileChooserResult].
     */
    fun onPhotoPickerResult(activityResultUriList: Any?) {
        sendSideEffect(FileChooserSideEffect.ReturnResult(handleUriList(activityResultUriList)))
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

    private fun getImageProviderItems(
        imagePickerResolveInfos: List<ResolveInfo>,
        imageProviderResolveInfos: List<ResolveInfo>
    ): List<ProvidersItem> = mutableListOf<ProvidersItem>().apply {
        if (imagePickerResolveInfos.isNotEmpty()
            || imageProviderResolveInfos.isNotEmpty()
        ) {
            add(ProvidersSectionItem(app.getString(R.string.gc_file_chooser_fotos_section_header)))

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
            ContextCompat.getDrawable(app, (R.drawable.gc_photo_picker_app_icon))
                ?.let { image ->
                    listOf(
                        ProvidersSectionItem(app.getString(R.string.gc_file_chooser_fotos_section_header)),
                        ProvidersAppWrapperItem(
                            image,
                            app.getString(R.string.gc_file_chooser_fotos_section_header)
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
                    add(ProvidersSectionItem(app.getString(R.string.gc_file_chooser_pdfs_xmls_section_header)))
                    createGetPdfAndXmlDocumentIntent()
                } else {
                    add(ProvidersSectionItem(app.getString(R.string.gc_file_chooser_pdfs_section_header)))
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

    private fun sendSideEffect(sideEffect: FileChooserSideEffect) {
        viewModelScope.launch { _sideEffects.send(sideEffect) }
    }

    companion object {
        private val getEInvoiceFeatureEnabledUseCase: GetEInvoiceFeatureEnabledUseCase
                by getGiniCaptureKoin().inject()

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

/**
 * One-shot side effects executed by [FileChooserFragment].
 *
 * Internal use only.
 */
internal sealed interface FileChooserSideEffect {
    data class LaunchFileChooser(val intent: Intent) : FileChooserSideEffect
    object LaunchPhotoPicker : FileChooserSideEffect
    data class ReturnResult(val result: FileChooserResult) : FileChooserSideEffect
}
