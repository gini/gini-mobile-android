package net.gini.android.capture.error

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.gini.android.capture.Document
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.internal.util.AlertDialogHelperCompat

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
class ErrorFragmentCompat : Fragment(), FragmentImplCallback {

    private lateinit  var fragmentImpl: ErrorFragmentImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentImpl = ErrorFragmentHelper.createFragmentImpl(this, arguments)
        activity?.let { ErrorFragmentHelper.setListener(fragmentImpl, it) }
        fragmentImpl.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return fragmentImpl.onCreateView(inflater, container, savedInstanceState)
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
            AlertDialogHelperCompat.showAlertDialog(it, message, positiveButtonTitle,
                positiveButtonClickListener, negativeButtonTitle, negativeButtonClickListener, cancelListener)
        }
    }

    companion object {
        /**
         *
         *
         * Factory method for creating a new instance of the Fragment.
         *
         *
         * @param document a [Document] for which no valid extractions were received
         *
         * @return a new instance of the Fragment
         */
        fun createInstance(errorType: ErrorType?, document: Document?, customError: String?): ErrorFragmentCompat {
            val fragment = ErrorFragmentCompat()
            fragment.arguments = ErrorFragmentHelper.createArguments(errorType, document, customError)
            return fragment
        }
    }
}
