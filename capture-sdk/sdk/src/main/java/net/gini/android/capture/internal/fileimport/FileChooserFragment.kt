package net.gini.android.capture.internal.fileimport

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionManager
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcFragmentFileChooserBinding
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAdapter
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAppItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSectionItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSeparatorItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSpanSizeLookup
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.internal.util.autoCleared

private const val ARG_DOCUMENT_IMPORT_FILE_TYPES = "GC_EXTRA_IN_DOCUMENT_IMPORT_FILE_TYPES"
private const val GRID_SPAN_COUNT_PHONE = 3
private const val GRID_SPAN_COUNT_TABLET = 6
private const val ANIM_DURATION = 200L
private const val SHOW_ANIM_DELAY = 300L
private const val REQ_CODE_CHOOSE_FILE = 1

/**
 * Internal use only.
 */
class FileChooserFragment : Fragment() {
    private var docImportEnabledFileTypes: DocumentImportEnabledFileTypes? = null
    private var binding: GcFragmentFileChooserBinding by autoCleared()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            docImportEnabledFileTypes =
                it.getSerializable(ARG_DOCUMENT_IMPORT_FILE_TYPES, DocumentImportEnabledFileTypes::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GcFragmentFileChooserBinding.inflate(inflater)
        setInputHandlers()
        setupFileProvidersView()
        handleOnBackPressed()
        return binding.root
    }

