package net.gini.android.capture.error

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import net.gini.android.capture.Document
import net.gini.android.capture.EnterManuallyButtonListener
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.internal.util.AlertDialogHelperCompat
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.util.CancelListener

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

    private lateinit var fragmentImpl: ErrorFragmentImpl
    private lateinit var listener: EnterManuallyButtonListener
    private lateinit var cancelListener: CancelListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentImpl = createFragmentImpl(this, arguments)
        fragmentImpl.setListener(listener)
        fragmentImpl.onCreate(savedInstanceState)
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
        return fragmentImpl.onCreateView(inflater, container, savedInstanceState)
    }

    fun setListeners(
        listener: EnterManuallyButtonListener,
        cancelListener: CancelListener
    ) {
        this.listener = listener
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

    private fun createFragmentImpl(
        fragment: FragmentImplCallback,
        arguments: Bundle?
    ): ErrorFragmentImpl {
        val document = arguments?.getParcelable<Document>(ARGS_DOCUMENT)
        val error = arguments?.getSerializable(ARGS_ERROR) as? ErrorType
        val customError = arguments?.getString(ARGS_CUSTOM_ERROR)
        return ErrorFragmentImpl(fragment, cancelListener, document, error, customError)
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
