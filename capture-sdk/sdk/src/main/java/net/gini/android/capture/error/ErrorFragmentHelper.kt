package net.gini.android.capture.error

import android.content.Context
import android.os.Bundle
import net.gini.android.capture.Document
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.ImageRetakeOptionsListener
import net.gini.android.capture.network.ErrorType

class ErrorFragmentHelper {

    companion object {
        private const val ARGS_ERROR = "GC_ARGS_ERROR"
        private const val ARGS_DOCUMENT = "ARGS_DOCUMENT"

        fun createArguments(errorType: ErrorType?, document: Document?): Bundle {
            val arguments = Bundle()
            arguments.putSerializable(ARGS_ERROR, errorType)
            arguments.putParcelable(ARGS_DOCUMENT, document)
            return arguments
        }

        fun createFragmentImpl(
            fragment: FragmentImplCallback,
            arguments: Bundle?
        ): ErrorFragmentImpl {
            val document = arguments?.getParcelable<Document>(ARGS_DOCUMENT)
            val error = arguments?.getSerializable(ARGS_ERROR) as? ErrorType

            return ErrorFragmentImpl(fragment, document, error)
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