    private fun handleOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.gcFileProviders.tag == null) {
                    return
                }
                val isShown = binding.gcFileProviders.tag as Boolean
                if (!isShown) {
                    return
                }
                hideFileProviders(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        isEnabled = false
                        setFragmentResult(REQUEST_KEY, Bundle().apply {
                            putParcelable(RESULT_KEY, FileChooserResult.Cancelled)
                        })
                    }
                })
            }
        })
    }

    private fun setInputHandlers() {
        binding.root.setOnClickListener {
            val isShown = binding.gcFileProviders.tag as? Boolean
            if (isShown != null && isShown) {
                hideFileProviders(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        setFragmentResult(REQUEST_KEY, Bundle().apply {
                            putParcelable(RESULT_KEY, FileChooserResult.Cancelled)
                        })
                    }
                })
            }
        }
    }

    private fun setupFileProvidersView() {
        binding.gcFileProviders.layoutManager = GridLayoutManager(requireContext(), getGridSpanCount())
    }

    private fun getGridSpanCount(): Int =
        if (ContextHelper.isTablet(requireContext())) GRID_SPAN_COUNT_TABLET else GRID_SPAN_COUNT_PHONE

    override fun onResume() {
        super.onResume()
        populateFileProviders()
        showFileProviders()
    }

    private fun populateFileProviders() {
        val providerItems: MutableList<ProvidersItem> = ArrayList()
        var imageProviderItems: List<ProvidersItem> = ArrayList()
        var pdfProviderItems: List<ProvidersItem> = ArrayList()
        if (shouldShowImageProviders()) {
            val imagePickerResolveInfos = queryImagePickers(requireContext())
            val imageProviderResolveInfos = queryImageProviders(requireContext())
            imageProviderItems = getImageProviderItems(
                imagePickerResolveInfos,
                imageProviderResolveInfos
            )
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
        binding.gcFileProviders.adapter = ProvidersAdapter(requireContext(), providerItems) { item -> launchApp(item) }
    }

    private fun launchApp(item: ProvidersAppItem) {
        item.intent.setClassName(
            item.resolveInfo.activityInfo.packageName,
            item.resolveInfo.activityInfo.name
        )
        startActivityForResult(item.intent, REQ_CODE_CHOOSE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_CHOOSE_FILE) {
            when (resultCode) {
                RESULT_OK -> {
                    if (data != null) {
                        setFragmentResult(REQUEST_KEY, Bundle().apply {
                            putParcelable(RESULT_KEY, FileChooserResult.FilesSelected(data))
                        })
                    } else {
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
        } else {
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putParcelable(
                    RESULT_KEY, FileChooserResult.Error(
                        GiniCaptureError(
                            GiniCaptureError.ErrorCode.DOCUMENT_IMPORT,
                            "Unexpected request code for activity result."
                        )
                    )
                )
            })
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

    private fun getPdfProviderItems(pdfProviderResolveInfos: List<ResolveInfo>): List<ProvidersItem> =
        mutableListOf<ProvidersItem>().apply {
            if (pdfProviderResolveInfos.isNotEmpty()) {
                add(ProvidersSectionItem(getString(R.string.gc_file_chooser_pdfs_section_header)))

                val getPdfDocumentIntent = createGetPdfDocumentIntent()
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

    private fun showFileProviders() {
        binding.root.postDelayed({
            val backgroundTransition = AutoTransition()
            backgroundTransition.duration = ANIM_DURATION
            backgroundTransition.addListener(object : TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: Transition) {
                    val bottomSheetTransition = AutoTransition()
                    bottomSheetTransition.duration = ANIM_DURATION
                    TransitionManager.beginDelayedTransition(binding.root, bottomSheetTransition)

                    val layoutParams = binding.gcFileProviders.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.addRule(RelativeLayout.BELOW)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

                    binding.gcFileProviders.layoutParams = layoutParams
                    binding.gcFileProviders.tag = true
                }
            })
            TransitionManager.beginDelayedTransition(binding.root, backgroundTransition)

            binding.gcBackground.visibility = View.VISIBLE
        }, SHOW_ANIM_DELAY)
    }

    private fun hideFileProviders(transitionListener: Transition.TransitionListener) {
        val bottomSheetTransition = AutoTransition()
        bottomSheetTransition.duration = ANIM_DURATION
        bottomSheetTransition.addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                val backgroundTransition = AutoTransition()
                backgroundTransition.duration = ANIM_DURATION
                backgroundTransition.addListener(transitionListener)
                TransitionManager.beginDelayedTransition(binding.root, backgroundTransition)

                binding.gcBackground.visibility = View.INVISIBLE
            }
        })
        TransitionManager.beginDelayedTransition(binding.root, bottomSheetTransition)

        val layoutParams = binding.gcFileProviders.layoutParams as RelativeLayout.LayoutParams
        layoutParams.addRule(RelativeLayout.BELOW, R.id.gc_space)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        binding.gcFileProviders.layoutParams = layoutParams
        binding.gcFileProviders.tag = false
    }

    companion object {
        const val REQUEST_KEY = "GC_FILE_CHOOSER_REQUEST_KEY"
        const val RESULT_KEY = "GC_FILE_CHOOSER_RESULT_BUNDLE_KEY"

        @JvmStatic
        fun newInstance(docImportEnabledFileTypes: DocumentImportEnabledFileTypes) =
            FileChooserFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DOCUMENT_IMPORT_FILE_TYPES, docImportEnabledFileTypes)
                }
            }

        @JvmStatic
        private fun queryImagePickers(context: Context): List<ResolveInfo> {
            val intent = createImagePickerIntent()
            return context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
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
            return (!imagePickerResolveInfos.isEmpty()
                    || !imageProviderResolveInfos.isEmpty()
                    || !pdfProviderResolveInfos.isEmpty())
        }

        @SuppressLint(
            "QueryPermissionsNeeded",
            "SDK documentation informs clients to declare the <queries> element in their manifest"
        )
        private fun queryImageProviders(context: Context): List<ResolveInfo> {
            val intent = createGetImageDocumentIntent()
            return context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
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
            val intent = createGetPdfDocumentIntent()
            return context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        }

        private fun createGetPdfDocumentIntent(): Intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = MimeType.APPLICATION_PDF.asString()
                if (FeatureConfiguration.isMultiPageEnabled()) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }
    }

}

@Parcelize
sealed class FileChooserResult : Parcelable {
    data class FilesSelected(val dataIntent: Intent) : FileChooserResult()
    data class Error(val error: GiniCaptureError) : FileChooserResult()
    object Cancelled : FileChooserResult()
}