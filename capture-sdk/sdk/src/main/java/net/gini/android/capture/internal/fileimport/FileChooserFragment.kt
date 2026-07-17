package net.gini.android.capture.internal.fileimport

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.databinding.GcFragmentFileChooserBinding
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersAdapter
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersItem
import net.gini.android.capture.internal.fileimport.providerchooser.ProvidersSpanSizeLookup
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.internal.util.autoCleared
import net.gini.android.capture.internal.util.disallowScreenshots
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme

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

    private val viewModel: FileChooserViewModel by viewModels {
        FileChooserViewModelFactory(requireActivity().application, docImportEnabledFileTypes)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        observeSideEffects()
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.providerItems.collect { providerItems ->
                    showFileProviders(providerItems)
                }
            }
        }
    }

    private fun observeSideEffects() {
        // Intentionally not bound to repeatOnLifecycle: the ReturnResult side effect is emitted
        // while the fragment is being popped from the back stack and must still be delivered.
        lifecycleScope.launch {
            viewModel.sideEffects.collect { sideEffect ->
                when (sideEffect) {
                    is FileChooserSideEffect.LaunchFileChooser ->
                        chooseFileLauncher?.launch(sideEffect.intent)

                    is FileChooserSideEffect.LaunchPhotoPicker ->
                        pickMedia?.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.SingleMimeType(
                                    MimeType.IMAGE_WILDCARD.asString()
                                )
                            )
                        )

                    is FileChooserSideEffect.ReturnResult ->
                        setFragmentResult(REQUEST_KEY, Bundle().apply {
                            putParcelable(RESULT_KEY, sideEffect.result)
                        })
                }
            }
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
                viewModel.onFileChooserActivityResult(result.resultCode, result.data)
            }

        val photoPickType =
            if (FeatureConfiguration.isMultiPageEnabled()) {
                ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
            } else {
                ActivityResultContracts.PickVisualMedia()
            }

        pickMedia = registerForActivityResult(photoPickType) { activityResultUriList ->
            findNavController().popBackStack()
            viewModel.onPhotoPickerResult(activityResultUriList)
        }
    }

    private fun getGridSpanCount(): Int =
        if (ContextHelper.isTablet(requireContext())) GRID_SPAN_COUNT_TABLET else GRID_SPAN_COUNT_PHONE

    override fun onResume() {
        super.onResume()
        viewModel.updateProviders()
    }

    private fun showFileProviders(providerItems: List<ProvidersItem>) {
        (binding.gcFileProviders.layoutManager as GridLayoutManager).spanSizeLookup =
            ProvidersSpanSizeLookup(providerItems, getGridSpanCount())
        binding.gcFileProviders.adapter =
            ProvidersAdapter(requireContext(), providerItems) { item ->
                viewModel.onProviderItemClicked(item)
            }
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
        fun canChooseFiles(context: Context): Boolean =
            FileChooserViewModel.canChooseFiles(context)
    }

}

@Parcelize
sealed class FileChooserResult : Parcelable {
    data class FilesSelected(val dataIntent: Intent) : FileChooserResult()
    data class FilesSelectedUri(val list: List<Uri>) : FileChooserResult()
    data class Error(val error: GiniCaptureError) : FileChooserResult()
    object Cancelled : FileChooserResult()
}
