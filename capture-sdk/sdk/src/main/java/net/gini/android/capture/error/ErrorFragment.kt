package net.gini.android.capture.error

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.launch
import net.gini.android.capture.Document
import net.gini.android.capture.EnterManuallyButtonListener
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.error.view.ErrorNavigationBarBottomAdapter
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.ui.setIntervalClickListener
import net.gini.android.capture.internal.util.AlertDialogHelperCompat
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.InjectedViewContainer
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Internal use only.
 */

/**
 * <p>
 * Include the {@code ErrorFragmentCompat} into your layout by using the {@link
 * ErrorFragmentCompat#createInstance(Document)} factory method to create an instance and
 * display it using the {@link androidx.fragment.app.FragmentManager}.
 * </p>
 * <p>
 * Your Activity must implement the {@link ImageRetakeOptionsListener} interface to receive events
 * from the Error Fragment. Failing to do so will throw an exception.
 * </p>
 * <p>
 * Your Activity is automatically set as the listener in {@link ErrorFragmentCompat#onCreate(Bundle)}.
 * </p>
 */
class ErrorFragment : Fragment(), FragmentImplCallback {

    private lateinit var listener: EnterManuallyButtonListener

    // CancelListener should be removed in the next major version - not a breaking change but better to keep it for now
    @Suppress("UnusedPrivateProperty")
    private lateinit var cancelListener: CancelListener

    private var enterManuallyButtonListener: EnterManuallyButtonListener? = null

    private val viewModel: ErrorViewModel by viewModels {
        ErrorViewModelFactory(
            requireActivity().application,
            arguments?.getParcelable(ARGS_DOCUMENT),
            arguments?.getSerializable(ARGS_ERROR) as? ErrorType,
            arguments?.getString(ARGS_CUSTOM_ERROR)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterManuallyButtonListener = listener
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.gc_fragment_error, container, false)
        handleOnBackPressed()
        viewModel.onScreenShown()

        setupTopBarNavigation(view)
        setupBottomBarNavigation(view)
        renderState(view, viewModel.uiState.value)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sideEffects.collect { sideEffect ->
                    when (sideEffect) {
                        is ErrorSideEffect.NavigateToCamera ->
                            findNavController().navigate(ErrorFragmentDirections.toCameraFragment())

                        is ErrorSideEffect.EnterManually ->
                            enterManuallyButtonListener?.onEnterManuallyPressed()
                    }
                }
            }
        }
    }

    private fun renderState(view: View, uiState: ErrorUiState) {
        val retakeImagesButton = view.findViewById<Button>(R.id.gc_button_error_retake_images)
        if (uiState.allowRetakeImages) {
            if (uiState.useBackToCameraButtonText) {
                retakeImagesButton.text = getString(R.string.gc_error_back_to_camera)
            }
            retakeImagesButton.setIntervalClickListener {
                viewModel.onRetakeImagesClicked()
            }
        } else {
            retakeImagesButton.visibility = View.GONE
        }

        val enterManuallyButton = view.findViewById<View>(R.id.gc_button_error_enter_manually)
        enterManuallyButton.setIntervalClickListener {
            viewModel.onEnterManuallyClicked()
        }

        uiState.customError?.let {
            view.findViewById<TextView>(R.id.gc_error_header).text = it
        }

        uiState.errorType?.let {
            view.findViewById<TextView>(R.id.gc_error_header).text =
                activity?.getString(it.titleTextResource)
            view.findViewById<TextView>(R.id.gc_error_textview).text =
                activity?.getString(it.descriptionTextResource)
            view.findViewById<ImageView>(R.id.gc_error_header_icon)
                .setImageResource(it.drawableResource)
        }
    }

    private fun setupTopBarNavigation(view: View) {
        val topBarContainer =
            view.findViewById<InjectedViewContainer<NavigationBarTopAdapter>>(R.id.gc_injected_navigation_bar_container_top)
        if (GiniCapture.hasInstance()) {
            topBarContainer.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(
                    GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
                ) { injectedViewAdapter ->
                    val navType = if (GiniCapture.getInstance().isBottomNavigationBarEnabled)
                        NavButtonType.NONE else NavButtonType.BACK
                    injectedViewAdapter.setNavButtonType(navType)
                    injectedViewAdapter.setTitle(
                        activity?.getString(R.string.gc_title_error) ?: ""
                    )
                    injectedViewAdapter.setOnNavButtonClickListener(IntervalClickListener {
                        viewModel.onCloseClicked()
                    })
                }
        }
    }

    private fun setupBottomBarNavigation(view: View) {
        val topBarContainer =
            view.findViewById<InjectedViewContainer<ErrorNavigationBarBottomAdapter>>(
                R.id.gc_injected_navigation_bar_container_bottom
            )

        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            topBarContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().errorNavigationBarBottomAdapterInstance
            ) { injectedViewAdapter ->
                injectedViewAdapter.setOnBackClickListener(IntervalClickListener {
                    viewModel.onCloseClicked()
                })
            }
        }
    }

    private fun handleOnBackPressed() {
        activity?.onBackPressedDispatcher
            ?.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        viewModel.onCloseClicked()
                    }
                })
    }

    fun setListener(
        listener: EnterManuallyButtonListener,
    ) {
        this.listener = listener
    }

    fun setCancelListener(
        cancelListener: CancelListener
    ) {
        this.cancelListener = cancelListener
    }

    override fun showAlertDialog(
        message: String,
        positiveButtonTitle: String,
        positiveButtonClickListener: DialogInterface.OnClickListener,
        negativeButtonTitle: String?,
        negativeButtonClickListener: DialogInterface.OnClickListener?,
        cancelListener: DialogInterface.OnCancelListener?
    ) {
        activity?.let {
            AlertDialogHelperCompat.showAlertDialog(
                it,
                message,
                positiveButtonTitle,
                positiveButtonClickListener,
                negativeButtonTitle,
                negativeButtonClickListener,
                cancelListener
            )
        }
    }

    override fun findNavController(): NavController {
        return NavHostFragment.findNavController(this)
    }

    companion object {
        private const val ARGS_ERROR = "GC_ARGS_ERROR"
        private const val ARGS_DOCUMENT = "ARGS_DOCUMENT"
        private const val ARGS_CUSTOM_ERROR = "ARGS_CUSTOM_ERROR"


        fun navigateToErrorFragment(
            navController: NavController,
            direction: NavDirections
        ) {
            if (navController.currentDestination?.id == R.id.gc_destination_error_fragment) {
                return
            }
            navController.navigate(direction)
        }


    }


}
