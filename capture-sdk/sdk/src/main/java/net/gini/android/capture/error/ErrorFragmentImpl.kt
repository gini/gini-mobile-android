package net.gini.android.capture.error

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import net.gini.android.capture.Document
import net.gini.android.capture.R
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.internal.util.ActivityHelper
import net.gini.android.capture.ImageRetakeOptionsListener

/**
 * Main logic implementation for error handling UI presented by {@link ErrorActivity}
 * Internal use only
 */
class ErrorFragmentImpl(
    private val fragment: FragmentImplCallback,
    private val document: Document?,
    private val errorType: ErrorType?,
    private val customError: String?
) {

    private val defaultListener: ImageRetakeOptionsListener = object :
        ImageRetakeOptionsListener {
        override fun onBackToCameraPressed() {}
        override fun onEnterManuallyPressed() {}
    }

    private var imageRetakeOptionsListener: ImageRetakeOptionsListener? = null
    private lateinit var retakeImagesButton: Button

    fun onCreate(savedInstanceState: Bundle?) {
        ActivityHelper.forcePortraitOrientationOnPhones(fragment.activity)
    }

    fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.gc_fragment_error, container, false)
        retakeImagesButton = view.findViewById(R.id.gc_button_error_retake_images)

        if (shouldAllowRetakeImages()) {
            retakeImagesButton.setOnClickListener { view12: View? -> imageRetakeOptionsListener?.onBackToCameraPressed() }
        } else {
            retakeImagesButton.visibility = View.GONE
        }

        val enterManuallyButton = view.findViewById<View>(R.id.gc_button_error_enter_manually)
        enterManuallyButton.setOnClickListener { view1: View? -> imageRetakeOptionsListener?.onEnterManuallyPressed() }

        customError?.let {
            view.findViewById<TextView>(R.id.gc_error_header).text = it
        }

        errorType?.let {
            view.findViewById<TextView>(R.id.gc_error_header).text =
                fragment.activity?.getString(it.titleTextResource)
            view.findViewById<TextView>(R.id.gc_error_textview).text =
                fragment.activity?.getString(it.descriptionTextResource)
            view.findViewById<ImageView>(R.id.gc_error_header_icon)
                .setImageResource(it.drawableResource)
        }

        return view
    }

    fun setListener(imageRetakeOptionsListener: ImageRetakeOptionsListener?) {
        this.imageRetakeOptionsListener = imageRetakeOptionsListener ?: defaultListener
    }

    private fun shouldAllowRetakeImages(): Boolean {
        if (document == null) {
            retakeImagesButton.text = fragment.activity?.getString(R.string.gc_error_back_to_camera)
            return true
        }

        if (document is ImageMultiPageDocument) {
            var isImportedDocFound = false
            var i = 0
            while (!isImportedDocFound && i < document.documents.size) {
                isImportedDocFound = !isDocumentFromCameraScreen(document.documents[i])
                i++
            }
            return !isImportedDocFound
        }

        return isDocumentFromCameraScreen(document)
    }

    private fun isDocumentFromCameraScreen(document: Document): Boolean {
        return document.importMethod != Document.ImportMethod.OPEN_WITH && document.source.name == "camera"
    }
}
