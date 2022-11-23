package net.gini.android.capture.error

import android.content.Context
import android.os.Bundle
import net.gini.android.capture.Document
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.ImageRetakeOptionsListener

class ErrorFragmentHelper {

    companion object {
        private const val ARGS_ERROR = "GC_ARGS_ERROR"
        private const val ARGS_DOCUMENT = "ARGS_DOCUMENT"

        // TODO: set correct param when handling real errors
        fun createArguments(document: Document?): Bundle {
            val arguments = Bundle()
            arguments.putString(ARGS_ERROR, "error")
            arguments.putParcelable(ARGS_DOCUMENT, document)
            return arguments
        }

        fun createFragmentImpl(
            fragment: FragmentImplCallback,
            arguments: Bundle?
        ): ErrorFragmentImpl {
            val document = arguments?.getParcelable<Document>(ARGS_DOCUMENT)

            // TODO: handle Resource<Error>
            return ErrorFragmentImpl(fragment, document)
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
