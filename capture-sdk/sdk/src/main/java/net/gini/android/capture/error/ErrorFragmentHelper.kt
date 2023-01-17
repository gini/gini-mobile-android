package net.gini.android.capture.error

import android.content.Context
import android.os.Bundle
import net.gini.android.capture.Document
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.ImageRetakeOptionsListener

/**
 * Helper class for setting arguments to analysis fragment
 * Internal use only
 */
class ErrorFragmentHelper {

    companion object {
        private const val ARGS_ERROR = "GC_ARGS_ERROR"
        private const val ARGS_DOCUMENT = "ARGS_DOCUMENT"
        private const val ARGS_CUSTOM_ERROR = "ARGS_CUSTOM_ERROR"


        fun createArguments(errorType: ErrorType?, document: Document?, customError: String?): Bundle {
            val arguments = Bundle()
            arguments.putSerializable(ARGS_ERROR, errorType)
            arguments.putParcelable(ARGS_DOCUMENT, document)
            arguments.putString(ARGS_CUSTOM_ERROR, customError)
            return arguments
        }

        fun createFragmentImpl(
            fragment: FragmentImplCallback,
            arguments: Bundle?
        ): ErrorFragmentImpl {
            val document = arguments?.getParcelable<Document>(ARGS_DOCUMENT)
            val error = arguments?.getSerializable(ARGS_ERROR) as? ErrorType
            val customError = arguments?.getString(ARGS_CUSTOM_ERROR)
            return ErrorFragmentImpl(fragment, document, error, customError)
        }

        fun setListener(
            fragmentImpl: ErrorFragmentImpl,
            context: Context
        ) {
            if (context is ImageRetakeOptionsListener) {
                fragmentImpl.setListener(context as ImageRetakeOptionsListener)
            } else {
                throw IllegalStateException(
                    "Hosting activity must implement ImageRetakeOptionsListener."
                )
            }
        }
    }
}
